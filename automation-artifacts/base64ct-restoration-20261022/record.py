import datetime
import pathlib
import shlex
import subprocess
import sys

root = pathlib.Path.cwd()
stage, *command = sys.argv[1:]
artifacts = pathlib.Path(__file__).resolve().parent
for row in subprocess.check_output(['/bin/ps', '-axo', 'pid=,comm='], text=True).splitlines():
    fields = row.strip().split(None, 1)
    if len(fields) == 2 and fields[1].endswith('/java'):
        cwd = subprocess.run(['/usr/sbin/lsof', '-a', '-p', fields[0], '-d', 'cwd', '-Fn'], capture_output=True, text=True).stdout
        if str(root) in cwd:
            raise SystemExit('Overlapping Base64ct Java process: ' + fields[0])
(artifacts / (stage + '-command.txt')).write_text(shlex.join(command) + '\n')
with (artifacts / (stage + '.log')).open('x') as log:
    started = datetime.datetime.now(datetime.timezone.utc).isoformat()
    process = subprocess.Popen(command, stdout=log, stderr=subprocess.STDOUT)
    print('Started', started, 'PID', process.pid, 'log', log.name, flush=True)
    code = process.wait()
(artifacts / (stage + '.exit')).write_text(str(code) + '\n')
print('Raw command exit:', code, flush=True)
raise SystemExit(code)
