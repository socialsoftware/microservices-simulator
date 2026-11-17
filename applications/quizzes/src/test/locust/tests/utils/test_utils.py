import logging
import re
import time

import gevent
from locust.clients import HttpSession
from locust.exception import StopUser

from . import jmeter_semantics as JS


class TestUtils:
    failures = 0  # number of fail() calls in this run
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
        TestUtils.info(
            f"Max simultaneous requests: {max_active} (<= {expected_max})")
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
        TestUtils.failures += 1
        logging.error('\033[91m' + "Test failed: " + msg + '\033[0m')

    """
    ===========================
    || Functionality tests (scenarios migrated from jmeter/tournament/thesis-cases)
    ===========================
    A scenario runs once: the data is created in test_start, the steps run in the single @task, which ends with
    end_scenario(), and test_stop calls report_scenario(). Every request is checked as in the .jmx, and a failed
    check does not stop the scenario. The test succeeds only if it reached its end and no check failed.

    Body checks take a dotted field path ("creator.name", "participants.0.name") that is searched anywhere in the
    body and evaluated with the same rules as the .jmx JSONPath Assertion on "$..creator.name" (jmeter_semantics.py):
    the check passes if any match equals the expected value.
    """

    @staticmethod
    def assert_success(response, step):
        """The request succeeded (2xx or 3xx)."""
        code = TestUtils._code(response)
        return TestUtils._check(JS.sample_ok(code), step, f"expected success, got {code or 'no response'}", response)

    @staticmethod
    def assert_rejected(response, step):
        """The request was not answered with 200 (any other code, or no response at all)."""
        code = TestUtils._code(response)
        return TestUtils._check("200" not in code, step, "expected a rejection, got 200", response)

    @staticmethod
    def assert_status(response, status, step):
        code = TestUtils._code(response)
        return TestUtils._check(code == str(status), step, f"expected {status}, got {code or 'no response'}",
                                response)

    @staticmethod
    def assert_field(response, field, expected, step=None):
        """`field` equals `expected`."""
        return TestUtils._check_body(response, TestUtils._json_path(field), str(expected), False,
                                     step or f"{field} = {expected}")

    @staticmethod
    def assert_matches(response, field, regex, step=None):
        """`field` matches the whole `regex`."""
        return TestUtils._check_body(response, TestUtils._json_path(field), regex, True,
                                     step or f"{field} matches {regex}")

    @staticmethod
    def assert_empty(response, field, step=None):
        """`field` is an empty list (or absent)."""
        return TestUtils._check_body(response, TestUtils._json_path(field), "[]", False, step or f"{field} is empty")

    @staticmethod
    def assert_count(response, field, count, step=None):
        """`field` has `count` elements (an absent field has 0)."""
        return TestUtils._check_body(response, TestUtils._json_path(field) + ".length()", str(count), False,
                                     step or f"{field} has {count} element(s)")

    @staticmethod
    def assert_at_most_one(response, field, step=None):
        """`field` has no second element."""
        return TestUtils._check_body(response, TestUtils._json_path(field) + "[1]", "[]", False,
                                     step or f"{field} has at most one element")

    @staticmethod
    def outcome(response, step):
        """For a request in a race, where either answer is valid: logs whether it was accepted (2xx/3xx) or
        rejected (any other code, or no response) and returns True if accepted. It is not a check."""
        code = TestUtils._code(response)
        accepted = JS.sample_ok(code)
        TestUtils.info(f"{step}: {'accepted' if accepted else 'rejected'} ({code or 'no response'})")
        return accepted

    @staticmethod
    def assert_ids(response, field, expected_ids, step=None):
        """The elements of the list `field` have exactly these aggregateIds, in any order."""
        expected = sorted(expected_ids)
        try:
            lists = JS.read(TestUtils._text(response), TestUtils._json_path(field))
            found = sorted(e.get("aggregateId") for lst in lists if isinstance(lst, list)
                           for e in lst if isinstance(e, dict))
            passed, detail = found == expected, f"expected {expected}, found {found}"
        except JS.JsonPathError as e:
            passed, detail = False, str(e)
        return TestUtils._check(passed, step or f"{field} are {expected}", detail, response)

    @staticmethod
    def run_in_parallel(user, *groups):
        """Runs the functions at the same time, like parallel JMeter Thread Groups. Each one gets its own HTTP
        client (its own connection) as its only argument."""
        clients = [HttpSession(base_url=user.host, request_event=user.environment.events.request, user=user)
                   for _ in groups]
        greenlets = [gevent.spawn(group, client) for group, client in zip(groups, clients)]
        gevent.joinall(greenlets)
        for g in greenlets:
            if g.exception is not None:
                raise g.exception

    @staticmethod
    def end_scenario(user):
        """Marks the scenario as complete and quits Locust from another greenlet, so this user stops cleanly."""
        user.environment.scenario_completed = True
        gevent.spawn(user.environment.runner.quit)
        raise StopUser()

    @staticmethod
    def report_scenario(environment):
        if not getattr(environment, "scenario_completed", False):
            TestUtils.fail("Scenario did not reach its end (run-time cap hit?)")
        elif TestUtils.failures == 0:
            TestUtils.succeed()

    @staticmethod
    def _code(response):
        """The status code as text; "" when there was no response (Locust reports those as status 0)."""
        if response is None or not response.status_code:
            return ""
        return str(response.status_code)

    @staticmethod
    def _text(response):
        try:
            return response.text or "" if response is not None else ""
        except Exception:
            return ""

    @staticmethod
    def _json_path(field):
        return "$.." + re.sub(r"\.(\d+)", r"[\1]", field)

    @staticmethod
    def _check_body(response, path, expected, regex, step):
        passed, message = JS.jsonpath_assertion(TestUtils._text(response), path, expected, regex)
        return TestUtils._check(passed, step, message, response)

    @staticmethod
    def _check(passed, step, detail, response=None):
        if passed:
            TestUtils.info(f"OK: {step}")
        else:
            body = TestUtils._text(response)[:300]
            TestUtils.fail(f"{step}: {detail}" + (f" | body: {body}" if body else ""))
        return passed
