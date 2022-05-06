"""
This module acts as an interface to the Python bytecode to Java bytecode interpreter
"""
from .jvm_setup import init, set_class_output_directory
from .python_to_java_bytecode_translator import translate_python_bytecode_to_java_bytecode, \
     convert_to_java_python_like_object
