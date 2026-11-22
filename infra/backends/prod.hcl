# Partial backend configuration. The bucket is passed at init: -backend-config="bucket=<state bucket>"
key          = "prod/terraform.tfstate"
region       = "us-east-2"
encrypt      = true
use_lockfile = true
