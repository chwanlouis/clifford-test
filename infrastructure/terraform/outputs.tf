output "eks_cluster_endpoint" {
  description = "EKS cluster endpoint"
  value       = aws_eks_cluster.itms.endpoint
}

output "eks_cluster_name" {
  description = "EKS cluster name"
  value       = aws_eks_cluster.itms.name
}

output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint"
  value       = aws_db_instance.itms_postgres.endpoint
  sensitive   = true
}

output "redis_endpoint" {
  description = "ElastiCache Redis endpoint"
  value       = aws_elasticache_cluster.itms_redis.cache_nodes[0].address
}

output "kafka_bootstrap_brokers" {
  description = "MSK Kafka bootstrap brokers"
  value       = aws_msk_cluster.itms_kafka.bootstrap_brokers
}

output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.itms.id
}
