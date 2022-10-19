"""
This module acts as an interface to the Python bytecode to Java bytecode interpreter
"""
from .jvm_setup import init, set_class_output_directory
from .python_to_java_bytecode_translator import translate_python_bytecode_to_java_bytecode, \
     translate_python_class_to_java_class, convert_to_java_python_like_object, force_update_type, \
     get_java_type_for_python_type, unwrap_python_like_object, as_java, as_untyped_java, as_typed_java, is_c_native, \
     is_current_python_version_supported, check_current_python_version_supported, is_python_version_supported, \
     _force_as_java_generator
