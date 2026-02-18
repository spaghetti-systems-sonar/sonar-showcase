# ⚠️ INTENTIONAL SECURITY VULNERABILITIES - Infrastructure as Code (IaC)
# This Terraform configuration contains multiple security misconfigurations
# for demonstrating SonarQube's IaC security analysis capabilities

terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# SEC-IAC-01: Hardcoded AWS credentials (S6329)
# Credentials should NEVER be hardcoded in Terraform files
provider "aws" {
  region     = "us-east-1"
  access_key = "AKIAIOSFODNN7EXAMPLE"                           # SEC: Hardcoded credentials
  secret_key = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"     # SEC: Hardcoded credentials
}

# SEC-IAC-02: S3 Bucket with public access (S6265)
# Bucket is publicly accessible - anyone can read/write
resource "aws_s3_bucket" "public_bucket" {
  bucket = "sonarshowcase-public-bucket"

  # SEC: Public bucket without encryption
  # SEC: No versioning enabled
  # SEC: No logging enabled
}

resource "aws_s3_bucket_acl" "public_bucket_acl" {
  bucket = aws_s3_bucket.public_bucket.id
  acl    = "public-read-write"  # SEC: Completely public - S6265
}

# SEC-IAC-03: S3 Bucket without encryption (S6275)
resource "aws_s3_bucket" "unencrypted_bucket" {
  bucket = "sonarshowcase-unencrypted-data"

  # SEC: No server-side encryption configured
  # SEC: Sensitive data stored without encryption
}

# SEC-IAC-04: Overpermissive Security Group (S6319)
# Allows all traffic from anywhere
resource "aws_security_group" "overpermissive_sg" {
  name        = "sonarshowcase-sg"
  description = "Security group with overpermissive rules"
  vpc_id      = aws_vpc.main.id

  # SEC: Allows SSH from anywhere (0.0.0.0/0)
  ingress {
    description = "SSH from anywhere"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]  # SEC: Should be restricted - S6319
  }

  # SEC: Allows RDP from anywhere
  ingress {
    description = "RDP from anywhere"
    from_port   = 3389
    to_port     = 3389
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]  # SEC: Extremely dangerous - S6319
  }

  # SEC: Allows all ports from anywhere
  ingress {
    description = "All ports open"
    from_port   = 0
    to_port     = 65535
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]  # SEC: Complete exposure - S6319
  }

  # SEC: Allows all outbound traffic
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]  # SEC: Could allow data exfiltration
  }
}

# SEC-IAC-05: RDS Instance without encryption (S6308)
resource "aws_db_instance" "unencrypted_db" {
  identifier           = "sonarshowcase-db"
  engine               = "postgres"
  engine_version       = "13.7"
  instance_class       = "db.t3.micro"
  allocated_storage    = 20
  username             = "admin"           # SEC: Default username
  password             = "SuperSecret123"  # SEC: Hardcoded password - S6329
  publicly_accessible  = true             # SEC: Database publicly accessible - S6329
  storage_encrypted    = false            # SEC: No encryption at rest - S6308
  skip_final_snapshot  = true             # SEC: No backup on deletion

  # SEC: No backup retention
  backup_retention_period = 0

  # SEC: No encryption in transit
  # SEC: No IAM database authentication
}

# SEC-IAC-06: EC2 Instance with IMDSv1 (S6319)
resource "aws_instance" "vulnerable_instance" {
  ami           = "ami-0c55b159cbfafe1f0"
  instance_type = "t2.micro"
  key_name      = "insecure-key"

  # SEC: No encryption for root volume
  root_block_device {
    encrypted = false  # SEC: Unencrypted storage - S6275
  }

  # SEC: IMDSv1 enabled (vulnerable to SSRF)
  metadata_options {
    http_endpoint = "enabled"
    http_tokens   = "optional"  # SEC: Should be "required" for IMDSv2 - S6319
  }

  # SEC: Using overpermissive security group
  vpc_security_group_ids = [aws_security_group.overpermissive_sg.id]

  # SEC: No monitoring enabled
  monitoring = false

  tags = {
    Name = "Vulnerable-Instance"
    # SEC: Sensitive data in tags
    Password     = "InstanceP@ss123"
    DatabaseConn = "postgresql://admin:SuperSecret123@db.example.com:5432/maindb"
  }
}

