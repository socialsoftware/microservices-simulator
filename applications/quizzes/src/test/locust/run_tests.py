import argparse
import subprocess
import os
import sys
import re

"""
    Performance tests (tests/performance/): network delays, capacity limits and their configuration.
    Add any tests you see fit.
    Format: (testname, users, spawn_rate (users/sec), iterations)
"""
PERFORMANCE_TESTS = [
    # Configuration and placement tests
    ("performance/valid_node_placement_test.py", 1, 1, 1),
    ("performance/invalid_node_placement_test.py", 1, 1, 1),
    ("performance/invalid_negative_requirement_test.py", 1, 1, 1),

    # Impairment tests
    ("performance/impairment_uniform_delays_test.py", 5, 1, 10),
    ("performance/impairment_lognormal_delays_test.py", 5, 1, 10),
    ("performance/impairment_full_sweep_delays_test.py", 1, 1, 1),

    # Capacity tests
    ("performance/capacity_single_command_limit_test.py", 10, 5, 30),
    ("performance/capacity_contention_test.py", 10, 5, 30),
    ("performance/capacity_multitask_contention_test.py", 10, 5, 30),
]

# Safety cap for a performance test, which normally ends at its iterations. A test whose users stop
# before running a task (e.g. a capacity test whose setup failed) never reaches them
PERFORMANCE_RUN_TIME = "300s"

"""
    Functionality tests (tests/functionality/): the numbered JMeter thesis cases. They are single user
    scenarios that end the run themselves, so they run with a time cap instead of iterations.
    Format: (testname, transactional profiles the scenario holds under)
"""
FUNCTIONALITY_TESTS = [
    ("functionality/5a_update_name_then_add_participant_test.py", ("sagas", "tcc")),
    ("functionality/5b_add_participant_then_update_name_test.py", ("sagas", "tcc")),
    ("functionality/5c_add_participant_concurrent_update_name_test.py", ("sagas", "tcc")),
    ("functionality/5d_update_name_processed_then_concurrent_add_participant_test.py", ("sagas", "tcc")),
    ("functionality/8_5_concurrent_update_tournament_test.py", ("sagas", "tcc")),
    ("functionality/8_6_add_creator_concurrent_update_name_processed_first_test.py", ("tcc",)),
    ("functionality/8_7_add_creator_concurrent_update_name_processed_last_test.py", ("tcc",)),
    ("functionality/8_8_update_name_then_add_creator_after_event_test.py", ("sagas", "tcc")),
    ("functionality/8_9_add_participant_concurrent_anonymize_processed_first_test.py", ("sagas", "tcc")),
    ("functionality/8_10_concurrent_remove_tournament_add_participant_test.py", ("sagas", "tcc")),
]

# Safety cap for a functionality scenario, the longest one waits 20s for event processing
FUNCTIONALITY_RUN_TIME = "180s"


