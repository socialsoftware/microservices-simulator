#!/bin/bash
cd "$(dirname "$0")"

N=${1:-1}
for ((i=1; i<=N; i++))
do
    echo "Stopping worker_$i"
    docker compose --project-directory .. -p worker_$i -f docker-compose-parallel-training.yml --profile train --profile debug down -v --remove-orphans
done
