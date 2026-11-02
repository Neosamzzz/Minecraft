"""Check output cleanup cannot consume inputs or arbitrary user directories."""
import importlib.util
from pathlib import Path
import json
import tempfile
import unittest
from unittest.mock import patch


SOURCE = Path(__file__).resolve().parents[2] / 'GalaxySpace/tests/telemetry_dimension/probe_runtime.py'


class ProbePathTests(unittest.TestCase):
    def setUp(self):
        spec = importlib.util.spec_from_file_location('probe_runtime', SOURCE)
        self.tool = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.tool)
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name).resolve()
        self.tool.ROOT = self.root
        (self.root / 'version.txt').write_text('1.4.8\n')
        self.runtime = self.root / '.local/probe-runtime'
        self.runtime.mkdir(parents=True)
        dependency = self.runtime / 'dependency.jar'
        dependency.write_bytes(b'dependency')
        (self.runtime / 'classpath.json').write_text(json.dumps([str(dependency)]))
        self.addon = self.root / 'build/libs/GSLaunchCompat-1.4.8.jar'
        self.addon.parent.mkdir(parents=True)
        self.addon.write_bytes(b'candidate')
        self.work = self.root / 'build/checks/gslc/telemetry'
        self.work.mkdir(parents=True)
        (self.work / 'stale.log').write_text('last result')
        mock_java = patch.object(self.tool, 'java_tool', side_effect=lambda name: name)
        mock_java.start()
        self.addCleanup(mock_java.stop)

    def test_defaults_are_independent_of_current_directory(self):
        self.assertEqual(self.tool.arguments('telemetry', []),
                         (self.runtime, self.addon, self.work, True))

    def test_default_run_replaces_previous_generated_output(self):
        self.tool.prepare(*self.tool.arguments('telemetry', []))
        self.assertFalse((self.work / 'stale.log').exists())
        self.assertTrue((self.work / 'mixin.mixinbooter.init.json').is_file())
        self.assertEqual(self.addon.read_bytes(), b'candidate')

    def test_missing_dependency_preserves_last_result(self):
        (self.runtime / 'dependency.jar').unlink()
        with self.assertRaises(FileNotFoundError):
            self.tool.prepare(*self.tool.arguments('telemetry', []))
        self.assertTrue((self.work / 'stale.log').exists())

    def test_custom_output_is_not_cleared(self):
        args = self.tool.arguments('telemetry', [str(self.runtime), str(self.addon), str(self.work)])
        self.tool.prepare(*args)
        self.assertTrue((self.work / 'stale.log').exists())

    def test_clean_refuses_output_containing_an_input(self):
        candidate = self.work / 'input.jar'
        candidate.write_bytes(b'keep')
        with self.assertRaises(ValueError):
            self.tool.prepare(self.runtime, candidate, self.work, clean=True)
        self.assertTrue(candidate.exists())

    def test_clean_refuses_symlinked_output(self):
        target = self.root / 'unrelated'
        target.mkdir()
        (target / 'keep').write_text('keep')
        link = self.work.parent / 'surface-map'
        link.symlink_to(target, target_is_directory=True)
        with self.assertRaises(ValueError):
            self.tool.prepare(self.runtime, self.addon, link, clean=True)
        self.assertTrue((target / 'keep').exists())
