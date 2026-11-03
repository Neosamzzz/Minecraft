"""Build GSLC and export its current mod package."""
import re
import subprocess
import sys


def build(ROOT, run_gradle, env, offline):
    version = (ROOT / 'project/GSLC/version.txt').read_text(encoding='utf-8').strip()
    if not re.fullmatch(r'\d+\.\d+\.\d+(?:[-+][A-Za-z0-9.-]+)?', version):
        raise ValueError('version.txt must contain a version such as 1.4.8.')
    run_gradle('dependencies/GalaxySpace', 'jar', env, offline, rerun=True)
    output = ROOT / 'build/libs' / ('GSLaunchCompat-' + version + '.jar')
    output.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run([
        sys.executable,
        str(ROOT / 'project/GSLC/tests/telemetry_dimension/build_addon.py'),
        str(ROOT / 'project/GSLC/materials/GSLaunchCompat-1.3-public-controller.jar'),
        str(ROOT / 'dependencies/GalaxySpace/build/libs/All-1.12.2.jar'),
        str(output), version,
    ], cwd=str(ROOT), env=env, check=True)
    print('Built: {}'.format(output))
