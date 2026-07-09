#!/bin/bash
N=${1:-1}
for ((i=0; i<=N; i++))
do
    export VERSION_MODE="distributed-version"
    export GATEWAY_PORT=$((8080+i))
    export H2_PORT=$((1521+i))
    export GRPC_PORT=$((4319+i))
    export JAEGER_PORT=$((16686+i))
    echo "Starting worker_$i on Gateway Port $GATEWAY_PORT, H2 Port $H2_PORT, gRPC Port $GRPC_PORT, Jaeger Port $JAEGER_PORT"
    docker compose -p worker_$i -f docker-compose-parallel.yml -f applications/quizzes/docker-compose-parallel.yml up -d quizzes-rl
done
