#!/bin/bash
set -e

source $(dirname $0)/common.sh
repository=$(pwd)/distribution-repository

pushd git-repo > /dev/null
./gradlew --no-daemon -Dorg.gradle.parallel=false -PdeploymentRepository=${repository} build publishAllPublicationsToDeploymentRepository
popd > /dev/null
