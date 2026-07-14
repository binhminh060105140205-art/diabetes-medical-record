#!/bin/sh
set -e
MAVEN_VERSION=3.9.11
MAVEN_HOME="${HOME}/.m2/wrapper/dists/apache-maven-${MAVEN_VERSION}"
if [ ! -x "${MAVEN_HOME}/bin/mvn" ]; then
  mkdir -p "$(dirname "${MAVEN_HOME}")"
  archive="${TMPDIR:-/tmp}/apache-maven-${MAVEN_VERSION}.tar.gz"
  curl -fsSL "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz" -o "${archive}"
  tar -xzf "${archive}" -C "$(dirname "${MAVEN_HOME}")"
fi
exec "${MAVEN_HOME}/bin/mvn" "$@"
