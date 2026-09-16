#!/usr/bin/env bash
set -euo pipefail

if ! kind get clusters | grep -qx settleup; then
  kind create cluster --name settleup --config k8s/kind-config.yaml
fi
kind load docker-image settleup-api:local --name settleup

helm upgrade --install ingress-nginx ingress-nginx \
  --repo https://kubernetes.github.io/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace \
  --set controller.hostPort.enabled=true \
  --set controller.service.type=ClusterIP \
  --set-string controller.nodeSelector.ingress-ready=true \
  --set 'controller.tolerations[0].key=node-role.kubernetes.io/control-plane' \
  --set 'controller.tolerations[0].operator=Exists' \
  --set 'controller.tolerations[0].effect=NoSchedule'

helm upgrade --install metrics-server metrics-server \
  --repo https://kubernetes-sigs.github.io/metrics-server/ \
  --namespace kube-system \
  --set 'args[0]=--cert-dir=/tmp' \
  --set 'args[1]=--secure-port=10250' \
  --set-string 'args[2]=--kubelet-preferred-address-types=InternalIP\,ExternalIP\,Hostname' \
  --set 'args[3]=--kubelet-use-node-status-port' \
  --set 'args[4]=--metric-resolution=15s' \
  --set 'args[5]=--kubelet-insecure-tls'

kubectl rollout status deployment/ingress-nginx-controller -n ingress-nginx --timeout=180s
kubectl rollout status deployment/metrics-server -n kube-system --timeout=180s
kubectl apply -k k8s/overlays/local
kubectl rollout status statefulset/postgres -n settleup --timeout=180s
kubectl rollout status deployment/api -n settleup --timeout=180s
