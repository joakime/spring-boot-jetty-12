#!/bin/bash
set -e

source $(dirname $0)/common.sh
repository=$(pwd)/distribution-repository

pushd git-repo > /dev/null
if [[ -d /opt/openjdk-toolchain ]]; then
  ./gradlew -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false --no-daemon --no-build-cache --max-workers=4 -PdeploymentRepository=${repository} build -PtoolchainVersion=${TOOLCHAIN_JAVA_VERSION} -Porg.gradle.java.installations.auto-detect=false -Porg.gradle.java.installations.auto-download=false -Porg.gradle.java.installations.paths=/opt/openjdk-toolchain/
else
  ./gradlew -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false --no-daemon --no-build-cache --max-workers=4 -PdeploymentRepository=${repository} build
fi
popd > /dev/null
