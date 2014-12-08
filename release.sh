#!/bin/sh

if [ $# -ne 1 ]; then
    >&2 echo "usage: $0 <version>"
    exit 1
fi

set -xe

lein version
git --version
docker version

version=$1

lein vcs assert-committed

lein change version ${version}
git add project.clj

printf ${version} > VERSION
git add VERSION

git commit -m "Bumped version to $version"

lein clean
lein test

lein uberjar

git tag ${version}
git push --follow-tags

docker build -t zalando/overlord:${version} .
docker push zalando/overlord:${version}