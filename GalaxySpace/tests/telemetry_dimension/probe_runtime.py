"""Prepare an existing runtime for isolated probes, not a full Forge launch."""
import json
import os
from pathlib import Path
import shutil


def java_tool(name):
    """Use the configured JDK, falling back to a tool on PATH."""
    java_home = os.environ.get('JAVA_HOME')
    if java_home:
        executable = Path(java_home).expanduser() / 'bin' / (
            name + ('.exe' if os.name == 'nt' else ''))
        if not executable.is_file():
            raise FileNotFoundError('Missing JDK tool: ' + str(executable))
        return str(executable.resolve())
    executable = shutil.which(name)
    if executable is None:
        raise FileNotFoundError('Set JAVA_HOME to JDK 8 or add ' + name + ' to PATH')
    return executable


def load_classpath(runtime):
    """Read portable JSON paths; retain legacy, host-specific text support."""
    json_file = runtime / 'classpath.json'
    if json_file.is_file():
        paths = json.loads(json_file.read_text(encoding='utf-8'))
        if not isinstance(paths, list) or not all(
                isinstance(path, str) and path.strip() for path in paths):
            raise ValueError(str(json_file) + ' must contain an array of nonempty paths')
    else:
        paths = [path.strip() for path in
                 (runtime / 'classpath-runtime.txt').read_text(encoding='utf-8')
                 .strip().split(os.pathsep) if path.strip()]
    resolved = []
    for value in paths:
        path = Path(value).expanduser()
        if not path.is_absolute():
            path = runtime / path
        path = path.resolve()
        if path.name.lower().startswith('gslaunchcompat-'):
            continue
        if not path.exists():
            raise FileNotFoundError('Missing probe dependency: ' + str(path))
        resolved.append(str(path))
    if not resolved:
        raise ValueError('Probe runtime classpath is empty: ' + str(runtime))
    return resolved


def prepare(runtime_arg, addon_arg, work_arg):
    runtime, addon, work = (
        Path(value).expanduser().resolve() for value in (runtime_arg, addon_arg, work_arg))
    if not addon.is_file():
        raise FileNotFoundError('Missing candidate addon: ' + str(addon))
    paths = load_classpath(runtime)
    java, javac = java_tool('java'), java_tool('javac')
    work.mkdir(parents=True, exist_ok=True)
    # MixinBooter also injects into Forge's startup. Those injections require a
    # full Forge process and are intentionally excluded from this isolated probe.
    # This resource overrides Booter's init config via the first classpath entry.
    (work / 'mixin.mixinbooter.init.json').write_text(json.dumps({
        'required': True,
        'minVersion': '0.8',
        'package': 'zone.rong.mixinbooter.mixin',
        'compatibilityLevel': 'JAVA_8',
        'mixins': [],
    }), encoding='utf-8')
    cp = os.pathsep.join([str(work), str(addon)] + paths)
    return java, javac, cp, work
