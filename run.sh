#!/bin/sh
if which graalpython > /dev/null ; then
    if [ ! -d optapy-school-timetabling/venv ]; then
        echo "Creating venv"
        graalpython -m venv optapy-school-timetabling/venv
    fi
    cd optapy
    python -m build
    source ../optapy-school-timetabling/venv/bin/activate
    pip uninstall -y optapy
    pip install dist/optapy-0.0.0-py3-none-any.whl
    cd ../optapy-school-timetabling
    python --jvm main.py
    deactivate
else
    echo "graalpython is not installed"
    exit 1
fi