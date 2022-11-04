import time
import xml.etree.ElementTree as ET
import pathlib
from typing import List
from dataclasses import dataclass


@dataclass
class CoverageInfo:
    line_count: int
    hit_lines: int
    branch_count: int
    hit_branches: int

    def get_line_rate(self):
        return f'{max(1, self.hit_lines) / max(1, self.line_count):.9g}'

    def get_branch_rate(self):
        return f'{max(1, self.hit_branches) / max(1, self.branch_count):.9g}'

    def merge(self, other: 'CoverageInfo') -> 'CoverageInfo':
        return CoverageInfo(self.line_count + other.line_count,
                            self.hit_lines + other.hit_lines,
                            self.branch_count + other.branch_count,
                            self.hit_branches + other.hit_branches)


def fix_package_name(name: str) -> str:
    try:
        site_package_index = name.index('.site-packages.')
        index_offset = len('.site-packages.')
        return name[site_package_index + index_offset:]
    except ValueError:
        return name


def fix_file_name(file_name: str) -> str:
    if 'optapy' in file_name:
        optapy_index = file_name.index('optapy')
        offset = optapy_index + len('optapy')
        return str(pathlib.Path('optapy-core', 'src', 'main', 'python')) + file_name[offset:]
    elif 'jpyinterpreter' in file_name:
        jpyinterpreter_index = file_name.index('jpyinterpreter')
        offset = jpyinterpreter_index + len('jpyinterpreter')
        return str(pathlib.Path('jpyinterpreter', 'src', 'main', 'python')) + file_name[offset:]
    else:
        return file_name


def merge_packages(canon_package: ET.Element, identical_packages: List[ET.Element]) -> CoverageInfo:
    coverage_info = CoverageInfo(0, 0, 0, 0)
    for canon_file in canon_package.find('classes').findall('class'):
        file_id = canon_file.get('name')
        identical_files = [package.find('classes').find(f'.//class[@name="{file_id}"]')
                           for package in identical_packages]
        coverage_info = coverage_info.merge(merge_files(canon_file, identical_files))

    canon_package.set('line-rate', coverage_info.get_line_rate())
    canon_package.set('branch-rate', coverage_info.get_branch_rate())
    return coverage_info


def merge_files(canon_file: ET.Element, identical_files: List[ET.Element]) -> CoverageInfo:
    coverage_info = CoverageInfo(0, 0, 0, 0)
    for canon_line in canon_file.find('lines').findall('line'):
        line_id = canon_line.get('number')
        identical_lines = [file.find('lines').find(f'.//line[@number="{line_id}"]')
                           for file in identical_files]
        coverage_info = coverage_info.merge(merge_lines(canon_line, identical_lines))

    canon_file.set('line-rate', coverage_info.get_line_rate())
    canon_file.set('branch-rate', coverage_info.get_branch_rate())
    return coverage_info


def merge_lines(canon_line: ET.Element, identical_lines: List[ET.Element]) -> CoverageInfo:
    is_hit = False
    for line in identical_lines:
        if line.get('hits') == '1':
            is_hit = True
            break

    canon_line.set('hits', '1' if is_hit else '0')
    is_branch = canon_line.get('branch', 'false') == 'true'
    if is_branch:
        missed_branches = canon_line.get('missing-branches', '')
        condition_coverage = canon_line.get('condition-coverage')
        num_of_conditions = int(condition_coverage[condition_coverage.index('/') + 1:-1])
        missing_branch_set = set(missed_branches.split(',')) if missed_branches != '' else set()
        for line in identical_lines:
            line_missed_branches = line.get('missing-branches', '')
            line_missing_branch_set = set(line_missed_branches.split(',')) if line_missed_branches != '' else set()
            missing_branch_set = missing_branch_set.intersection(line_missing_branch_set)

        if len(missing_branch_set) == 0:
            num_of_hit_branches = num_of_conditions
            canon_line.attrib.pop('missing-branches', None)
        else:
            num_of_hit_branches = num_of_conditions - len(missing_branch_set)
            canon_line.set('condition-coverage',
                           f'{(num_of_hit_branches / num_of_conditions) * 100:.9g}% ({num_of_hit_branches}/{num_of_conditions})')
            canon_line.set('missing-branches', ','.join(sorted(missing_branch_set)))

        return CoverageInfo(1, 1 if is_hit else 0, num_of_conditions, num_of_hit_branches)
    else:
        return CoverageInfo(1, 1 if is_hit else 0, 0, 0)


def is_test_package(package_name: str) -> bool:
    if package_name == 'tests':
        return True
    elif package_name.startswith('tests.'):
        return True
    elif package_name.endswith('.tests'):
        return True
    elif '.tests.' in package_name:
        return True
    return False


