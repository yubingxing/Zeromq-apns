#!/bin/bash

# Build
sbt clean update compile pack package

DIST="target/dist"

# Package structure
rm -rf $DIST
mkdir -p $DIST/lib $DIST/conf
#cp lib_managed/scala_2.9.2/compile/* $DIST/lib
cp target/scala-2.9.2/zeromq-apns_2.9.2-1.0.jar $DIST/lib
cp target/scala-2.9.2/lib/* $DIST/lib
#cp target/scala_2.9.2/resources/* $DIST/conf

cp *.sh $DIST
rm $DIST/package.sh

cp src/main/resources/* $DIST/conf
chmod +x $DIST/*
