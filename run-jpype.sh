#!/bin/sh
if which python > /dev/null ; then
    if [ ! -d optapy-school-timetabling/venv ]; then
        echo "Creating venv"
        python -m venv optapy-school-timetabling/venv
    fi
    cd optapy-jpype
    python -m build  || { echo 'Build failed' ; exit 1; }
    source ../optapy-school-timetabling/venv/bin/activate
    pip uninstall -y optapy
    pip install dist/optapy-0.0.0-py3-none-any.whl
    cd ../optapy-school-timetabling
    python main.py
    deactivate
else
    echo "python is not installed"
    exit 1
fi
