"""Exercise the actual packaging CLI and the build entry's JDK guard."""
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import zipfile
import json
import contextlib
import importlib.util
import io
from unittest import mock

ROOT = Path(__file__).resolve().parents[2]
PACKAGER = ROOT / 'project/GSLC/tests/telemetry_dimension/build_addon.py'

SPEC = importlib.util.spec_from_file_location('build_entry', ROOT / 'tools/build.py')
BUILD = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(BUILD)


class PackagingTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.work = Path(self.tmp.name)
        self.packager = self.work / 'project/GSLC/tests/telemetry_dimension/build_addon.py'
        self.packager.parent.mkdir(parents=True)
        self.packager.write_bytes(PACKAGER.read_bytes())
        (self.work / 'project/GSLC').mkdir(parents=True, exist_ok=True)
        (self.work / 'project/GSLC/version.txt').write_text('9.8.7\n')
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


class BuildTargetTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.work = Path(self.tmp.name)
        self.ftb = self.work / 'project/FTB'
        self.ftb.mkdir(parents=True)
        (self.ftb / 'gradle.properties').write_text('mod_name=FTBQuests\nmod_version=1202.9.0\n')
        (self.ftb / 'build/libs').mkdir(parents=True)
        # Old versions and sources must never be mistaken for the current mod JAR.
        (self.ftb / 'build/libs/FTBQuests-1202.8.0.jar').write_bytes(b'stale mod')
        (self.ftb / 'build/libs/FTBQuests-1202.9.0-sources.jar').write_bytes(b'sources')
        self.commands = []
        self.build_number = ''
        self.emit_ftb = True
        self.newearth = self.work / 'project/NewEarth'
        self.newearth.mkdir(parents=True)
        (self.newearth / 'gradle.properties').write_text('mod_version=0.1.0\n')
        (self.newearth / 'build/libs').mkdir(parents=True)
        self.emit_newearth = True
        source=self.newearth / 'src/main/java/newearth/NewEarth.java'
        source.parent.mkdir(parents=True)
        source.write_text('public static final String VERSION = "0.1.0";')
        (self.work / 'config/GalaxySpace').mkdir(parents=True)
        (self.work / 'config/ExtraPlanets.cfg').write_text('fixture')
        (self.work / 'config/GalaxySpace/dimensions.conf').write_text('fixture')

    def fake_subprocess(self, command, **kwargs):
        if command[-1] == '-version':
            return subprocess.CompletedProcess(command, 0, stdout='openjdk version "25.0.1"', stderr='')
        self.commands.append(command)
        if '-p' in command:
            project = command[command.index('-p') + 1]
            if project == 'project/FTB' and self.emit_ftb:
                suffix = '.' + self.build_number if self.build_number else ''
                (self.ftb / ('build/libs/FTBQuests-1202.9.0' + suffix + '.jar')).write_bytes(b'current mod')
            elif project == 'project/NewEarth' and self.emit_newearth:
                with zipfile.ZipFile(self.newearth / 'build/libs/NewEarth-0.1.0.jar', 'w') as jar:
                    jar.writestr('mcmod.info', json.dumps([{'modid': 'newearth', 'version': '0.1.0'}]))
                    jar.writestr('newearth/NewEarth.class', b'entry point')
                    terrain=io.BytesIO()
                    with zipfile.ZipFile(terrain,'w') as core:
                        core.writestr('newearth/terrain/TerrainEngine.class',bytes.fromhex('cafebabe00000045'))
                        for resource in ['default-preset.json','biomes.png','biomes.txt']:
                            core.writestr('newearth/terrain/'+resource,b'fixture')
                    jar.writestr('META-INF/newearth-terrain.jar',terrain.getvalue())
        elif len(command) > 1 and command[1].endswith('build_addon.py'):
            Path(command[-2]).write_bytes(b'GSLC package')
        else:
            self.fail('Unexpected subprocess: ' + repr(command))
        return subprocess.CompletedProcess(command, 0)

    def invoke(self, *args, build_number=''):
        self.build_number = build_number
        env = {'NEWEARTH_JAVA_HOME': str(self.work / 'jdk25')}
        if build_number: env['BUILD_NUMBER'] = build_number
        stdout, stderr = io.StringIO(), io.StringIO()
        with mock.patch.object(BUILD, 'ROOT', self.work), \
                mock.patch.object(BUILD, 'check_jdk'), \
                mock.patch.object(BUILD.subprocess, 'run', side_effect=self.fake_subprocess), \
                mock.patch.dict(os.environ, env, clear=True), \
                mock.patch.object(sys, 'argv', ['build.py', *args]), \
                contextlib.redirect_stdout(stdout), contextlib.redirect_stderr(stderr):
            result = BUILD.main()
        return result, stdout.getvalue(), stderr.getvalue()

    def test_default_preserves_gslc_packaging_and_offline_flag(self):
        (self.work / 'project/GSLC').mkdir(parents=True, exist_ok=True)
        (self.work / 'project/GSLC/version.txt').write_text('9.8.7\n')
        code, _, error = self.invoke('--offline')
        self.assertEqual(code, 0, error)
        self.assertEqual((self.work / 'build/libs/GSLaunchCompat-9.8.7.jar').read_bytes(), b'GSLC package')
        self.assertEqual(len(self.commands), 2)
        self.assertIn('dependencies/GalaxySpace', self.commands[0])
        self.assertIn('--offline', self.commands[0])
        self.assertIn('--rerun-tasks', self.commands[0])
        self.assertEqual(self.commands[0][-1], 'jar')
        self.assertEqual(self.commands[1][-1], '9.8.7')

    def test_ftb_build_does_not_require_gslc_version_and_copies_exact_mod(self):
        code, _, error = self.invoke('--target', 'ftbquests', '--offline')
        self.assertEqual(code, 0, error)
        self.assertEqual(len(self.commands), 1)
        self.assertIn('project/FTB', self.commands[0])
        self.assertIn('--offline', self.commands[0])
        self.assertEqual(self.commands[0][-1], 'build')
        self.assertEqual([p.name for p in (self.work / 'build/libs').iterdir()], ['FTBQuests-1202.9.0.jar'])
        self.assertEqual((self.work / 'build/libs/FTBQuests-1202.9.0.jar').read_bytes(), b'current mod')

    def test_ftb_build_number_selects_matching_artifact(self):
        code, _, error = self.invoke('--target', 'ftbquests', build_number='17')
        self.assertEqual(code, 0, error)
        self.assertEqual((self.work / 'build/libs/FTBQuests-1202.9.0.17.jar').read_bytes(), b'current mod')

    def test_missing_ftb_artifact_does_not_export_stale_or_source_jars(self):
        self.emit_ftb = False
        code, _, error = self.invoke('--target', 'ftbquests')
        self.assertEqual(code, 1)
        self.assertIn('Expected FTB Quests artifact', error)
        self.assertFalse((self.work / 'build/libs').exists())

    def test_newearth_reuses_gs_wrapper_and_exports_exact_artifact(self):
        code, _, error = self.invoke('--target', 'newearth', '--offline')
        self.assertEqual(code, 0, error)
        self.assertEqual(len(self.commands), 2)
        self.assertEqual(self.commands[0][-1], "deobfGSJar")
        command = self.commands[1]
        self.assertIn('project/NewEarth', command)
        self.assertTrue(any('GalaxySpace' in entry and 'gradlew' in entry for entry in command))
        self.assertIn('--offline', command)
        self.assertEqual((self.work / 'build/libs/NewEarth-0.1.0.jar').read_bytes(),
                         (self.newearth / 'build/libs/NewEarth-0.1.0.jar').read_bytes())

    def test_newearth_rejects_mismatched_metadata_before_export(self):
        self.emit_newearth = False
        with zipfile.ZipFile(self.newearth / 'build/libs/NewEarth-0.1.0.jar', 'w') as jar:
            jar.writestr('mcmod.info', '[{"modid":"newearth","version":"0.0.9"}]')
            jar.writestr('newearth/NewEarth.class', b'old entry point')
        code, _, error = self.invoke('--target', 'newearth')
        self.assertEqual(code, 1)
        self.assertIn('metadata does not match', error)
        self.assertFalse((self.work / 'build/libs').exists())

    def test_missing_newearth_artifact_does_not_export_stale_jar(self):
        self.emit_newearth = False
        (self.newearth / 'build/libs/NewEarth-0.0.9.jar').write_bytes(b'stale mod')
        code, _, error = self.invoke('--target', 'newearth')
        self.assertEqual(code, 1)
        self.assertIn('Expected NewEarth artifact', error)
        self.assertFalse((self.work / 'build/libs').exists())

    def test_invalid_newearth_version_stops_before_gradle(self):
        (self.newearth / 'gradle.properties').write_text('mod_version=../other\n')
        code, _, error = self.invoke('--target', 'newearth')
        self.assertEqual(code, 1)
        self.assertIn('Invalid NewEarth', error)
        self.assertEqual(self.commands, [])

    def test_all_exports_three_targets_in_order(self):
        (self.work / 'project/GSLC').mkdir(parents=True, exist_ok=True)
        (self.work / 'project/GSLC/version.txt').write_text('9.8.7\n')
        code, _, error = self.invoke('--target', 'all', '--offline')
        self.assertEqual(code, 0, error)
        self.assertEqual([p.name for p in sorted((self.work / 'build/libs').iterdir())],
                         ['FTBQuests-1202.9.0.jar', 'GSLaunchCompat-9.8.7.jar', 'NewEarth-0.1.0.jar', 'NewEarth-BiomeBundle-0.1.0.zip'])
        self.assertIn('dependencies/GalaxySpace', self.commands[0])
        self.assertTrue(self.commands[1][1].endswith('build_addon.py'))
        self.assertIn('project/FTB', self.commands[2])
        self.assertIn('--offline', self.commands[2])
        self.assertEqual(self.commands[3][-1], 'deobfGSJar')
        self.assertIn('project/NewEarth', self.commands[4])
        self.assertIn('--offline', self.commands[4])

    def test_all_stops_after_first_gradle_failure(self):
        (self.work / 'project/GSLC').mkdir(parents=True, exist_ok=True)
        (self.work / 'project/GSLC/version.txt').write_text('9.8.7\n')
        with mock.patch.object(self, 'fake_subprocess', side_effect=subprocess.CalledProcessError(7, ['gradle'])) as runner:
            code, _, error = self.invoke('--target', 'all')
        self.assertEqual(code, 1)
        self.assertIn('Build failed', error)
        self.assertEqual(runner.call_count, 1)
        self.assertFalse((self.work / 'build/libs').exists())

    def test_ftb_rejects_path_in_build_number_before_invoking_gradle(self):
        code, _, error = self.invoke('--target', 'ftbquests', build_number='../other')
        self.assertEqual(code, 1)
        self.assertIn('Invalid FTB Quests', error)
        self.assertEqual(self.commands, [])


if __name__ == '__main__':
    unittest.main()
