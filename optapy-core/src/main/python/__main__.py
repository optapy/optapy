"""
You can run this module to download optapy jars.

Commands:

setup [jar-directory]: downloads jars to jar-directory (or <current-directory>/.optapy if not passed)
"""
import sys
import os
from optapy import extract_optaplanner_jars


def _setup(setup_args):
    if len(setup_args) == 0:
        extract_jars_to = os.path.join(os.getcwd(), '.optapy')
    elif len(setup_args) == 1:
        extract_jars_to = args[0]
    else:
        raise RuntimeError('The command setup accept 0 to 1 arguments. Usage: python -m optapy setup [jar-directory]')
    extract_optaplanner_jars(extract_jars_to)


_optapy_main_module_functions = {
    'setup': _setup
}

if __name__ == '__main__':
    args = sys.argv[1:]
    possible_commands = [str(key) for key in _optapy_main_module_functions.keys()]
    if len(args) == 0:
        raise RuntimeError('Command required. Possible commands are: {}'.format(possible_commands))
    if args[0] in _optapy_main_module_functions:
        _optapy_main_module_functions[args[0]](args[1:])
    else:
        raise RuntimeError('Command {} does not exist. Possible commands are: {}'
                           .format(args[0], possible_commands))