def run_test(filename, users, spawn_rate, iterations=None, run_time=None):
    """
        A test only passes if it reports "Test Succeeded!" and no "Test failed:", so a run that
        errors before reaching its assertions is not taken as a pass
    """
    print(f"\n{'='*80}")
    print(f" RUNNING: {filename}")
    print(
        f" PARAMETERS: Users={users}, Spawn Rate={spawn_rate}, Iterations={iterations}, Run Time={run_time}")
    print(f"{'='*80}\n")

    # Check if file exists before running
    if not os.path.exists(filename):
        print(f"[!] Warning: {filename} not found, skipping.")
        return False, "File not found"

    """ 
    This is the running command, add any parameter you see fit.
        @ headless - runs on the terminal
        @ only-summary - prevents printing additional information (to no pollute terminal window)
        @ u (users) - max number of simultaneous users
        @ r (ramp-up) - number of users it will spawn per second until it reaches the max
        @ iterations - number of max tasks completed across all users
        @ run-time - stops the test after this time (e.g. 180s)
    """
    cmd = [
        sys.executable, "-m", "locust",
        "-f", filename,
        "--headless",
        "--only-summary",
        "-u", str(users),
        "-r", str(spawn_rate)
    ]
    if iterations is not None:
        cmd += ["--iterations", str(iterations)]
    if run_time is not None:
        cmd += ["--run-time", run_time]

    test_failed = False
    test_succeeded = False
    failure_messages = []
    last_line = ""

    try:
        # Use Popen to stream output in real-time and inspect it
        process = subprocess.Popen(
            cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1, shell=os.name == 'nt'
        )

        for line in process.stdout:
            """ 
                Removed this lines to not pollute the terminal.
                Uncomment them if you want to see the entire output of the tests running 
            """
            # sys.stdout.write(line)
            # sys.stdout.flush()

            # Check if any failure was emitted
            if "Test failed:" in line:
                test_failed = True
                msg = line.split("Test failed:", 1)[1].strip()
                # Strip ANSI escape sequences
                msg = re.sub(r'\x1b\[[0-9;]*m', '', msg)
                failure_messages.append(msg)
            elif "No module named locust" in line:
                test_failed = True
                failure_messages.append("ERROR: Locust is not installed in this Python environment.")
            elif "Test Succeeded!" in line:
                test_succeeded = True

            if line.strip():
                last_line = re.sub(r'\x1b\[[0-9;]*m', '', line.strip())

        process.wait()

        if not test_failed and not test_succeeded:
            test_failed = True
            failure_messages.append(
                f"No success reported, last output: {last_line}")

        if test_failed:
            print(f"\n[!] Test {filename} FAILED.")

            # Join multiple failures if they occurred, or provide a default if none parsed cleanly
            fail_reason = " | ".join(
                failure_messages) if failure_messages else "Unknown failure"
            return False, fail_reason
        else:
            print(f"\n[+] {filename} completed successfully.")
            return True, None

    except Exception as e:
        print(f"\n[!] An error occurred while running {filename}: {e}")
        return False, str(e)


def parse_args():
    parser = argparse.ArgumentParser(
        description="Runs a Locust test suite against the quizzes gateway (localhost:8080)")
    parser.add_argument("suite", nargs="?", choices=["performance", "functionality"], default="performance",
                        help="performance: network delay and capacity tests, functionality: JMeter thesis cases")
    parser.add_argument("--tx-mode", choices=["sagas", "tcc"], default="sagas",
                        help="profile the application was started with (TX_MODE), functionality tests not valid for it are skipped")
    return parser.parse_args()


def main():
    args = parse_args()

    # Change directory to the location of this script to ensure relative paths work
    script_dir = os.path.dirname(os.path.abspath(__file__))
    if script_dir:
        os.chdir(script_dir)

    # Move into the tests directory
    if os.path.exists("tests"):
        os.chdir("tests")

    print(f"Starting Locust Test Suite in {os.getcwd()}...")

    tests = [test + (PERFORMANCE_RUN_TIME,) for test in PERFORMANCE_TESTS]
    if args.suite == "functionality":
        tests = [(f, 1, 1, None, FUNCTIONALITY_RUN_TIME)
                 for f, profiles in FUNCTIONALITY_TESTS if args.tx_mode in profiles]
        for f, profiles in FUNCTIONALITY_TESTS:
            if args.tx_mode not in profiles:
                print(f"[-] Skipping {f}: only holds under {', '.join(profiles)}")

    failed_tests = []
    for test_config in tests:
        success, error_msg = run_test(*test_config)
        if not success:
            failed_tests.append((test_config[0], error_msg))

    if failed_tests:
        print('\033[91m' + "\n" + "="*80)
        print(
            f" TESTS COMPLETED WITH FAILURES: {len(failed_tests)} out of {len(tests)} failed.")
        for ft, msg in failed_tests:
            print(f"  - {ft}: {msg}")
        print("="*80 + '\033[0m')
        sys.exit(1)
    else:
        print('\033[92m' + "\n" + "="*80)
        print(" ALL TESTS COMPLETED SUCCESSFULLY")
        print("="*80 + '\033[0m')
        sys.exit(0)


if __name__ == "__main__":
    main()
