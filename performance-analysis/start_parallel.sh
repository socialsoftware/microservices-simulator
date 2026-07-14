#!/bin/bash
cd "$(dirname "$0")"

N=${1:-1}
for ((i=1; i<=N; i++))
do
    export VERSION_MODE="distributed-version"
    export GATEWAY_PORT=$((8080+i))
    export H2_PORT=$((1521+i))
    export GRPC_PORT=$((4319+i))
    export JAEGER_PORT=$((16686+i))
    echo "Starting worker_$i on Gateway Port $GATEWAY_PORT, H2 Port $H2_PORT, gRPC Port $GRPC_PORT, Jaeger Port $JAEGER_PORT"
    docker compose --project-directory .. -p worker_$i -f docker-compose-parallel-training.yml up -d quizzes-rl
done
