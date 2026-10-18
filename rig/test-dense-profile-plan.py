#!/usr/bin/env python3
import importlib.util,pathlib,unittest
P=pathlib.Path(__file__).with_name('dense-profile-plan.py');spec=importlib.util.spec_from_file_location('profile_plan',P);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class Plan(unittest.TestCase):
 def test_no_execution_surface(self):
  s=P.read_text()
  for bad in ['import socket','subprocess','os.system','ssh ','docker ','Popen']:self.assertNotIn(bad,s)
 def test_plan(self):
  p=m.make_plan();self.assertEqual(p['mode'],'PLAN_ONLY');self.assertEqual(p['players'],40);self.assertEqual(p['limits']['totalNativeClientsOnBox'],2)
 def test_summary(self):
  r=m.summarize([{'stage':'synthetic','tick':i,'tickNs':10_000_000+i*1_000_000,'checkNs':{'combat':1_000_000},'gcPauseNs':0,'validPlayers':40,'attacks':2} for i in range(20)])['synthetic']
  self.assertEqual(r['tickMsP95'],28);self.assertEqual(r['checkMeanMs']['combat'],1);self.assertEqual(r['attacks'],40)
 def test_no_empty_pass(self):
  with self.assertRaises(ValueError):m.summarize([])
 def test_overlapping_timers_rejected(self):
  with self.assertRaises(ValueError):m.summarize([{'stage':'x','tick':1,'tickNs':1,'checkNs':{'combat':2},'gcPauseNs':0,'validPlayers':40,'attacks':0}])
if __name__=='__main__':unittest.main(verbosity=2)
