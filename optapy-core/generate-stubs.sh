#!/bin/sh
mvn clean
mvn dependency:copy-dependencies -Dstubs
mvn dependency:copy-dependencies -Dstubs -Dclassifier=javadoc
echo "version = '1.0'" > stubgenj/stubgenj/_version.py
python create-stubs.py
mvn clean
