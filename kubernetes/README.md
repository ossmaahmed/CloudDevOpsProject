# iVolve on Kubernetes

Workload manifests are organized into four folders directly under `kubernetes/`: `frontend`, `auth-service`, `roadmap-service`, and `mysql`. Each folder contains that workload's manifests. Shared resources are kept outside the workload folders: the namespace and frontend Ingress are at the `kubernetes/` root, the StorageClass is in `storage/`, and all network policies are in the top-level `network-policies/` folder with a separate YAML file for each deny or allow rule. MySQL has a PVC, StatefulSet, and client/headless Services.

The network flow is restricted to:

```text
Browser -> ALB Ingress -> Frontend -> Auth Service -> MySQL
                             |
                             +--------> Roadmap Service
```

## Prerequisites

- EKS and `kubectl` configured for the cluster.
- AWS Load Balancer Controller installed with IAM permissions and subnet discovery tags; the Ingress uses class `alb`.
- Terraform applied with the EBS CSI driver and VPC CNI network-policy enforcement enabled.
- Container images built and pushed to the Terraform ECR repository.

The shared `network-policies/deny-all.yaml` policy selects every pod in the `ivolve` namespace. Separate allow policies permit only the required traffic. The frontend allows ingress from the public ALB subnets `10.0.1.0/24` and `10.0.2.0/24`, matching the current Terraform defaults. If `public_subnet_cidrs` changes, update those CIDRs in `network-policies/frontend-allow-ingress-from-alb.yaml`. Egress between services is limited to the requested flow, plus DNS to CoreDNS. These policies rely on the Amazon VPC CNI network policy feature; Kubernetes accepts policies without enforcing them if the cluster CNI is not configured for enforcement.

## Build and configure

From the repository root in a Bash-compatible shell, after applying Terraform:

```shell
ECR_REPOSITORY_URL=$(terraform -chdir=terraform output -raw ecr_repository_url)
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin "${ECR_REPOSITORY_URL%%/*}"

docker build -t "$ECR_REPOSITORY_URL:frontend" ./app/frontend
docker build -t "$ECR_REPOSITORY_URL:auth-service" ./app/auth-service
docker build -t "$ECR_REPOSITORY_URL:roadmap-service" ./app/roadmap-service
docker push "$ECR_REPOSITORY_URL:frontend"
docker push "$ECR_REPOSITORY_URL:auth-service"
docker push "$ECR_REPOSITORY_URL:roadmap-service"
```

Replace `REPLACE_WITH_ECR_REPOSITORY_URL` in the three Deployment manifests with the repository URL. Replace the Secret placeholders in `frontend/secret.yaml`, `auth-service/secret.yaml`, and `mysql/secret.yaml` with strong values. The auth-service Secret contains the shared database application password used by both auth-service and MySQL; MySQL's Secret contains its separate root password. Do not commit real credentials. Kubernetes Secrets are base64-encoded at rest by default; enable EKS secret encryption and restrict cluster access for production.

Apply in order:

```shell
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/mysql/configmap.yaml
kubectl apply -f kubernetes/mysql/secret.yaml
kubectl apply -f kubernetes/auth-service/secret.yaml
kubectl apply -f kubernetes/frontend/configmap.yaml
kubectl apply -f kubernetes/frontend/secret.yaml
kubectl apply -f kubernetes/storage/storage-class.yaml
kubectl apply -f kubernetes/mysql/service.yaml
kubectl apply -f kubernetes/mysql/pvc.yaml
kubectl apply -f kubernetes/auth-service/configmap.yaml
kubectl apply -f kubernetes/auth-service/service.yaml
kubectl apply -f kubernetes/roadmap-service/configmap.yaml
kubectl apply -f kubernetes/roadmap-service/service.yaml
kubectl apply -f kubernetes/frontend/service.yaml
kubectl apply -f kubernetes/mysql/statefulset.yaml
kubectl apply -f kubernetes/auth-service/deployment.yaml
kubectl apply -f kubernetes/roadmap-service/deployment.yaml
kubectl apply -f kubernetes/frontend/deployment.yaml
kubectl apply -f kubernetes/ingress.yaml
kubectl apply -f kubernetes/network-policies/
kubectl get pods,services,ingress,pvc,networkpolicies -n ivolve
```

The Ingress controller provisions an ALB; find its address with `kubectl get ingress -n ivolve`. Point DNS to that address if using a custom domain.
