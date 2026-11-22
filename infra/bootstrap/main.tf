data "aws_caller_identity" "current" {}

locals {
  account_id  = data.aws_caller_identity.current.account_id
  bucket_name = "${var.project}-tfstate-${local.account_id}"
  oidc_url    = "token.actions.githubusercontent.com"
}

# ---------------------------------------------------------------------------
# Terraform state bucket. Locking uses a lock file in the same bucket
# (use_lockfile), so no DynamoDB table is needed.
# ---------------------------------------------------------------------------
resource "aws_s3_bucket" "state" {
  bucket = local.bucket_name
}

resource "aws_s3_bucket_versioning" "state" {
  bucket = aws_s3_bucket.state.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "state" {
  bucket = aws_s3_bucket.state.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "state" {
  bucket                  = aws_s3_bucket.state.id
  block_public_acls       = true
  ignore_public_acls      = true
  block_public_policy     = true
  restrict_public_buckets = true
}

data "aws_iam_policy_document" "state_bucket" {
  statement {
    sid       = "DenyInsecureTransport"
    effect    = "Deny"
    actions   = ["s3:*"]
    resources = [aws_s3_bucket.state.arn, "${aws_s3_bucket.state.arn}/*"]

    principals {
      type        = "*"
      identifiers = ["*"]
    }

    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }
}

resource "aws_s3_bucket_policy" "state" {
  bucket = aws_s3_bucket.state.id
  policy = data.aws_iam_policy_document.state_bucket.json

  depends_on = [aws_s3_bucket_public_access_block.state]
}

# ---------------------------------------------------------------------------
# GitHub Actions -> AWS without static keys (OIDC).
# ---------------------------------------------------------------------------
resource "aws_iam_openid_connect_provider" "github" {
  count = var.create_oidc_provider ? 1 : 0

  url            = "https://${local.oidc_url}"
  client_id_list = ["sts.amazonaws.com"]
}

data "aws_iam_openid_connect_provider" "github" {
  count = var.create_oidc_provider ? 0 : 1

  url = "https://${local.oidc_url}"
}

locals {
  oidc_provider_arn = var.create_oidc_provider ? aws_iam_openid_connect_provider.github[0].arn : data.aws_iam_openid_connect_provider.github[0].arn
}

# One role per environment. The "sub" claim pins the repository AND the GitHub
# Environment, so only a job that runs in environment "dev" can assume the dev role.
data "aws_iam_policy_document" "deploy_assume" {
  for_each = var.environments

  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [local.oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_url}:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_url}:sub"
      values   = ["repo:${var.github_repository}:environment:${each.key}"]
    }
  }
}

resource "aws_iam_role" "deploy" {
  for_each = var.environments

  name               = "${var.project}-${each.key}-deploy"
  assume_role_policy = data.aws_iam_policy_document.deploy_assume[each.key].json
}

# Least privilege by service. Create/Describe calls of these services do not
# support resource-level scoping, so they stay on "*"; what can be narrowed is
# narrowed: the state prefix, the IAM roles and the secrets of that environment.
data "aws_iam_policy_document" "deploy" {
  for_each = var.environments

  statement {
    sid       = "StateList"
    actions   = ["s3:ListBucket"]
    resources = [aws_s3_bucket.state.arn]

    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values   = ["${each.key}/*"]
    }
  }

  statement {
    sid       = "StateObjects"
    actions   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
    resources = ["${aws_s3_bucket.state.arn}/${each.key}/*"]
  }

  statement {
    sid = "InfrastructureServices"
    actions = [
      "ec2:*",
      "elasticloadbalancing:*",
      "ecs:*",
      "ecr:*",
      "rds:*",
      "logs:*",
      "application-autoscaling:*",
      "cloudwatch:PutMetricAlarm",
      "cloudwatch:DeleteAlarms",
      "cloudwatch:DescribeAlarms",
      "cloudwatch:ListTagsForResource",
      "cloudwatch:TagResource",
    ]
    resources = ["*"]
  }

  statement {
    sid       = "SecretsOfThisEnvironment"
    actions   = ["secretsmanager:*"]
    resources = ["arn:aws:secretsmanager:${var.region}:${local.account_id}:secret:${var.project}/${each.key}/*"]
  }

  statement {
    sid       = "SecretsDescribeAndRdsManaged"
    actions   = ["secretsmanager:DescribeSecret", "secretsmanager:GetResourcePolicy", "secretsmanager:ListSecrets"]
    resources = ["*"]
  }

  statement {
    sid = "RolesOfThisEnvironment"
    actions = [
      "iam:CreateRole",
      "iam:DeleteRole",
      "iam:GetRole",
      "iam:UpdateAssumeRolePolicy",
      "iam:TagRole",
      "iam:UntagRole",
      "iam:ListRolePolicies",
      "iam:ListAttachedRolePolicies",
      "iam:ListInstanceProfilesForRole",
      "iam:PutRolePolicy",
      "iam:GetRolePolicy",
      "iam:DeleteRolePolicy",
      "iam:AttachRolePolicy",
      "iam:DetachRolePolicy",
    ]
    resources = ["arn:aws:iam::${local.account_id}:role/${var.project}-${each.key}-*"]
  }

  statement {
    sid       = "PassRolesToEcsTasks"
    actions   = ["iam:PassRole"]
    resources = ["arn:aws:iam::${local.account_id}:role/${var.project}-${each.key}-*"]

    condition {
      test     = "StringEquals"
      variable = "iam:PassedToService"
      values   = ["ecs-tasks.amazonaws.com"]
    }
  }

  statement {
    sid       = "ServiceLinkedRoles"
    actions   = ["iam:CreateServiceLinkedRole"]
    resources = ["arn:aws:iam::${local.account_id}:role/aws-service-role/*"]
  }
}

resource "aws_iam_role_policy" "deploy" {
  for_each = var.environments

  name   = "terraform-deploy"
  role   = aws_iam_role.deploy[each.key].id
  policy = data.aws_iam_policy_document.deploy[each.key].json
}
