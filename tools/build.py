#!/usr/bin/env python3
"""Build GSLC and FTB Quests with their pinned toolchains into build/libs."""
import argparse
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def check_jdk(env):
    """Reject missing JDKs and Java versions incompatible with this project."""
    suffix = '.exe' if os.name == 'nt' else ''
    java_home = env.get('JAVA_HOME')
    for tool in ('java', 'javac'):
        executable = str(Path(java_home).expanduser() / 'bin' / (tool + suffix)) if java_home else shutil.which(tool)
        if not executable or not Path(executable).is_file():
            raise ValueError('JDK 8 is required: set JAVA_HOME to a JDK 8 installation containing java and javac.')
        result = subprocess.run([executable, '-version'], env=env, capture_output=True, text=True, timeout=20)
        version = result.stdout + result.stderr
        if result.returncode or not re.search(r'\b1\.8(?:[._"\s]|$)', version):
            raise ValueError('JDK 8 is required; {} reported: {}'.format(tool, version.strip()))
    if java_home:
        env['JAVA_HOME'] = str(Path(java_home).expanduser().absolute())


def run_gradle(project, task, env, offline, rerun=False):
    if os.name == 'nt':
        command = [env.get('COMSPEC', 'cmd.exe'), '/d', '/c', project + r'\gradlew.bat']
    else:
        command = ['bash', str(ROOT / project / 'gradlew')]
    command += ['-p', project, '--no-daemon', '--console=plain']
    if rerun:
        command.append('--rerun-tasks')
    if offline:
        command.append('--offline')
    command.append(task)
    subprocess.run(command, cwd=str(ROOT), env=env, check=True)


def build_gslc(env, offline):
    version = (ROOT / 'version.txt').read_text(encoding='utf-8').strip()
    if not re.fullmatch(r'\d+\.\d+\.\d+(?:[-+][A-Za-z0-9.-]+)?', version):
        raise ValueError('version.txt must contain a version such as 1.4.8.')
    run_gradle('GalaxySpace', 'jar', env, offline, rerun=True)
    output = ROOT / 'build/libs' / ('GSLaunchCompat-' + version + '.jar')
    output.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run([
        sys.executable,
        str(ROOT / 'GalaxySpace/tests/telemetry_dimension/build_addon.py'),
        str(ROOT / 'dev-tools/GSLaunchCompat-1.3-public-controller.jar'),
        str(ROOT / 'GalaxySpace/build/libs/All-1.12.2.jar'),
        str(output), version,
    ], cwd=str(ROOT), env=env, check=True)
    print('Built: {}'.format(output))


def build_ftbquests(env, offline):
    properties = {}
    for line in (ROOT / 'FTB-Quests/gradle.properties').read_text(encoding='utf-8').splitlines():
        line = line.strip()
        if line and not line.startswith(('#', '!')) and '=' in line:
            key, value = line.split('=', 1)
            properties[key.strip()] = value.strip()
    name = properties.get('mod_name', '')
    version = properties.get('mod_version', '')
    if env.get('BUILD_NUMBER'):
        version += '.' + env['BUILD_NUMBER']
    for label, value in (('mod_name', name), ('mod_version / BUILD_NUMBER', version)):
        if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._+-]*', value):
            raise ValueError('Invalid FTB Quests {}: {!r}'.format(label, value))
    run_gradle('FTB-Quests', 'build', env, offline)
    artifact = ROOT / 'FTB-Quests/build/libs' / ('{}-{}.jar'.format(name, version))
    if not artifact.is_file():
        raise FileNotFoundError('Expected FTB Quests artifact was not produced: {}'.format(artifact))
    output = ROOT / 'build/libs' / artifact.name
    output.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(str(artifact), str(output))
    print('Built: {}'.format(output))


def main():
    parser = argparse.ArgumentParser(
        description='Build GSLaunchCompat and/or FTB Quests into build/libs using JDK 8.',
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    parser.add_argument('--target', choices=('gslc', 'ftbquests', 'all'), default='gslc',
                        help='Build target; all builds GSLC followed by FTB Quests.')
    parser.add_argument('--offline', action='store_true', help='Use only already cached Gradle dependencies.')
    args = parser.parse_args()
    try:
        env = dict(os.environ)
        check_jdk(env)
        targets = ('gslc', 'ftbquests') if args.target == 'all' else (args.target,)
        builders = {'gslc': build_gslc, 'ftbquests': build_ftbquests}
        for target in targets:
            print('Building {}...'.format(target), flush=True)
            builders[target](env, args.offline)
        return 0
    except (ValueError, OSError, subprocess.SubprocessError) as error:
        print('Build failed: {}'.format(error), file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