def is_ignored(package_name: str, ignore_jpyinterpreter: bool = False):
    # optapy packages start with 'optapy.' (or just plain 'optapy'), not 'optapy-core'
    return is_test_package(package_name) or (ignore_jpyinterpreter and package_name.startswith('jpyinterpreter')) or \
           package_name.startswith('optapy-core') or package_name.endswith('.jars')


def update_coverage_xml(coverage_xml: ET.ElementTree, *, ignore_jpyinterpreter: bool = False):
    # First, fix the paths that use venv paths
    packages_element = coverage_xml.getroot().find('packages')
    for package in packages_element.findall('package'):
        package_name = package.get('name')
        package.set('name', fix_package_name(package_name))
        for file in package.find('classes').findall('class'):
            file_name = file.get('filename')
            file.set('filename', fix_file_name(file_name))

    # Now merge all elements that represent the same packages/files
    canonical_package_dict = dict()
    package_name_to_packages = dict()
    for package in packages_element.findall('package'):
        package_name = package.get('name')
        if package_name not in canonical_package_dict:
            canonical_package_dict[package_name] = package
            package_name_to_packages[package_name] = [package]
        else:
            package_name_to_packages[package_name].append(package)

    coverage_info = CoverageInfo(0, 0, 0, 0)
    for package_name, identical_packages in package_name_to_packages.items():
        canon_package = canonical_package_dict[package_name]
        merged_package_coverage = merge_packages(canon_package, identical_packages)
        if not is_ignored(package_name, ignore_jpyinterpreter=ignore_jpyinterpreter):
            # do not include tests in coverage
            coverage_info = coverage_info.merge(merged_package_coverage)

    coverage_element = coverage_xml.getroot()
    coverage_element.set('lines-valid', str(coverage_info.line_count))
    coverage_element.set('lines-covered', str(coverage_info.hit_lines))
    coverage_element.set('line-rate', str(coverage_info.get_line_rate()))
    coverage_element.set('branches-valid', str(coverage_info.branch_count))
    coverage_element.set('branches-covered', str(coverage_info.hit_branches))
    coverage_element.set('branch-rate', str(coverage_info.get_branch_rate()))

    #  Remove uncanonical elements from the tree
    for package_name, identical_packages in package_name_to_packages.items():
        canon_package = canonical_package_dict[package_name]
        for package in identical_packages:
            if package != canon_package:
                packages_element.remove(package)

        if is_ignored(package_name, ignore_jpyinterpreter=ignore_jpyinterpreter):
            #  We do not want to include the tests package in results
            packages_element.remove(canon_package)


def merge_coverage_xml(*coverage_xmls: ET.ElementTree):
    root_element = ET.Element('coverage')
    sources_element = ET.Element('sources')

    source = ET.Element('source')
    source.text = str(pathlib.Path('.').resolve())
    sources_element.append(source)

    root_element.append(sources_element)
    packages_element = ET.Element('packages')
    root_element.append(packages_element)

    coverage_info = CoverageInfo(0, 0, 0, 0)
    for coverage_xml in coverage_xmls:
        xml_coverage_info = CoverageInfo(int(coverage_xml.getroot().get('lines-valid')),
                                         int(coverage_xml.getroot().get('lines-covered')),
                                         int(coverage_xml.getroot().get('branches-valid')),
                                         int(coverage_xml.getroot().get('branches-covered')))
        xml_packages_element = coverage_xml.getroot().find('packages')
        for package in xml_packages_element.findall('package'):
            packages_element.append(package)

        coverage_info = coverage_info.merge(xml_coverage_info)


    root_element.set('version', '6.5.0')
    root_element.set('timestamp', str(int(time.time())))
    root_element.set('lines-valid', str(coverage_info.line_count))
    root_element.set('lines-covered', str(coverage_info.hit_lines))
    root_element.set('line-rate', str(coverage_info.get_line_rate()))
    root_element.set('branches-valid', str(coverage_info.branch_count))
    root_element.set('branches-covered', str(coverage_info.hit_branches))
    root_element.set('branch-rate', str(coverage_info.get_branch_rate()))
    root_element.set('complexity', '0')

    return ET.ElementTree(root_element)

if __name__ == '__main__':
    optapy_coverage_xml = ET.parse('target/coverage.xml')
    jpyinterpreter_coverage_xml = ET.parse('jpyinterpreter/target/coverage.xml')

    update_coverage_xml(optapy_coverage_xml, ignore_jpyinterpreter=True)
    update_coverage_xml(jpyinterpreter_coverage_xml)

    merge_coverage_xml(optapy_coverage_xml, jpyinterpreter_coverage_xml).write('target/coverage.xml')
