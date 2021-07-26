#!/bin/sh
if which graalpython > /dev/null ; then
    if [ ! -d optapy-quickstarts/school-timetabling/graalvenv ]; then
        echo "Creating venv"
        graalpython -m venv optapy-quickstarts/school-timetabling/graalvenv
    fi
    cd optapy-graal-core || { echo 'Unable to find the optapy-graal-core directory. Maybe it was accidently deleted?'; exit 1; }
    python -m build || { echo 'Build failed' ; exit 1; }
    . ../optapy-quickstarts/school-timetabling/graalvenv/bin/activate
    pip uninstall -y optapy
    pip install dist/optapy-0.0.0-py3-none-any.whl
    cd ../optapy-quickstarts/school-timetabling || { echo 'Unable to find the optapy-quickstarts/school-timetabling directory. Maybe it was accidently deleted?'; exit 1; }
    python --jvm main.py
    deactivate
else
    echo "graalpython is not installed"
    exit 1
fi
