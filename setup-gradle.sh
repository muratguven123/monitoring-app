#!/bin/bash
# This script attempts to download and setup the Gradle wrapper JAR

set -e

GRADLE_VERSION="8.5"
WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
GRADLE_ZIP="gradle-${GRADLE_VERSION}-bin.zip"

echo "Downloading Gradle $GRADLE_VERSION..."

# Try multiple sources
SOURCES=(
  "https://downloads.gradle-dn.com/distributions/${GRADLE_ZIP}"
  "https://services.gradle.org/distributions/${GRADLE_ZIP}"
  "https://gradle.org/releases/gradle-${GRADLE_VERSION}/${GRADLE_ZIP}"
)

for source in "${SOURCES[@]}"; do
  echo "Trying: $source"
  if wget --timeout=30 "$source" 2>/dev/null || curl -f -o "$GRADLE_ZIP" "$source" 2>/dev/null; then
    echo "Downloaded successfully"
    unzip -q "$GRADLE_ZIP"
    cp "gradle-${GRADLE_VERSION}/lib/gradle-wrapper-${GRADLE_VERSION}.jar" "$WRAPPER_JAR"
    rm -rf "gradle-${GRADLE_VERSION}" "$GRADLE_ZIP"
    echo "Gradle wrapper JAR installed"
    exit 0
  fi
done

echo "Failed to download Gradle from any source"
exit 1

