#!/bin/bash
cd "$(dirname "$0")"

N=${1:-1}
for ((i=0; i<=N; i++))
do
    echo "Stopping worker_$i"
    docker compose --project-directory .. -p worker_$i -f docker-compose-parallel-training.yml down -v --remove-orphans
done
