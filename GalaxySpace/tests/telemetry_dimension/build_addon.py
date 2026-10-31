"""Extend the 1.3 addon; preserve every previous binary patch and add telemetry."""
import json, sys, zipfile
from pathlib import Path
base, compiled, output = map(Path, sys.argv[1:])
with zipfile.ZipFile(base) as old, zipfile.ZipFile(compiled) as gs, zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as out:
    for name in old.namelist():
        if name not in ('mixins.gslaunchcompat.json','META-INF/MANIFEST.MF','mixins.galaxyspace.refmap.json'):out.writestr(name,old.read(name))
    manifest=old.read('META-INF/MANIFEST.MF').decode().rstrip('\r\n')+'\r\nFMLCorePluginContainsFMLMod: true\r\nImplementation-Version: 1.4.8\r\n\r\n'
    out.writestr('META-INF/MANIFEST.MF',manifest)
    out.writestr('pack.mcmeta',json.dumps({'pack':{'pack_format':3,'description':'GSLaunchCompat telemetry resources'}}))
    config=json.loads(old.read('mixins.gslaunchcompat.json'))
    config['mixins'].append('MixinTileEntityTelemetry')
    config['client'].extend(['MixinGameScreenText','MixinDrawGameScreen'])
    out.writestr('mixins.gslaunchcompat.json',json.dumps(config,indent=2))
    refmap=json.loads(old.read('mixins.galaxyspace.refmap.json'))
    compiled_refmap=json.loads(gs.read('mixins.galaxyspace.refmap.json'))
    screen='galaxyspace/core/mixins/MixinGameScreenText'
    if screen in compiled_refmap['mappings']:
        refmap['mappings'][screen]=compiled_refmap['mappings'][screen]
        refmap['data']['searge'][screen]=compiled_refmap['data']['searge'][screen]
    out.writestr('mixins.galaxyspace.refmap.json',json.dumps(refmap,indent=2))
    for name in gs.namelist():
        if ((name.startswith('galaxyspace/core/telemetry/') and name.endswith('.class'))
            or name in ['galaxyspace/core/mixins/MixinTileEntityTelemetry.class','galaxyspace/core/mixins/MixinGameScreenText.class','galaxyspace/core/mixins/MixinDrawGameScreen.class']
            or name.startswith('assets/gslaunchtelemetry/lang/')):
            out.writestr(name,gs.read(name))
with zipfile.ZipFile(output) as out,zipfile.ZipFile(base) as old:
    assert out.testzip() is None
    assert json.loads(out.read('pack.mcmeta'))['pack']['pack_format']==3
    assert 'gslc.dimension.line=所在维度：%s' in out.read('assets/gslaunchtelemetry/lang/zh_cn.lang').decode('utf8')
    assert b'FMLCorePluginContainsFMLMod: true' in out.read('META-INF/MANIFEST.MF')
    for name in old.namelist():
        if name not in ('mixins.gslaunchcompat.json','META-INF/MANIFEST.MF','mixins.galaxyspace.refmap.json'):assert out.read(name)==old.read(name),name
    old_refmap=json.loads(old.read('mixins.galaxyspace.refmap.json'))
    new_refmap=json.loads(out.read('mixins.galaxyspace.refmap.json'))
    for name,value in old_refmap['mappings'].items():assert new_refmap['mappings'][name]==value
    for name in out.namelist():
        if name.endswith('.class'):assert int.from_bytes(out.read(name)[6:8],'big')==52
print('Addon verified:',output)
