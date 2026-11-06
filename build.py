from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
import os,subprocess,shutil
r=Path(__file__).resolve().parent
cp=os.environ.get('ALTAR_COMPILE_CLASSPATH')
if not cp:raise SystemExit('Set ALTAR_COMPILE_CLASSPATH to the Paper 1.21.11 API and its transitive libraries')
j=Path(os.environ['JAVA_HOME'])/'bin/javac' if os.environ.get('JAVA_HOME') else Path('javac')
out=r/'build/classes';shutil.rmtree(out,ignore_errors=True);out.mkdir(parents=True)
subprocess.run([str(j),'--release','21','-proc:none','-encoding','UTF-8','-g:none','-cp',cp,'-d',str(out)]+[str(p) for p in sorted((r/'src/main/java').rglob('*.java'))],check=True)
with ZipFile(r/'RunesAltarV.jar','w',ZIP_DEFLATED) as z:
 for folder in [out,r/'src/main/resources']:
  for p in sorted(folder.rglob('*')):
   if p.is_file():z.writestr(p.relative_to(folder).as_posix(),p.read_bytes())
 for name in ['LICENSE','NOTICE']:z.writestr('META-INF/'+name,(r/name).read_bytes())
with ZipFile(r/'RunesAltarV.jar') as z:assert z.testzip() is None
print(r/'RunesAltarV.jar')
