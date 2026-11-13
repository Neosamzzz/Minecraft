"""Reproducible patch of the official Saplanting 0.2 release."""
import hashlib
import os
from pathlib import Path
import subprocess
import urllib.request


def build(ROOT, run_gradle, env, offline):
    source = ROOT / '.local/saplanting/saplanting-forge-1.12.2-0.2.jar'
    expected = 'b1df2dc59e49d4e46a6734220a05c2abfc050ebb'
    if not source.exists():
        if offline:
            raise ValueError('Saplanting upstream JAR is not cached; build once without --offline.')
        source.parent.mkdir(parents=True, exist_ok=True)
        urllib.request.urlretrieve('https://cdn.modrinth.com/data/Vo4uFiz0/versions/aqCYcTNx/saplanting-forge-1.12.2-0.2.jar', source)
    if hashlib.sha1(source.read_bytes()).hexdigest() != expected:
        raise ValueError('Saplanting upstream SHA1 mismatch.')
    cache = Path.home() / '.gradle/caches/modules-2/files-2.1/org.ow2.asm'
    jars = []
    for artifact in ('asm', 'asm-tree'):
        matches = sorted((cache / artifact / '9.2').glob('*/*.jar'))
        if not matches:
            raise ValueError('ASM 9.2 is required in the shared Gradle cache.')
        jars.append(str(matches[0]))
    classes = ROOT / 'project/Saplanting/build/classes'
    classes.mkdir(parents=True, exist_ok=True)
    java_bin = Path(env['JAVA_HOME']) / 'bin'
    suffix = '.exe' if os.name == 'nt' else ''
    subprocess.run([str(java_bin / ('javac' + suffix)), '-encoding', 'UTF-8', '-cp', os.pathsep.join(jars), '-d', str(classes), str(ROOT / 'project/Saplanting/Patch.java')], check=True)
    output = ROOT / 'build/libs/saplanting-forge-1.12.2-0.2-serverfix.jar'
    output.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run([str(java_bin / ('java' + suffix)), '-cp', os.pathsep.join([str(classes)] + jars), 'Patch', str(source), str(output)], check=True)
    print('Built: {}'.format(output))
