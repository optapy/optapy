#!/bin/bash

# This script copies the antora-template.yml with asciidoc attributes substituted by a maven build to src/antora.yml
# in optaplanner-docs.

# Absolute path to this script, e.g. /home/user/bin/foo.sh
script=$(readlink -f "$0")
# Absolute path this script is in, thus /home/user/bin
this_script_directory=$(dirname "$script")

readonly optapy_docs=$this_script_directory
readonly antora_yml=$optapy_docs/src/antora.yml
readonly antora_yml_template=$optapy_docs/target/antora-template.yml

if [ ! -f "$antora_yml_template" ]; then
    echo "The $antora_yml_template with substituted attributes was not found. Maybe build the optapy-docs first."
    exit 1
fi

cp "$antora_yml_template" "$antora_yml"