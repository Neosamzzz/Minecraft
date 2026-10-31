"""Exercise the actual packaging CLI and the build entry's JDK guard."""
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import zipfile
import json

ROOT = Path(__file__).resolve().parents[2]
PACKAGER = ROOT / 'GalaxySpace/tests/telemetry_dimension/build_addon.py'


class PackagingTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.work = Path(self.tmp.name)
        self.packager = self.work / 'GalaxySpace/tests/telemetry_dimension/build_addon.py'
        self.packager.parent.mkdir(parents=True)
        self.packager.write_bytes(PACKAGER.read_bytes())
        (self.work / 'version.txt').write_text('9.8.7\n')
        self.base = self.work / 'base.jar'
        self.compiled = self.work / 'compiled.jar'
        self.output = self.work / 'output.jar'
        self.original = b'preserved baseline binary'
        with zipfile.ZipFile(self.base, 'w') as jar:
            jar.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\r\n\r\n')
            jar.writestr('mixins.gslaunchcompat.json', json.dumps({'mixins': [], 'client': []}))
            jar.writestr('mixins.galaxyspace.refmap.json', json.dumps({'mappings': {'old': {}}, 'data': {'searge': {'old': {}}}}))
            jar.writestr('baseline.bin', self.original)
        with zipfile.ZipFile(self.compiled, 'w') as jar:
            jar.writestr('mixins.galaxyspace.refmap.json', json.dumps({'mappings': {}, 'data': {'searge': {}}}))
            jar.writestr('galaxyspace/core/telemetry/Sample.class', bytes.fromhex('cafebabe00000034'))
            jar.writestr('assets/gslaunchtelemetry/lang/zh_cn.lang', 'gslc.dimension.line=所在维度：%s\n')

    def package(self, *version):
        return subprocess.run([sys.executable, str(self.packager), str(self.base), str(self.compiled), str(self.output), *version], capture_output=True, text=True)

    def test_three_arguments_read_repository_version(self):
        result = self.package()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.check_package('9.8.7')

    def test_explicit_version_changes_manifest_without_changing_baseline(self):
        result = self.package('2.3.4-test')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.check_package('2.3.4-test')

    def test_invalid_version_is_rejected_before_writing_package(self):
        result = self.package('bad/version')
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(self.output.exists())

    def check_package(self, version):
        with zipfile.ZipFile(self.output) as jar:
            self.assertEqual(jar.read('baseline.bin'), self.original)
            self.assertIn(('Implementation-Version: ' + version).encode(), jar.read('META-INF/MANIFEST.MF'))
            self.assertIn('galaxyspace/core/telemetry/Sample.class', jar.namelist())
            self.assertIsNone(jar.testzip())


class BuildEntryTests(unittest.TestCase):
    def test_invalid_java_home_has_actionable_error_before_gradle(self):
        with tempfile.TemporaryDirectory() as tmp:
            env = dict(os.environ, JAVA_HOME=tmp)
            result = subprocess.run([sys.executable, str(ROOT / 'tools/build.py'), '--offline'], env=env, capture_output=True, text=True)
        self.assertEqual(result.returncode, 1)
        self.assertIn('JDK 8', result.stderr)


if __name__ == '__main__':
    unittest.main()
