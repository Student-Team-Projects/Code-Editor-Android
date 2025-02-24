#!/bin/bash
set -e
chmod +x gradlew

./gradlew clean
./gradlew assembleRelease