# SEC-IAC-07: IAM Policy with wildcard permissions (S6302)
resource "aws_iam_policy" "overpermissive_policy" {
  name        = "OverpermissivePolicy"
  description = "Policy with excessive permissions"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = "*"  # SEC: Allows ALL actions - S6302
        Resource = "*"  # SEC: On ALL resources - S6302
      }
    ]
  })
}

# SEC-IAC-08: IAM User with hardcoded access key (S6329)
resource "aws_iam_access_key" "hardcoded_key" {
  user = aws_iam_user.app_user.name
}

resource "aws_iam_user" "app_user" {
  name = "application-user"
}

# SEC: Access key will be in state file
output "access_key" {
  value     = aws_iam_access_key.hardcoded_key.id
  sensitive = false  # SEC: Should be true - S6329
}

output "secret_key" {
  value     = aws_iam_access_key.hardcoded_key.secret
  sensitive = false  # SEC: Secret exposed in output - S6329
}

# SEC-IAC-09: CloudFront without HTTPS enforcement (S6319)
resource "aws_cloudfront_distribution" "insecure_cdn" {
  enabled = true

  origin {
    domain_name = aws_s3_bucket.public_bucket.bucket_regional_domain_name
    origin_id   = "S3-Public"
  }

  default_cache_behavior {
    allowed_methods  = ["GET", "HEAD", "OPTIONS"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "S3-Public"

    viewer_protocol_policy = "allow-all"  # SEC: Allows HTTP (unencrypted) - S6319

    forwarded_values {
      query_string = false
      cookies {
        forward = "none"
      }
    }
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"  # SEC: No geographic restrictions
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true  # SEC: Not using custom SSL certificate
  }
}

# SEC-IAC-10: Lambda function with overpermissive role (S6302)
resource "aws_lambda_function" "insecure_lambda" {
  filename      = "lambda.zip"
  function_name = "insecure-function"
  role          = aws_iam_role.lambda_role.arn
  handler       = "index.handler"
  runtime       = "nodejs18.x"

  environment {
    variables = {
      DB_PASSWORD = "SuperSecret123"  # SEC: Hardcoded secret - S6329
      API_KEY     = "sk_live_abc123"  # SEC: Hardcoded API key - S6329
    }
  }

  # SEC: No encryption for environment variables
  # SEC: No VPC configuration
  # SEC: No dead letter queue
  # SEC: No reserved concurrent executions (DoS risk)
}

resource "aws_iam_role" "lambda_role" {
  name = "lambda-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_admin" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/AdministratorAccess"  # SEC: Admin access - S6302
}

# SEC-IAC-11: VPC without flow logs (S6321)
resource "aws_vpc" "main" {
  cidr_block = "10.0.0.0/16"

  # SEC: No VPC flow logs enabled
  # SEC: No DNS support
  enable_dns_support   = false
  enable_dns_hostnames = false
}

# SEC-IAC-12: ELB without access logs (S6329)
resource "aws_elb" "insecure_lb" {
  name               = "insecure-lb"
  availability_zones = ["us-east-1a"]

  listener {
    instance_port     = 80
    instance_protocol = "http"   # SEC: HTTP instead of HTTPS
    lb_port           = 80
    lb_protocol       = "http"   # SEC: HTTP instead of HTTPS
  }

  # SEC: No access logs enabled
  # SEC: No connection draining
  # SEC: No SSL certificate

  security_groups = [aws_security_group.overpermissive_sg.id]
}

# SEC-IAC-13: Secrets in plain text (S6329)
locals {
  database_password = "MyDatabasePassword123!"  # SEC: Hardcoded secret
  api_key           = "sk_live_1234567890"      # SEC: Hardcoded API key
  encryption_key    = "ThisIsMyEncryptionKey"   # SEC: Hardcoded encryption key

  # SEC: Connection strings with credentials
  database_url = "postgresql://admin:${local.database_password}@db.example.com:5432/maindb"
}

# SEC-IAC-14: KMS Key with overpermissive policy (S6302)
resource "aws_kms_key" "insecure_key" {
  description             = "KMS key with overpermissive policy"
  deletion_window_in_days = 7

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "Enable IAM User Permissions"
        Effect = "Allow"
        Principal = {
          AWS = "*"  # SEC: Allows access from any AWS account - S6302
        }
        Action   = "kms:*"  # SEC: All KMS actions allowed - S6302
        Resource = "*"
      }
    ]
  })
}
