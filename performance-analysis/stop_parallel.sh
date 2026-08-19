#!/bin/bash
cd "$(dirname "$0")"

N=${1:-1}
for ((i=1; i<=N; i++))
do
    echo "Stopping martim03_$i"
    docker compose --project-directory .. -p martim03_$i -f docker-compose-parallel-training.yml --profile train --profile debug down -t 1 -v --remove-orphans
    sleep 0.5
done