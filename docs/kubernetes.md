# Local Kubernetes runbook

SettleUp runs locally on a three-node kind cluster. The API is scheduled as two replicas, PostgreSQL uses a persistent volume, nginx handles ingress, and metrics-server feeds a CPU-based HPA.

## Start

```bash
docker compose build api
cp k8s/overlays/local/secrets.env.example k8s/overlays/local/secrets.env
./k8s/scripts/create-local-cluster.sh
```

The creation script is idempotent for an existing `settleup` cluster. It loads the local image, installs ingress-nginx and metrics-server, applies the local Kustomize overlay, and waits for the database, API, Prometheus, and Grafana rollouts.

## Verify

```bash
kubectl get pods,service,ingress,hpa,pvc -n settleup
kubectl top pods -n settleup
curl -H 'Host: settleup.local' http://127.0.0.1:8081/actuator/health/readiness
```

## Observability

Prometheus discovers both annotated API pods and scrapes `/actuator/prometheus`. Grafana provisions its Prometheus data source and the SettleUp overview dashboard automatically.

```bash
kubectl -n settleup port-forward service/prometheus 9090:9090
kubectl -n settleup port-forward service/grafana 3000:3000
```

- Prometheus targets: <http://localhost:9090/targets>
- Grafana dashboard: <http://localhost:3000/d/settleup-overview/settleup-overview>

API logs are emitted as JSON. Every response includes `X-Correlation-ID`; the same value is placed in the logging MDC so a request can be traced across its log entries.

## Resilience checks

Delete one API pod by its exact name, then wait for Kubernetes to restore two ready replicas:

```bash
kubectl get pods -n settleup -l app.kubernetes.io/name=settleup-api
kubectl delete pod <api-pod> -n settleup
kubectl rollout status deployment/api -n settleup --timeout=180s
```

Test database-readiness isolation with the disposable failure fixture. It intentionally uses an unreachable database host, must remain unready, and must have `ready=false` in its EndpointSlice before removal.

```bash
kubectl apply -f k8s/tests/readiness-failure-pod.yaml
kubectl get endpointslice -n settleup -l kubernetes.io/service-name=api -o yaml
kubectl delete pod api-readiness-failure -n settleup
```

Exercise autoscaling with sustained in-cluster traffic, observe the API scale from two toward five replicas, then remove the generator:

```bash
kubectl apply -f k8s/tests/load-generator.yaml
kubectl get hpa api -n settleup --watch
kubectl delete pod api-load-generator -n settleup
```

## Phase 11 acceptance record

On September 15, 2026, the local acceptance run verified:

- both API replicas ready on separate kind worker nodes;
- ingress, liveness, and database-backed readiness endpoints returning `UP`;
- PostgreSQL's 1 Gi persistent volume bound and all six Flyway migrations applied;
- uninterrupted ingress health while an API pod was deleted, followed by automatic replacement;
- an isolated bad-database pod reported `ready=false, serving=false` and was excluded from ready service endpoints;
- metrics-server reported pod CPU, and load drove the HPA from two to its five-replica maximum with all five replicas ready.

## Phase 13 acceptance record

On September 16, 2026, the local acceptance run verified:

- both API replicas exposed Prometheus metrics and appeared as healthy scrape targets;
- the provisioned Grafana dashboard loaded with request-rate, p95 latency, error-rate, JVM-heap, and database-pool panels;
- structured application logs rendered as JSON;
- the ingress health endpoint remained `UP` after the observability rollout.
