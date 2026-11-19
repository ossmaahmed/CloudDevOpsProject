# CloudDevOpsProject

An end-to-end DevOps project that builds and deploys a microservices application to AWS EKS. Jenkins builds and scans container images, pushes them to Amazon ECR, and commits updated Kubernetes manifests. Argo CD watches the repository and synchronizes the application to the cluster.

Repository: [ossmaahmed/CloudDevOpsProject](https://github.com/ossmaahmed/CloudDevOpsProject)

## Architecture

```text
Developer
   |
   v
GitHub repository <--- Jenkins on EC2
   |                    | Build / Trivy scan / push
   |                    v
   |                  Amazon ECR
   |                    ^
   |                    | pulls images
   v                    |
Argo CD ------------> EKS
                       |
Browser -> ALB Ingress -> Frontend
                            |----> Auth Service ----> MySQL StatefulSet + EBS volume
                            `----> Roadmap Service
```

Terraform provisions the VPC, Jenkins EC2 instance, EKS, and ECR. Ansible configures Jenkins with Java, Docker, and Trivy. Kubernetes manifests define the application resources, and Argo CD deploys changes from Git.

## Repository layout

| Path | Contents |
| --- | --- |
| [`app/`](app/) | Application source, Dockerfiles, and local Docker Compose setup |
| [`terraform/`](terraform/) | AWS Terraform modules and S3 state backend |
| [`ansible/`](ansible/) | Dynamic AWS inventory, playbook, and Jenkins configuration roles |
| [`kubernetes/`](kubernetes/) | Namespace, Deployments, Services, MySQL StatefulSet, storage, Ingress, and network policies |
| [`argocd/`](argocd/) | Argo CD Application manifest |
| [`vars/`](vars/) | Jenkins Shared Library steps used by the pipeline |
| [`Jenkinsfile`](Jenkinsfile) | CI pipeline for the three application services |

Component-specific guides: [application](app/README.md), [Terraform](terraform/README.md), [Ansible](ansible/README.md), and [Kubernetes](kubernetes/README.md).

## Prerequisites

- Docker with the Compose plugin for local development.
- AWS CLI configured with permissions for the project resources.
- Terraform 1.10 or later.
- Ansible and the required AWS inventory dependencies.
- `kubectl` configured for the EKS cluster.
- A GitHub credential configured in Jenkins for pushing manifest changes.

## Run locally with Docker Compose

The Compose file builds the three services and starts MySQL. Provide these variables in a private `app/.env` file:

```text
MYSQL_ROOT_PASSWORD=<local-root-password>
DB_NAME=<database-name>
DB_USERNAME=<application-user>
DB_PASSWORD=<application-password>
```

Do not commit real passwords, tokens, or other credentials. Start the application from the repository root:

```shell
docker compose --env-file app/.env -f app/docker-compose.yml up --build
```

Open the frontend at [http://localhost:3005](http://localhost:3005). Stop the services with:

```shell
docker compose --env-file app/.env -f app/docker-compose.yml down
```

To delete the local MySQL data as well, add `-v` to the `down` command.

## Provision AWS infrastructure

The S3 bucket configured in [`terraform/backend.tf`](terraform/backend.tf) must exist before Terraform initialization. Review `terraform/terraform.tfvars` and use a trusted IP range for Jenkins and EKS API access.

```shell
cd terraform
terraform init
terraform validate
terraform plan
terraform apply
```

Configure `kubectl` after the cluster is created:

```shell
aws eks update-kubeconfig --region us-east-1 --name ivolve-eks
```

See the [Terraform guide](terraform/README.md) for AWS prerequisites and infrastructure details.

## Configure Jenkins with Ansible

From the repository root, install the inventory dependencies and configure the running Jenkins instance:

```shell
python -m pip install boto3 botocore
ansible-galaxy collection install -r ansible/requirements.yml
ansible-inventory -i ansible/inventory/aws_ec2.yml --graph
ansible-playbook -i ansible/inventory/aws_ec2.yml ansible/site.yml --private-key <path-to-ec2-private-key>
```

See the [Ansible guide](ansible/README.md) for access requirements and role details.

In Jenkins, configure the global Shared Library named `ivolve-shared-library` to use this repository. Configure the `github-creds` username/password credential for the `Push Manifests` step. The Jenkins EC2 instance role supplies the AWS permissions used to push images to ECR. Configure the Jenkins job to use this repository's root `Jenkinsfile`; enable a webhook or SCM polling in the job if builds should start automatically.

The pipeline uses the steps in `vars/` and runs these stages for `auth-service`, `frontend`, and `roadmap-service`:

1. Build Image
2. Scan Image
3. Push Image
4. Delete Image Locally
5. Update Manifests
6. Push Manifests

## Deploy with Kubernetes and Argo CD

Create the required application secrets securely before deployment. The secret manifests in Git are placeholders and are excluded from Argo CD synchronization; never replace them with real credentials in a commit.

Follow the deployment order and prerequisites in the [Kubernetes guide](kubernetes/README.md). Install Argo CD in the cluster, then apply the Application manifest:

```shell
kubectl apply -f argocd/application.yaml
kubectl get application ivolve -n argocd
kubectl get pods,services,ingress,pvc -n ivolve
```

Argo CD synchronizes the `kubernetes/` directory from the `main` branch. The frontend Ingress provisions an AWS Application Load Balancer; retrieve its address with:

```shell
kubectl get ingress -n ivolve
```

## Security and operations

- Keep application credentials in Jenkins Credentials or Kubernetes Secrets, not in Git.
- Restrict Jenkins and EKS API access to trusted IP ranges.
- The Kubernetes Secret YAML files in the repository are placeholders; provision real values separately.
- Terraform state is stored in the configured S3 backend. Protect access to the bucket and do not commit state files.
- The EKS worker count is controlled by `terraform/main.tf`; verify it against available pod capacity and expected AWS cost before changing it.
