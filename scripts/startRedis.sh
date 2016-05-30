#!/bin/sh

name=$1
serverPort=$2

docker ps -a -q -f name="$name" | xargs docker rm -f

DOCKER_IMAGE="jamespedwards42/alpine-redis-testing:unstable"
MOUNT_MODULES="$(pwd)/redis/modules:/redis/modules"
REDIS_CONTAINER_NAME="$name-$serverPort"

docker run -d --name="$REDIS_CONTAINER_NAME" -p "$serverPort:$serverPort" -v "$MOUNT_MODULES" "$DOCKER_IMAGE" "$serverPort" 1 \
   --protected-mode no --save \"\" --repl-diskless-sync yes --appendfsync no --activerehashing no

exit 0
