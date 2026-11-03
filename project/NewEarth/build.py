"""Build NewEarth and export its current mod package."""
import json
import re
import shutil
import zipfile


def build(ROOT, run_gradle, env, offline):
    properties = (ROOT / 'project/NewEarth/gradle.properties').read_text(encoding='utf-8')
    match = re.search(r'^mod_version=([A-Za-z0-9][A-Za-z0-9._+-]*)$', properties, re.MULTILINE)
    if not match:
        raise ValueError('Invalid NewEarth mod_version.')
    version = match.group(1)
    run_gradle('project/NewEarth', 'build', env, offline, wrapper='dependencies/GalaxySpace')
    artifact = ROOT / 'project/NewEarth/build/libs' / ('NewEarth-' + version + '.jar')
    if not artifact.is_file():
        raise FileNotFoundError('Expected NewEarth artifact was not produced: {}'.format(artifact))
    with zipfile.ZipFile(artifact) as jar:
        metadata = json.loads(jar.read('mcmod.info').decode('utf-8'))
        if len(metadata) != 1 or metadata[0].get('modid') != 'newearth' or metadata[0].get('version') != version:
            raise ValueError('NewEarth metadata does not match the requested version.')
        if 'newearth/NewEarth.class' not in jar.namelist():
            raise ValueError('NewEarth artifact has no mod entry point.')
    output = ROOT / 'build/libs' / artifact.name
    output.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(str(artifact), str(output))
    print('Built: {}'.format(output))
