#!/usr/bin/env python3
"""Build and package GSLC using the repository's pinned toolchain."""
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


def main():
    parser = argparse.ArgumentParser(description='Build GSLaunchCompat into build/libs using JDK 8.')
    parser.add_argument('--offline', action='store_true', help='Use only already cached Gradle dependencies.')
    args = parser.parse_args()
    try:
        version = (ROOT / 'version.txt').read_text(encoding='utf-8').strip()
        if not re.fullmatch(r'\d+\.\d+\.\d+(?:[-+][A-Za-z0-9.-]+)?', version):
            raise ValueError('version.txt must contain a version such as 1.4.8.')
        env = dict(os.environ)
        check_jdk(env)
        if os.name == 'nt':
            command = [env.get('COMSPEC', 'cmd.exe'), '/d', '/c', r'GalaxySpace\gradlew.bat']
        else:
            command = ['bash', str(ROOT / 'GalaxySpace/gradlew')]
        command += ['-p', 'GalaxySpace', '--no-daemon', '--rerun-tasks']
        if args.offline:
            command.append('--offline')
        command.append('jar')
        subprocess.run(command, cwd=str(ROOT), env=env, check=True)
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
        return 0
    except (ValueError, OSError, subprocess.SubprocessError) as error:
        print('Build failed: {}'.format(error), file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
