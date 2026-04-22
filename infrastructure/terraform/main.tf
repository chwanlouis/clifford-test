terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
  }
  required_version = ">= 1.5.0"

  backend "s3" {
    bucket = "itms-terraform-state"
    key    = "infrastructure/terraform.tfstate"
    region = "us-east-1"
  }
}

provider "aws" {
  region = var.aws_region
}

# EKS Cluster
resource "aws_eks_cluster" "itms" {
  name     = "itms-cluster-${var.environment}"
  role_arn = aws_iam_role.eks_cluster_role.arn
  version  = "1.28"

  vpc_config {
    subnet_ids              = aws_subnet.private[*].id
    endpoint_private_access = true
    endpoint_public_access  = true
    public_access_cidrs     = var.allowed_cidrs
  }

  tags = local.common_tags
}

# EKS Node Group
resource "aws_eks_node_group" "itms_nodes" {
  cluster_name    = aws_eks_cluster.itms.name
  node_group_name = "itms-nodes"
  node_role_arn   = aws_iam_role.eks_node_role.arn
  subnet_ids      = aws_subnet.private[*].id

  instance_types = [var.node_instance_type]

  scaling_config {
    desired_size = var.desired_node_count
    max_size     = var.max_node_count
    min_size     = var.min_node_count
  }

  tags = local.common_tags
}

# RDS PostgreSQL
resource "aws_db_instance" "itms_postgres" {
  identifier        = "itms-postgres-${var.environment}"
  engine            = "postgres"
  engine_version    = "15.4"
  instance_class    = var.db_instance_class
  allocated_storage = 100
  storage_encrypted = true

  db_name  = "itms_trades"
  username = "itms"
  password = var.db_password

  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.itms.name

  backup_retention_period  = 7
  multi_az                 = var.environment == "production" ? true : false
  deletion_protection      = var.environment == "production" ? true : false
  skip_final_snapshot      = var.environment != "production"
  final_snapshot_identifier = "${var.environment}-itms-final-snapshot"

  tags = local.common_tags
}

# ElastiCache Redis
resource "aws_elasticache_cluster" "itms_redis" {
  cluster_id           = "itms-redis-${var.environment}"
  engine               = "redis"
  node_type            = var.redis_node_type
  num_cache_nodes      = 1
  parameter_group_name = "default.redis7"
  engine_version       = "7.0"
  port                 = 6379
  subnet_group_name    = aws_elasticache_subnet_group.itms.name
  security_group_ids   = [aws_security_group.redis.id]

  tags = local.common_tags
}

# MSK (Managed Kafka)
resource "aws_msk_cluster" "itms_kafka" {
  cluster_name           = "itms-kafka-${var.environment}"
  kafka_version          = "3.5.1"
  number_of_broker_nodes = 3

  broker_node_group_info {
    instance_type  = var.kafka_instance_type
    client_subnets = aws_subnet.private[*].id
    storage_info {
      ebs_storage_info {
        volume_size = 100
      }
    }
    security_groups = [aws_security_group.kafka.id]
  }

  encryption_info {
    encryption_in_transit {
      client_broker = "TLS_PLAINTEXT"
    }
  }

  tags = local.common_tags
}

# VPC
resource "aws_vpc" "itms" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true
  tags = merge(local.common_tags, { Name = "itms-vpc-${var.environment}" })
}

resource "aws_subnet" "private" {
  count             = 2
  vpc_id            = aws_vpc.itms.id
  cidr_block        = "10.0.${count.index + 1}.0/24"
  availability_zone = data.aws_availability_zones.available.names[count.index]
  tags = merge(local.common_tags, { Name = "itms-private-${count.index + 1}" })
}

resource "aws_subnet" "public" {
  count                   = 2
  vpc_id                  = aws_vpc.itms.id
  cidr_block              = "10.0.${count.index + 10}.0/24"
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true
  tags = merge(local.common_tags, { Name = "itms-public-${count.index + 1}" })
}

data "aws_availability_zones" "available" {}

# IAM Roles
resource "aws_iam_role" "eks_cluster_role" {
  name = "itms-eks-cluster-role-${var.environment}"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = { Service = "eks.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "eks_cluster_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
  role       = aws_iam_role.eks_cluster_role.name
}

resource "aws_iam_role" "eks_node_role" {
  name = "itms-eks-node-role-${var.environment}"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "eks_worker_node_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
  role       = aws_iam_role.eks_node_role.name
}

resource "aws_iam_role_policy_attachment" "eks_cni_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
  role       = aws_iam_role.eks_node_role.name
}

resource "aws_iam_role_policy_attachment" "ecr_readonly" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
  role       = aws_iam_role.eks_node_role.name
}

# Security Groups
resource "aws_security_group" "rds" {
  name   = "itms-rds-sg-${var.environment}"
  vpc_id = aws_vpc.itms.id
  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  tags = local.common_tags
}

resource "aws_security_group" "redis" {
  name   = "itms-redis-sg-${var.environment}"
  vpc_id = aws_vpc.itms.id
  ingress {
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  tags = local.common_tags
}

resource "aws_security_group" "kafka" {
  name   = "itms-kafka-sg-${var.environment}"
  vpc_id = aws_vpc.itms.id
  ingress {
    from_port   = 9092
    to_port     = 9092
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  tags = local.common_tags
}

# Subnet Groups
resource "aws_db_subnet_group" "itms" {
  name       = "itms-db-subnet-group-${var.environment}"
  subnet_ids = aws_subnet.private[*].id
  tags       = local.common_tags
}

resource "aws_elasticache_subnet_group" "itms" {
  name       = "itms-redis-subnet-group-${var.environment}"
  subnet_ids = aws_subnet.private[*].id
}

locals {
  common_tags = {
    Project     = "ITMS"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}
