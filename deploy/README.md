# DisruptFlow 部署目录

## Docker 构建

```bash
cd /Users/chenxiaofei.ropz/Desktop/DisruptFlow

docker build -f deploy/docker/Dockerfile -t disruptflow:latest .
```

## Kubernetes 部署

```bash
kubectl create namespace disruptflow
kubectl apply -f deploy/k8s/configmap.yaml
kubectl apply -f deploy/k8s/secret.example.yaml
kubectl apply -f deploy/k8s/deployment.yaml
kubectl apply -f deploy/k8s/hpa.yaml
```

> `secret.example.yaml` 仅为示例，请在部署前替换成真实凭据并通过安全渠道管理。
