# Bootstrap

Creates, once and with local state, what the rest of the infrastructure needs before it can exist:

- the S3 bucket that stores the Terraform state (versioned, encrypted, private, TLS only);
- the GitHub OIDC provider;
- one deploy role per environment (`dev`, `staging`, `prod`) that GitHub Actions assumes without static keys.

The state of this directory stays on your machine (`*.tfstate` is git-ignored). It is tiny and rarely changes.

## Apply (PowerShell)

```powershell
cd infra\bootstrap
terraform init
terraform plan
terraform apply
```

If the account already has a GitHub OIDC provider (`aws iam list-open-id-connect-providers`), reuse it:

```powershell
terraform plan "-var=create_oidc_provider=false"
```

## Which repository may assume the roles

The trust policy compares the `sub` claim of GitHub's token. This repository uses **immutable subjects** (owner and
repository IDs instead of names), so the value is not `repo:<owner>/<name>`. Read the real prefix and pass it as
`github_subject_prefix` if the repository changes:

```powershell
gh api repos/<owner>/<name>/actions/oidc/customization/sub
```

With the wrong prefix the workflow fails at `configure-aws-credentials` with `Not authorized to perform sts:AssumeRoleWithWebIdentity`.

## After applying

- `terraform output state_bucket` is the value for `-backend-config="bucket=..."` in `infra/`.
- `terraform output deploy_role_arns` gives one ARN per environment; save each as the `AWS_ROLE_ARN` variable of the
  GitHub Environment with the same name (`dev`, `staging`, `prod`).

## Limits

- The deploy roles are scoped by service, not by resource: `ec2:*`, `rds:*`, etc. cannot be narrowed to one
  environment. What is narrowed: state prefix, IAM roles and secrets of that environment.
- The policy is a first iteration: adjust it when a real `plan`/`apply` reports `AccessDenied`.
