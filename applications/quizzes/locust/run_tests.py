import subprocess
import os
import sys
import re
"""
    List of tests to run, add any tests you see fit.
    Format: (testname, users, spawn_rate (users/sec), iterations)
"""
TESTS = [
    # Configuration and placement tests
    ("valid_node_placement_test.py", 1, 1, 1),
    ("invalid_node_placement_test.py", 1, 1, 1),
    ("invalid_negative_requirement_test.py", 1, 1, 1),

    # Impairment tests
    ("impairment_uniform_delays_test.py", 5, 1, 10),
    ("impairment_lognormal_delays_test.py", 5, 1, 10),
    ("impairment_full_sweep_delays_test.py", 1, 1, 1),

    # Capacity tests
    ("capacity_single_command_limit_test.py", 10, 5, 30),
    ("capacity_contention_test.py", 10, 5, 30),
    ("capacity_multitask_contention_test.py", 10, 5, 30),
]


def run_test(filename, users, spawn_rate, iterations):
    print(f"\n{'='*80}")
    print(f" RUNNING: {filename}")
    print(
        f" PARAMETERS: Users={users}, Spawn Rate={spawn_rate}, Iterations={iterations}")
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
    """
    cmd = [
        "locust",
        "-f", filename,
        "--headless",
        "--only-summary",
        "-u", str(users),
        "-r", str(spawn_rate),
        "--iterations", str(iterations)
    ]

    test_failed = False
    failure_messages = []

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

        process.wait()

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


def main():
    # Change directory to the location of this script to ensure relative paths work
    script_dir = os.path.dirname(os.path.abspath(__file__))
    if script_dir:
        os.chdir(script_dir)

    # Move into the tests directory
    if os.path.exists("tests"):
        os.chdir("tests")

    print(f"Starting Locust Test Suite in {os.getcwd()}...")

    failed_tests = []
    for test_config in TESTS:
        success, error_msg = run_test(*test_config)
        if not success:
            failed_tests.append((test_config[0], error_msg))

    if failed_tests:
        print('\033[91m' + "\n" + "="*80)
        print(
            f" TESTS COMPLETED WITH FAILURES: {len(failed_tests)} out of {len(TESTS)} failed.")
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
