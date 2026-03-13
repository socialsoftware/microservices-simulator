#!/bin/bash

# =============================================================================
# Azure Container Registry Push Script
# =============================================================================
# This script creates an ACR, attaches it to AKS, and pushes all local images
# =============================================================================

set -e

# Configuration
RESOURCE_GROUP="simulator-rg-es"
ACR_NAME="simulatorcr"
AKS_CLUSTER="simulator-cluster"

echo "=========================================="
echo "Azure Container Registry Setup & Push"
echo "=========================================="

# Step 1: Create ACR (if not exists)
echo ""
echo "[1/5] Creating Azure Container Registry..."
if az acr show --name $ACR_NAME --resource-group $RESOURCE_GROUP &>/dev/null; then
    echo "  ✓ ACR '$ACR_NAME' already exists"
else
    az acr create --resource-group $RESOURCE_GROUP --name $ACR_NAME --sku Basic
    echo "  ✓ ACR '$ACR_NAME' created"
fi

# Step 2: Attach ACR to AKS
echo ""
echo "[2/5] Attaching ACR to AKS cluster..."
az aks update -n $AKS_CLUSTER -g $RESOURCE_GROUP --attach-acr $ACR_NAME
echo "  ✓ ACR attached to AKS"

# Step 3: Login to ACR
echo ""
echo "[3/5] Logging into ACR..."
az acr login --name $ACR_NAME
echo "  ✓ Logged into ACR"

# Step 4: Tag and push images
echo ""
echo "[4/5] Tagging and pushing images..."

# Array of local images and their ACR names
declare -A IMAGES=(
    ["simulator:latest"]="simulator"
    ["quizzes:latest"]="quizzes"
)

ACR_LOGIN_SERVER="${ACR_NAME}.azurecr.io"

for LOCAL_IMAGE in "${!IMAGES[@]}"; do
    ACR_IMAGE="${ACR_LOGIN_SERVER}/${IMAGES[$LOCAL_IMAGE]}:latest"
    
    echo ""
    echo "  Processing: $LOCAL_IMAGE -> $ACR_IMAGE"
    
    # Check if local image exists
    if docker image inspect "$LOCAL_IMAGE" &>/dev/null; then
        docker tag "$LOCAL_IMAGE" "$ACR_IMAGE"
        docker push "$ACR_IMAGE"
        echo "  ✓ Pushed $ACR_IMAGE"
    else
        echo "  ⚠ WARNING: Local image '$LOCAL_IMAGE' not found - skipping"
    fi
done

# Step 5: List images in ACR
echo ""
echo "[5/5] Verifying images in ACR..."
echo ""
az acr repository list --name $ACR_NAME --output table

echo ""
echo "=========================================="
echo "✓ All done!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  1. Delete existing deployments:"
echo "     kubectl delete -f k8s/services/ -n microservices-simulator"
echo ""
echo "  2. Deploy infrastructure (PostgreSQL + RabbitMQ):"
echo "     kubectl apply -f k8s/infrastructure/"
echo ""
echo "  3. Wait for PostgreSQL to be ready:"
echo "     kubectl wait --for=condition=ready pod -l app=postgres -n microservices-simulator --timeout=120s"
echo ""
echo "  4. Deploy Azure-optimized services:"
echo "     kubectl apply -f k8s/services-azure/"
echo ""
echo "  5. Check pods:"
echo "     kubectl get pods -n microservices-simulator"
