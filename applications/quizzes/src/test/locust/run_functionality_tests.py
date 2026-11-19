import argparse
import csv
import os
import re
import subprocess
import sys
import time

"""
    Runner for the functionality tests migrated from jmeter/tournament/thesis-cases (tests/functionality tests/).

    Each scenario runs once and quits Locust itself; --run-time is only a safety cap. A test passes only if
    "Test Succeeded!" appears in its output and "Test failed:" does not.

    The scenarios create the course execution with the fixed acronym TESTBLCM, as the .jmx files do, so on a manually
    started application only the first test after each start can pass its setup (same as with JMeter).
"""
FUNCTIONALITY_TESTS = [
    "5a-updateStudentName-addParticipant-processUpdateNameEvent.py",
    "5b-addParticipant-updateStudentName-processUpdateNameEvent.py",
    "5c-updateStudentName1-addParticipant-updateStudentName2-processUpdateNameEvent.py",
    "5d-addParticipant1-updateStudentName-processUpdateNameEvent1-addParticipant2-processUpdateNameEvent2.py",
    "8-5-update-tournament-concurrent-intention-pass.py",
    "8-6-add-participant-concurrent-update-execution-student-name-processing-ends-first.py",
    "8-7-add-participant-concurrent-anonymize-event-processing-processing-ends-last.py",
    "8-8-update-execution-student-add-participant-process-event-add-participant.py",
    "8-9-add-participant-concurrent-anonymize-event-processing-processing-ends-first.py",
    "8-10-concurrent-delete-tournament-add-participant.py",
    "concurrentAddparticipant.py",
    "concurrentAddUpdateparticipant.py",
    "concurrent-addCreatorAsParticipant-updateCreatorName-invBreak.py",
    "test_Multiple_Tests.py",
    "DeleteTournament.py",
    "DeleteUserTest.py",
    "Getquiz.py",
]

TEST_DIR = "functionality tests"
ANSI = re.compile(r'\x1b\[[0-9;]*m')


def run_test(filename, run_time, verbose, log_dir):
    print(f"\n{'='*80}")
    print(f" RUNNING: {filename}")
    print(f"{'='*80}\n")

    path = os.path.join(TEST_DIR, filename)
    if not os.path.exists(path):
        print(f"[!] Warning: {path} not found, skipping.")
        return False, "File not found", 0.0

    cmd = [
        sys.executable, "-m", "locust",
        "-f", path,
        "--headless",
        "--only-summary",
        "-u", "1",
        "-r", "1",
        "--run-time", run_time,
    ]

    succeeded = False
    failure_messages = []
    log_file = None
    if log_dir:
        os.makedirs(log_dir, exist_ok=True)
        log_file = open(os.path.join(log_dir, os.path.splitext(filename)[0] + ".log"), "w")

    start = time.time()
    try:
        process = subprocess.Popen(
            cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1, shell=os.name == 'nt'
        )
        for line in process.stdout:
            if verbose:
                sys.stdout.write(line)
                sys.stdout.flush()
            if log_file:
                log_file.write(line)
            if "Test failed:" in line:
                failure_messages.append(ANSI.sub('', line.split("Test failed:", 1)[1].strip()))
            elif "Test Succeeded!" in line:
                succeeded = True
            elif "No module named locust" in line:
                failure_messages.append("ERROR: Locust is not installed in this Python environment.")
        process.wait()
    except Exception as e:
        failure_messages.append(str(e))
    finally:
        if log_file:
            log_file.close()
    duration = time.time() - start

    if succeeded and not failure_messages:
        print(f"\n[+] {filename} completed successfully ({duration:.1f}s).")
        return True, None, duration
    reason = " | ".join(failure_messages) if failure_messages else "No 'Test Succeeded!' in the output"
    print(f"\n[!] Test {filename} FAILED ({duration:.1f}s): {reason}")
    return False, reason, duration


def main():
    parser = argparse.ArgumentParser(description="Run the functionality tests migrated from the JMeter thesis cases")
    parser.add_argument("--tests", help="comma-separated filename prefixes to run (default: all)")
    parser.add_argument("--run-time", default="120s", help="safety cap per test (default: 120s)")
    parser.add_argument("--verbose", action="store_true", help="stream the Locust output")
    parser.add_argument("--results-file", help="write a CSV test,result,duration_s,failure")
    parser.add_argument("--log-dir", help="save each test's full Locust output as <test>.log in this directory")
    args = parser.parse_args()

    results_file = os.path.abspath(args.results_file) if args.results_file else None
    log_dir = os.path.abspath(args.log_dir) if args.log_dir else None

    # Run from tests/ so that `utils` resolves through the cwd
    os.chdir(os.path.join(os.path.dirname(os.path.abspath(__file__)), "tests"))
    print(f"Starting Locust functionality tests in {os.getcwd()}...")

    tests = FUNCTIONALITY_TESTS
    if args.tests:
        prefixes = [p.strip() for p in args.tests.split(",") if p.strip()]
        tests = [t for t in tests if any(t.startswith(p) for p in prefixes)]
        if not tests:
            print(f"No test matches {args.tests}")
            sys.exit(2)

    results = []
    for filename in tests:
        ok, reason, duration = run_test(filename, args.run_time, args.verbose, log_dir)
        results.append((filename, ok, duration, reason))

    if results_file:
        with open(results_file, "w", newline="") as f:
            writer = csv.writer(f)
            writer.writerow(["test", "result", "duration_s", "failure"])
            for filename, ok, duration, reason in results:
                writer.writerow([filename, "PASS" if ok else "FAIL", f"{duration:.1f}", reason or ""])

    failed = [r for r in results if not r[1]]
    if failed:
        print('\033[91m' + "\n" + "="*80)
        print(f" TESTS COMPLETED WITH FAILURES: {len(failed)} out of {len(results)} failed.")
        for filename, _, _, reason in failed:
            print(f"  - {filename}: {reason}")
        print("="*80 + '\033[0m')
        sys.exit(1)
    print('\033[92m' + "\n" + "="*80)
    print(" ALL TESTS COMPLETED SUCCESSFULLY")
    print("="*80 + '\033[0m')
    sys.exit(0)


if __name__ == "__main__":
    main()
