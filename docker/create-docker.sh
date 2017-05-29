#!/usr/bin/env bash

DAVE_VERSION="1.0-SNAPSHOT"
DAVE_APPLICATION_NAME="dave-store-manager"
DAVE_CONFIG_FILE="${DAVE_APPLICATION_NAME}-${DAVE_VERSION}/etc/storemanager.conf"

# Copy the DAVe binaries
cp -r -v ./target/vertx-http-echo-1.0-SNAPSHOT/vertx-http-echo-1.0-SNAPSHOT ./docker/vertx-http-echo-1.0-SNAPSHOT

docker build -t scholzj/vertx-http-echo:latest ./docker/
docker tag -f scholzj/vertx-http-echo:latest docker.io/scholzj/vertx-http-echo:latest
docker push scholzj/vertx-http-echo:latest

rm -rf ./docker/vertx-http-echo-1.0-SNAPSHOT