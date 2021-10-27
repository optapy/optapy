#!/bin/sh
mvn dependency:copy-dependencies -Dstubs
echo "version = '1.0'" > stubgenj/stubgenj/_version.py
python create-stubs.py
mvn clean
