#!/bin/sh
if which python > /dev/null ; then
    if [ ! -d optapy-quickstarts/school-timetabling/venv ]; then
        echo "Creating venv"
        python -m venv optapy-quickstarts/school-timetabling/venv
    fi
    cd optapy-core || { echo 'Unable to find the optapy-core directory. Maybe it was accidently deleted?'; exit 1; }
    python -m build  || { echo 'Build failed' ; exit 1; }
    . ../optapy-quickstarts/school-timetabling/venv/bin/activate
    pip uninstall -y optapy
    pip install dist/optapy-8.11.0a0-py3-none-any.whl
    cd ../optapy-quickstarts/school-timetabling || { echo 'Unable to find the optapy-quickstarts/school-timetabling directory. Maybe it was accidently deleted?'; exit 1; }
    python main.py
    deactivate
else
    echo "python is not installed"
    exit 1
fi
