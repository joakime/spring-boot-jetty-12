#!/bin/bash
set -e

source $(dirname $0)/common.sh
repository=$(pwd)/distribution-repository

pushd git-repo > /dev/null
./gradlew --no-daemon -PdeploymentRepository=${repository} clean build publishAllPublicationsToDeploymentRepository
popd > /dev/null
