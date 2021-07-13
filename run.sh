#!/bin/sh
cd optapy
python -m build
source ../optapy-school-timetabling/venv/bin/activate
pip uninstall -y optapy
pip install dist/optapy-0.0.0-py3-none-any.whl
cd ../optapy-school-timetabling
python --jvm main.py
deactivate
