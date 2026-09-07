#!/usr/bin/env bash
set -euo pipefail
run_root="${1:?absolute private run root required}"
source_root="$run_root/docker-source"
repository="$run_root/docker-maven-repository"
build="$run_root/docker-build"
test -d "$source_root/simulator/src"
test -d "$source_root/verifiers/src"
test -d "$source_root/applications/quizzes/src"
test ! -e "$build/READY"
mkdir -p "$build/classes" "$repository"
java -version > "$build/java-version.txt" 2>&1
mvn -version > "$build/maven-version.txt" 2>&1
cd "$source_root/simulator"
mvn -q -Dmaven.repo.local="$repository" -DskipTests install
cd "$source_root/verifiers"
mvn -q -Dmaven.repo.local="$repository" -Dmaven.test.skip=true install
mvn -q -Dmaven.repo.local="$repository" dependency:build-classpath -Dmdep.outputFile="$build/verifiers-classpath.txt"
cd "$source_root/applications/quizzes"
mvn -q -Dmaven.repo.local="$repository" -Ptest-sagas -DskipTests test-compile dependency:build-classpath -Dmdep.outputFile="$build/quizzes-classpath.txt"
mvn -q -Dmaven.repo.local="$repository" -Ptest-sagas -Dtest=QuizzesSagaReadResponseAdaptersTest test
runtime_cp="$build/classes:$source_root/applications/quizzes/target/classes:$source_root/applications/quizzes/target/test-classes:$source_root/verifiers/target/classes:$(tr -d '\n' < "$build/quizzes-classpath.txt"):$(tr -d '\n' < "$build/verifiers-classpath.txt"):$run_root/memory-tool/jol-cli-0.17-full.jar"
javac -cp "$runtime_cp" -d "$build/classes" "$source_root/verifiers/experiments/saga-read-exposure/"*.java
printf '%s\n' "$runtime_cp" > "$build/runtime-classpath.txt"
printf '%s\n' ready > "$build/READY"
