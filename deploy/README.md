# Self-hosted deployment assets

These files support the self-hosted path described in `docs/deployment/self-hosted-migration.md`:
one VM, k3s, Helm-managed MySQL/Redis/RabbitMQ, and Kubernetes Secrets for application configuration.

## Datastores

Create private copies of the two `*.example` files and replace every `CHANGE_ME` value. The copied files
must remain outside Git:

```bash
cp mysql-values.yaml.example /tmp/flipnote-mysql-values.yaml
cp rabbitmq-values.yaml.example /tmp/flipnote-rabbitmq-values.yaml

helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
helm upgrade --install mysql bitnami/mysql -n flipnote --create-namespace \
  -f /tmp/flipnote-mysql-values.yaml
helm upgrade --install redis bitnami/redis -n flipnote -f redis-values.yaml
helm upgrade --install rabbitmq bitnami/rabbitmq -n flipnote \
  -f /tmp/flipnote-rabbitmq-values.yaml
```

The resulting in-cluster endpoints are `mysql:3306`, `redis-master:6379`, and `rabbitmq:5672`.
Use those names in the application Secrets. Redis has authentication disabled in this profile; RabbitMQ
uses the username/password from the private values file.

## Application Secrets

The Helm charts default to these existing Secret names:

```text
user-service-secret
image-service-secret
group-service-secret
cardset-service-secret
reaction-service-secret
notification-service-secret
api-gateway-secret
```

Create each with `kubectl create secret generic ... --from-env-file=... -n flipnote`. The complete key list
is in the deployment guide's Appendix A. Do not put those env files in the repository.

If the GHCR packages are private, add a pull secret and set `imagePullSecrets` in each chart's values:

```bash
kubectl create secret docker-registry ghcr-pull-secret -n flipnote \
  --docker-server=ghcr.io \
  --docker-username=<GITHUB_USERNAME> \
  --docker-password=<GHCR_READ_TOKEN>
```

For public packages, leave `imagePullSecrets: []` as-is.
