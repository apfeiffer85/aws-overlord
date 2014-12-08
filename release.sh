#!/bin/sh

if [ $# -ne 1 ]; then
    >&2 echo "usage: $0 <version>"
    exit 1
fi

set -xe

version=$1

lein vcs assert-committed

lein clean
lein test

lein uberjar

git tag ${version}
git push --tags

docker build -t zalando/overlord:${version} .
docker push zalando/overlord:${version}