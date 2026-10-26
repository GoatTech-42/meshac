from pathlib import Path
import tempfile,shutil,subprocess,os,json
r=Path(__file__).resolve().parents[1];env=os.environ.copy();results=[]
for name,old,new in [
 ('unmoved-claim-footprint','AABB b = pl.getBoundingBox().move(dx, dy, dz);','AABB b = pl.getBoundingBox();'),
 ('too-shallow-support-strip','b.minY - 0.1, b.minZ, b.maxX, b.minY, b.maxZ','b.minY - 0.05, b.minZ, b.maxX, b.minY, b.maxZ'),
 ('wrong-query-entity','claimAir = pl.level().noCollision(pl,','claimAir = pl.level().noCollision(null,')]:
 with tempfile.TemporaryDirectory(prefix='meshac-geom-mutant-') as tmp:
  p=Path(tmp)/'tree';shutil.copytree(r,p);f=p/'src/main/java/dev/meshac/Movement.java';s=f.read_text();assert old in s;s=s.replace(old,new);f.write_text(s)
  q=subprocess.run(['python3',str(p/'sim/test-airchain-port.py')],env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=90,text=True)
  assert q.returncode!=0,name
  err=next((l for l in q.stdout.splitlines() if 'Exception in thread' in l),'missing authored assertion');assert 'AssertionError' in err,(name,q.stdout)
  results.append({'mutation':name,'exit':q.returncode,'caught_by':err})
print(json.dumps(results,indent=2));(r/'geometry-mutations.json').write_text(json.dumps(results,indent=2)+'\n')
