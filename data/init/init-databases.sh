#!/bin/bash
set -e

# Create all microservice databases
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE versiondb;
    CREATE DATABASE answerdb;
    CREATE DATABASE coursedb;
    CREATE DATABASE executiondb;
    CREATE DATABASE questiondb;
    CREATE DATABASE quizdb;
    CREATE DATABASE topicdb;
    CREATE DATABASE tournamentdb;
    CREATE DATABASE userdb;
EOSQL

echo "All microservice databases created successfully!"
