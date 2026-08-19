#!/bin/bash
set -e
cd "$(dirname "$0")"

N=${1:-1}
PROFILE=${2:-train} # Defaults to 'train' if not provided

docker compose --project-directory .. -f docker-compose-parallel-training.yml --profile "$PROFILE" build quizzes-"$PROFILE"

for ((i=1; i<=N; i++))
do
    export VERSION_MODE="distributed-version"
    export GATEWAY_PORT=$((8080+i))
    export H2_PORT=$((1521+i))
    export GRPC_PORT=$((4319+i))
    export JAEGER_PORT=$((16686+i))
    echo "Starting martim03_$i on Gateway Port $GATEWAY_PORT, H2 Port $H2_PORT, gRPC Port $GRPC_PORT, Jaeger Port $JAEGER_PORT with profile: $PROFILE"
    docker compose --project-directory .. -p martim03_$i -f docker-compose-parallel-training.yml --profile $PROFILE up -d quizzes-$PROFILE
    sleep 5
done
