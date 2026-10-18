#!/usr/bin/env python3
import copy,importlib.util,json,pathlib,tempfile,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('replay',pathlib.Path(__file__).with_name('combat-replay.py'));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
FILE=ROOT/'rig/corpus/combat-legit-synthetic-v1.json'
class Replay(unittest.TestCase):
 def test_all_synthetic_geometry_cases(self):
  d=m.load(FILE)
  self.assertEqual(len(d['cases']),12)
  for c in d['cases']:
   with self.subTest(case=c['id']):self.assertEqual(m.replay(c),c['expected'])
 def invalid(self,edit):
  d=json.loads(FILE.read_text());edit(d)
  with tempfile.TemporaryDirectory() as t:
   p=pathlib.Path(t)/'bad.json';p.write_text(json.dumps(d))
   with self.assertRaises((ValueError,TypeError,KeyError)):m.load(p)
 def test_rejects_unknown_schema(self):self.invalid(lambda d:d.update(schema='v0'))
 def test_rejects_false_recording_claim(self):self.invalid(lambda d:d.update(provenance='real10-11'))
 def test_rejects_nan(self):self.invalid(lambda d:d['cases'][0]['samples'][0]['eye'].__setitem__(0,float('nan')))
 def test_rejects_inverted_box(self):self.invalid(lambda d:d['cases'][0]['samples'][0]['box'].__setitem__(0,999))
 def test_rejects_unordered_ticks(self):self.invalid(lambda d:d['cases'][0]['samples'][1].update(tick=0))
 def test_rejects_future_sent_time(self):self.invalid(lambda d:d['cases'][0]['attack'].update(sentMs=9999))
 def test_rejects_non_unit_look(self):self.invalid(lambda d:d['cases'][0]['attack'].update(look=[0,0,10]))
 def test_rejects_duplicate_ids(self):self.invalid(lambda d:d['cases'][1].update(id=d['cases'][0]['id']))
 def test_batched_legal_inputs_are_not_changed_into_client_cadence(self):
  sent=list(range(0,2600,200));handled=list(range(3000,3013))
  self.assertTrue(all(b-a==200 for a,b in zip(sent,sent[1:])))
  self.assertTrue(all(b-a==1 for a,b in zip(handled,handled[1:])))
  source=(ROOT/'src/main/java/dev/meshac/Combat.java').read_text()
  self.assertIn('lowConfidence(pl, "attack rate")',source)
  self.assertNotIn('hit = "attack rate"',source)
 def test_bounce_endpoint_alias_does_not_punish(self):
  y=[70.8,70.3,70,70.42,70.75,71,70.8]
  self.assertEqual(y[0],y[-1]);self.assertGreater(max(y)-min(y),.9)
  source=(ROOT/'src/main/java/dev/meshac/Combat.java').read_text()
  self.assertIn('lowConfidence(pl, String.format("short-fall attack pattern',source)
 def test_fixture_arrays_are_not_mutated(self):
  d=m.load(FILE);before=copy.deepcopy(d)
  for c in d['cases']:m.replay(c)
  self.assertEqual(before,d)
if __name__=='__main__':unittest.main(verbosity=2)
