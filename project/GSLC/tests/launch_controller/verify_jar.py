"""Validate the game artifact, including refmap packaging and Java 8 bytecode."""
import json
from pathlib import Path
import sys
import zipfile

path = Path(sys.argv[1])
with zipfile.ZipFile(path) as jar:
    assert jar.testzip() is None, 'Corrupt JAR entry'
    names = set(jar.namelist())
    config = json.loads(jar.read('mixins.galaxyspace.json'))
    assert config['refmap'] in names, 'Configured refmap is missing from the JAR'
    refmap = json.loads(jar.read(config['refmap']))
    assert 'galaxyspace/core/mixins/MixinWorld' in refmap['mappings'], 'World remappings are missing'
    for name in ('MixinTileEntityLaunchConroller', 'MixinEntityAutoRocket'):
        assert name in config['mixins'], 'Compatibility mixin is not registered'
        data = jar.read('galaxyspace/core/mixins/' + name + '.class')
        assert data[:4] == b'\xca\xfe\xba\xbe' and int.from_bytes(data[6:8], 'big') == 52, 'Expected Java 8 bytecode'
    assert 'galaxyspace/core/util/LaunchControllerCompatibility.class' in names
    manifest = jar.read('META-INF/MANIFEST.MF').decode()
    assert 'MixinConfigs: mixins.galaxyspace.json' in manifest, 'GS mixin config is not in the manifest'
print('Game JAR structure, refmap, mixin registration and Java 8 bytecode: PASS')
