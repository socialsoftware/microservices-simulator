# Kubernetes Deployment

The distributed mode can also be deployed on Kubernetes, using Spring Cloud Kubernetes for service discovery instead of Eureka.

## Table of Contents

- [Deploying to Local Kubernetes (Kind)](#deploying-to-local-kubernetes-kind)
  - [Prerequisites](#prerequisites)
  - [Build and Load Images](#build-and-load-images)
  - [Deploy to Kubernetes](#deploy-to-kubernetes)
  - [Access the Application](#access-the-application)
  - [Access Jaeger UI](#access-jaeger-ui)
  - [Cleanup](#cleanup)
- [Azure Kubernetes Service (AKS) Deployment](#azure-kubernetes-service-aks-deployment)
  - [Prerequisites](#prerequisites-1)
  - [Setup AKS Cluster](#setup-aks-cluster)
  - [Register Azure Resource Providers (One-time setup)](#register-azure-resource-providers-one-time-setup)
  - [Push Images to Azure Container Registry](#push-images-to-azure-container-registry)
  - [Deploy to Azure](#deploy-to-azure)
  - [Cleanup Azure Resources](#cleanup-azure-resources)
  - [Managing Multiple Clusters (Local vs Azure)](#managing-multiple-clusters-local-vs-azure)

## Deploying to Local Kubernetes (Kind)

### Prerequisites

Install the following packages:
- [Docker](https://docs.docker.com/get-docker/) - Container runtime
- [kubectl](https://kubernetes.io/docs/tasks/tools/) - Kubernetes CLI
- [Kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation) (recommended) – Local Kubernetes cluster

**Create a Kind cluster:**
```bash
kind create cluster --name microservices
```

### Build and Load Images
```bash
# Build all Docker images
docker compose build

# Load images into Kind cluster
for img in simulator quizzes; do
  kind load docker-image ${img}:latest --name microservices
done
```

### Deploy to Kubernetes
```bash
# Create namespace and RBAC
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/rbac.yaml
kubectl apply -f k8s/configmap.yaml

# Deploy infrastructure
kubectl apply -f k8s/infrastructure/rabbitmq.yaml
kubectl apply -f k8s/infrastructure/jaeger.yaml

# Wait for infrastructure to be ready
kubectl wait --for=condition=ready pod -l app=rabbitmq -n microservices-simulator --timeout=120s
kubectl wait --for=condition=ready pod -l app=jaeger -n microservices-simulator --timeout=60s

# Deploy microservices (choose one)
# For stream communication
kubectl apply -f k8s/services-stream/

# For gRPC communication
# kubectl apply -f k8s/services-grpc/

# Check status
kubectl get pods -n microservices-simulator
```
> **Note:** To change transactional model profile, edit `k8s/services-stream/` or `k8s/services-grpc/` and change the `SPRING_PROFILES_ACTIVE` environment variable of each service.

### Access the Application
```bash
# Port-forward to gateway
kubectl port-forward svc/gateway 8080:8080 -n microservices-simulator
```

### Access Jaeger UI
```bash
kubectl port-forward svc/jaeger 16686:16686 -n microservices-simulator
```
Then open [http://localhost:16686](http://localhost:16686) to view distributed traces.

### Cleanup
```bash
kubectl delete namespace microservices-simulator
```

---

## Azure Kubernetes Service (AKS) Deployment

Deploy the distributed mode to Azure Kubernetes Service for cloud-based deployments.

### Prerequisites
- [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli) installed
- Active Azure subscription (e.g., Azure for Students)

### Setup AKS Cluster
```bash
# Login to Azure
az login

# Create Resource Group
az group create --name simulator-rg-es --location spaincentral

# Create AKS Cluster (Free tier, minimal resources) -- This is a cluster example
az aks create \
  --resource-group simulator-rg-es \
  --name simulator-cluster \
  --tier free \
  --node-count 1 \
  --node-vm-size Standard_B2s_v2 \
  --generate-ssh-keys

# Connect to the Cluster
az aks get-credentials --resource-group simulator-rg-es --name simulator-cluster

# Verify connection
kubectl get nodes
```

### Register Azure Resource Providers (One-time setup)
```bash
# Register Container Registry provider (required for ACR)
az provider register --namespace Microsoft.ContainerRegistry

# Check registration status (wait until "Registered")
az provider show --namespace Microsoft.ContainerRegistry --query "registrationState"
```

### Push Images to Azure Container Registry
```bash
# Run the push script (creates ACR, attaches to AKS, pushes images)
chmod +x scripts/push-to-acr.sh
./scripts/push-to-acr.sh
```

### Deploy to Azure
```bash
# 1. Base setup
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/rbac.yaml

# 2. Infrastructure (Centralized PostgreSQL + RabbitMQ)
kubectl apply -f k8s/infrastructure/

# 3. Wait for infrastructure to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n microservices-simulator --timeout=180s
kubectl wait --for=condition=ready pod -l app=rabbitmq -n microservices-simulator --timeout=120s

# 4. Deploy Azure-optimized microservices (uses ACR images + centralized DB)
kubectl apply -f k8s/services-azure/

# 5. Check status
kubectl get pods -n microservices-simulator

# 6. Access the gateway
kubectl get svc gateway -n microservices-simulator
# Or use port-forward
kubectl port-forward -n microservices-simulator svc/gateway 8080:8080
```

**Save costs by stopping the cluster when not in use:**
```bash
# Stop the cluster
az aks stop --name simulator-cluster --resource-group simulator-rg-es

# Start the cluster again
az aks start --name simulator-cluster --resource-group simulator-rg-es
```

### Cleanup Azure Resources
```bash
# Delete the cluster
az aks delete --name simulator-cluster --resource-group simulator-rg-es

# Delete everything (including ACR)
az group delete --name simulator-rg-es
```

### Managing Multiple Clusters (Local vs Azure)
When using both local (Kind) and Cloud (Azure) clusters, your `kubectl` context may be pointing to the wrong cluster.

To see all available clusters:
```bash
kubectl config get-contexts
```
To switch back to your local Kind cluster:
```bash
kubectl config use-context kind-microservices
```
To switch to your Azure AKS cluster:
```bash
kubectl config use-context simulator-cluster
```