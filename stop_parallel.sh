#!/bin/bash
N=${1:-1}
for ((i=0; i<=N; i++))
do
    echo "Stopping worker_$i"
    docker compose -p worker_$i -f docker-compose-parallel.yml -f applications/quizzes/docker-compose-parallel.yml down -v --remove-orphans
done
