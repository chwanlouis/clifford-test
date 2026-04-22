variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name (development, staging, production)"
  type        = string
  default     = "development"
}

variable "node_instance_type" {
  description = "EKS node instance type"
  type        = string
  default     = "m5.xlarge"
}

variable "desired_node_count" {
  description = "Desired number of EKS nodes"
  type        = number
  default     = 3
}

variable "max_node_count" {
  description = "Maximum number of EKS nodes"
  type        = number
  default     = 10
}

variable "min_node_count" {
  description = "Minimum number of EKS nodes"
  type        = number
  default     = 2
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.r6g.large"
}

variable "db_password" {
  description = "RDS master password"
  type        = string
  sensitive   = true
}

variable "redis_node_type" {
  description = "ElastiCache Redis node type"
  type        = string
  default     = "cache.r6g.large"
}

variable "kafka_instance_type" {
  description = "MSK Kafka broker instance type"
  type        = string
  default     = "kafka.m5.large"
}

variable "allowed_cidrs" {
  description = "CIDRs allowed to access EKS API"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}
