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

lein clean
lein test
lein uberjar

lein set-version ${version}
git add project.clj

printf ${version} > VERSION
git add VERSION

git commit -m "Bumped version to $version"
git push

git tag ${version}
git push --tags

docker build -t zalando/overlord:${version} .
docker push zalando/overlord:${version}
