#!/bin/bash
set -e
mvn -q dependency:copy-dependencies \
  -DincludeScope=runtime \
  -DoutputDirectory=layer/java/lib
