#!/usr/bin/env bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BUILDROOT=$DIR/..

cd $BUILDROOT

GIT_HEAD="$(git rev-parse --short=7 HEAD)"
GIT_DATE=$(git log HEAD -n1 --pretty='format:%cd' --date=format:'%Y%m%d-%H%M')

REPO="dakepilogue"
CONTAINER="reactor-processor-flux-framework"
TAG="${GIT_HEAD}-${GIT_DATE}"

IMAGE_NAME="${REPO}/${CONTAINER}:${TAG}"

# Build docker --no-cache
cmd="docker build -t $IMAGE_NAME -f $DIR/Dockerfile $BUILDROOT"
echo $cmd
eval $cmd
# remove docker image
cmd="docker rmi $IMAGE_NAME"
echo $cmd
eval $cmd