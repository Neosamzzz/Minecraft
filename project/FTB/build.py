"""Build FTB and export its current mod package."""
import re
import shutil


def build(ROOT, run_gradle, env, offline):
    properties = {}
    for line in (ROOT / 'project/FTB/gradle.properties').read_text(encoding='utf-8').splitlines():
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
    run_gradle('project/FTB', 'build', env, offline)
    artifact = ROOT / 'project/FTB/build/libs' / ('{}-{}.jar'.format(name, version))
    if not artifact.is_file():
        raise FileNotFoundError('Expected FTB Quests artifact was not produced: {}'.format(artifact))
    output = ROOT / 'build/libs' / artifact.name
    output.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(str(artifact), str(output))
    print('Built: {}'.format(output))
