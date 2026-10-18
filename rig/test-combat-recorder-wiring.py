#!/usr/bin/env python3
from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[1]/'src/main/java/dev/meshac'
C=(R/'Combat.java').read_text();M=(R/'MeshCommands.java').read_text();B=(R/'CombatRecorder.java').read_text()
class Wiring(unittest.TestCase):
 def test_optin_no_logging_verdict_disk(self):
  for bad in ['LOG.','Verdict.','Files.','new Thread(','Scheduled']:self.assertNotIn(bad,B)
 def test_append_independent_of_log_trace(self):
  block=C.split('RECORDER.add(',1)[1].split('CombatFeatures.Summary',1)[0]
  self.assertNotIn('Trace.ON',block);self.assertNotIn('logs.allow',block)
 def test_thread_guard_retained(self):self.assertIn("private void guard(){if(!serverThread.getAsBoolean())",B)
 def test_disconnect_clears_capture(self):self.assertIn('RECORDER.clear(id)',C)
 def test_command_admin_root_and_no_broadcast(self):
  self.assertIn('Commands.literal("mesh").requires(Perm.admin())',M)
  block=M.split('private static int capture(',1)[1].split('private static String veilLine',1)[0]
  self.assertNotIn(', true)',block)
  self.assertIn('IntegerArgumentType.integer(1, 32)',M)
 def test_command_tree_parentheses_balanced(self):
  # Cheap syntax guard only, not Brigadier API or Minecraft compilation.
  import re
  text=re.sub(r'"(?:\\.|[^"\\])*"','""',M)
  count=0
  for char in text:
   if char=='(':count+=1
   elif char==')':count-=1
   self.assertGreaterEqual(count,0)
  self.assertEqual(count,0)
if __name__=='__main__':unittest.main(verbosity=2)
