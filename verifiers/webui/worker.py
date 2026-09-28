"""Detached job supervisor. Browser and HTTP server lifetimes do not own a run."""
import fcntl
import json
import os
from pathlib import Path
import subprocess
import sys
import time


def save(path, value):
    temporary = path.with_suffix('.tmp')
    with temporary.open('w') as stream:
        json.dump(value, stream, indent=2)
        stream.flush()
        os.fsync(stream.fileno())
    temporary.replace(path)


def main():
    directory = Path(sys.argv[1]).resolve()
    meta = directory / 'job.json'
    job = json.loads(meta.read_text())
    # Lock is inherited by the runner: even supervisor death cannot admit a
    # second run while the original runner is still executing.
    with (directory / 'worker.lock').open('a') as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            return
        job.update(state='RUNNING', pid=os.getpid(), startedAt=time.time(), error=None)
        save(meta, job)
        try:
            with (directory / 'runner.log').open('ab', buffering=0) as log:
                result = subprocess.run(job['command'], cwd=job['repo'], stdout=log,
                                        stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL,
                                        pass_fds=(lock.fileno(),))
            status_path = directory / 'output/status.json'
            stage = json.loads(status_path.read_text()).get('stage') if status_path.exists() else None
            job['state'] = ('PAUSED' if stage == 'PAUSED' else 'COMPLETE') if result.returncode == 0 else 'FAILED'
            if stage == 'REVIEW_REQUIRED':
                job['state'] = 'REVIEW_REQUIRED'
            job['exitCode'] = result.returncode
        except Exception as error:
            job.update(state='FAILED', error=str(error))
        job['finishedAt'] = time.time()
        save(meta, job)


if __name__ == '__main__':
    main()
