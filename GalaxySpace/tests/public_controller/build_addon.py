"""Add client GUI access patches while retaining the existing flight compatibility bytes."""
import json
from pathlib import Path
import sys
import zipfile

base, compiled, output = map(Path, sys.argv[1:])
clients = ['MixinGuiLaunchController', 'MixinGuiLaunchControllerAdvanced']
with zipfile.ZipFile(base) as old, zipfile.ZipFile(compiled) as gs, zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as out:
    for name in old.namelist():
        if name != 'mixins.gslaunchcompat.json':
            out.writestr(name, old.read(name))
    config = json.loads(old.read('mixins.gslaunchcompat.json'))
    config['client'] = clients
    out.writestr('mixins.gslaunchcompat.json', json.dumps(config, indent=2))
    for cls in clients:
        name = 'galaxyspace/core/mixins/' + cls + '.class'
        out.writestr(name, gs.read(name))
with zipfile.ZipFile(output) as out, zipfile.ZipFile(base) as old:
    assert out.testzip() is None
    for name in old.namelist():
        if name != 'mixins.gslaunchcompat.json':
            assert out.read(name) == old.read(name), name
    for cls in clients:
        data = out.read('galaxyspace/core/mixins/' + cls + '.class')
        assert int.from_bytes(data[6:8], 'big') == 52
print(output, ': original flight patch bytes retained; new client patches verified')
