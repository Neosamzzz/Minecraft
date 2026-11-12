#!/usr/bin/env python3
"""Build project mods with their pinned toolchains into build/libs."""
import argparse
import importlib.util
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def load_builder(folder, entrypoint='build'):
    path = Path(__file__).resolve().parents[1] / 'project' / folder / 'build.py'
    spec = importlib.util.spec_from_file_location(folder + '_build', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return getattr(module, entrypoint)


BUILDERS = {name: load_builder(folder) for name, folder in
            (('gslc', 'GSLC'), ('ftbquests', 'FTB'), ('newearth', 'NewEarth'))}
BUILDERS['newearth-server'] = load_builder('NewEarth', 'build_server')


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


def run_gradle(project, task, env, offline, rerun=False, wrapper=None):
    wrapper = wrapper or project
    if os.name == 'nt':
        command = [env.get('COMSPEC', 'cmd.exe'), '/d', '/c', str(Path(wrapper) / 'gradlew.bat')]
    else:
        command = ['bash', str(ROOT / wrapper / 'gradlew')]
    command += ['-p', project, '--no-daemon', '--console=plain']
    if rerun:
        command.append('--rerun-tasks')
    if offline:
        command.append('--offline')
    command.append(task)
    subprocess.run(command, cwd=str(ROOT), env=env, check=True)


def main():
    parser = argparse.ArgumentParser(
        description='Build GSLaunchCompat, FTB Quests and NewEarth into build/libs using JDK 8.',
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    parser.add_argument('--target', choices=('gslc', 'ftbquests', 'newearth', 'newearth-server', 'all'), default='gslc',
                        help='Build target; all builds GSLC, FTB Quests, then NewEarth.')
    parser.add_argument('--offline', action='store_true', help='Use only already cached Gradle dependencies.')
    args = parser.parse_args()
    try:
        env = dict(os.environ)
        check_jdk(env)
        targets = ('gslc', 'ftbquests', 'newearth') if args.target == 'all' else (args.target,)
        for target in targets:
            print('Building {}...'.format(target), flush=True)
            BUILDERS[target](ROOT, run_gradle, env, args.offline)
        return 0
    except (ValueError, OSError, zipfile.BadZipFile, subprocess.SubprocessError) as error:
        print('Build failed: {}'.format(error), file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
