import logging
import re
import time


class TestUtils:
    @staticmethod
    def get_delays_from_func(report, functionality_name):
        """Extracts delays (before/after) for each command in a functionality from a report"""
        # Format: [NetworkManager]: Impairing <funcName>\n  >> on command <command> (<src>-><trg>): Fault [X] | Before [Yms] | After [Zms]
        pattern = rf"(?:\[NetworkManager\]:\s+)?Impairing\s+{functionality_name}\n\s*>>\s*on\s+command\s+(\w+)\s+\([^)]+\):\s+(?:Fault\s+\[\d+\]\s+\|\s+)?Before\s+\[(\d+)ms\]\s+\|\s+After\s+\[(\d+)ms\]"
        matches = re.findall(pattern, report)

        delays_map = {}
        for command, before, after in matches:
            if command not in delays_map:
                delays_map[command] = []
            delays_map[command].extend([int(before), int(after)])
        return delays_map

    @staticmethod
    def assert_values_range(values, interval):
        """Asserts if all delays in a list fall within the interval [min, max]"""
        min_val, max_val = interval
        if not values:
            TestUtils.fail("No delays found to assert")
            return False
        for d in values:
            if not (min_val <= d <= max_val):
                TestUtils.fail(
                    f"Delay violation! Found {d}ms, expected {min_val}-{max_val}ms")
                return False
        TestUtils.succeed()
        return True

    @staticmethod
    def assert_values_statistical_range(values, expected_mean):
        """Asserts delays against a statistical mean (Log-Normal)"""
        if not values:
            TestUtils.fail("No delays found to assert")
            return False
        lower_bound, upper_bound = expected_mean * 0.5, expected_mean * 2.0
        for d in values:
            if not (lower_bound <= d <= upper_bound):
                TestUtils.fail(
                    f"Statistical violation! Found {d}ms, expected ~{int(expected_mean)}ms")
                return False
        TestUtils.succeed()
        return True

    @staticmethod
    def get_max_concurrency_from_microservice(report, ms_name):
        """Extracts the maximum number of active requests for a microservice from a report"""
        # Format: [msName][operationName] ACTION: requestId | Active: [id1, id2] | Waiting: [] | Available: X
        pattern = rf"\[{ms_name.lower()}\]\[.*?\] .* \| Active: \[(.*?)\]"
        matches = re.findall(pattern, report)
        max_active = 0
        for m in matches:
            if not m.strip():
                count = 0
            else:
                count = len(m.split(","))
            if count > max_active:
                max_active = count
        return max_active

    @staticmethod
    def assert_max_capacity(ms_name, report, expected_max):
        """Asserts the number of active requests of a service does not surpass the limit"""
        max_active = TestUtils.get_max_concurrency_from_microservice(
            report, ms_name)
        TestUtils.info(f"Max simultaneous requests: {max_active} (<= {expected_max})")
        if not (max_active <= expected_max):
            TestUtils.fail(
                f"{ms_name} capacity violation! Found {max_active} concurrent requests, expected between 1 and {expected_max}")
            return False
        TestUtils.succeed()
        return True

    @staticmethod
    def assert_concurrency_range(ms_name, report, min_expected, max_expected):
        """Asserts the number of active requests of a sevice fall within expected bounds"""
        max_active = TestUtils.get_max_concurrency_from_microservice(
            report, ms_name)
        if not (min_expected <= max_active <= max_expected):
            TestUtils.fail(
                f"{ms_name} concurrency violation! Found {max_active}, expected between {min_expected} and {max_expected}")
            return False
        TestUtils.succeed()
        return True

    @staticmethod
    def assert_all_captured_steps_have_delays(report, ignored_commands=None):
        """Checks for commands with all zero delays: Fault [0] | Before [0ms] | After [0ms]"""
        if not report or "on command" not in report:
            TestUtils.fail("Report is empty or invalid.")
            return False

        if ignored_commands is None:
            ignored_commands = set()

        pattern = r"Impairing\s+\w+\n\s*>>\s*on\s+command\s+(\w+)\s+\([^)]+\):\s+Fault\s+\[0\]\s+\|\s+Before\s+\[0ms\]\s+\|\s+After\s+\[0ms\]"
        zero_delays = re.findall(pattern, report)
        zero_delays = [
            cmd for cmd in zero_delays if cmd not in ignored_commands]

        if zero_delays:
            TestUtils.fail(f"Commands with zero delay: {set(zero_delays)}")
            return False
        TestUtils.succeed()
        return True

    @staticmethod
    def info(msg):
        logging.info('\033[94m' + msg + '\033[0m')

    @staticmethod
    def succeed():
        logging.info('\033[92m' + "Test Succeeded!" + '\033[0m')

    @staticmethod
    def fail(msg):
        logging.error('\033[91m' + "Test failed: " + msg + '\033[0m')
