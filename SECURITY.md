# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| latest  | ✅        |

## Secrets Management

### ✅ Required: Environment Variables Only

**All secrets and credentials must be supplied via environment variables at
runtime. They must never be hardcoded in source code, configuration files, or
committed to version control.**

This includes, but is not limited to:

| Secret | Environment Variable |
|---|---|
| Database password | `SPRING_DATASOURCE_PASSWORD` |
| JWT signing secret | `JWT_SECRET` |
| Any third-party API keys | application-specific variables |

### How to supply secrets safely

**Local development** — use a `.env` file (already listed in `.gitignore`) and
load it with a tool such as [direnv](https://direnv.net/) or the IDE of your
choice:

```bash
# .env  (never commit this file)
SPRING_DATASOURCE_PASSWORD=<your-local-dev-password>
JWT_SECRET=<your-local-dev-jwt-secret>
```

**CI/CD** — store secrets in your pipeline's secret store (e.g. GitHub Actions
Secrets, AWS Secrets Manager, HashiCorp Vault) and inject them as environment
variables at build/deploy time.

**Production** — use a secrets manager (AWS Secrets Manager, Azure Key Vault,
GCP Secret Manager, HashiCorp Vault) and inject values at container/pod startup.
Never bake secrets into Docker images or Kubernetes manifests stored in Git.

### What we check in CI

- [ ] No plaintext passwords or secrets in `application.yml` defaults
- [ ] No plaintext passwords or secrets in any `*.properties` or `*.yml` file
- [ ] No `Authorization` headers with real tokens in test fixtures
- [ ] `.env` files are listed in `.gitignore`

### Configuration file conventions

Default values in `application.yml` use the pattern:

```yaml
some-secret: ${ENV_VAR_NAME:replace-in-production}
```

The string `replace-in-production` is a deliberately non-functional placeholder.
The application will fail to start correctly if that placeholder reaches
production — this is intentional.

## Reporting a Vulnerability

If you discover a security vulnerability in this project, please **do not** open
a public GitHub issue. Instead, report it privately via the GitHub
[Security Advisories](https://github.com/hmuthusamy/carddemo-java/security/advisories/new)
feature.

Please include:

1. A description of the vulnerability and its potential impact
2. Steps to reproduce
3. Any suggested remediation

We aim to acknowledge reports within **48 hours** and provide a fix or mitigation
plan within **14 days** for critical issues.
