"""
Migrated from jmeter/tournament/thesis-cases/Getquiz.jmx

After setUp, the tournament's quiz is read at about 3 s; the read must succeed (no assertion, so any 2xx/3xx).

The steps run one after the other, as the .jmx Thread Groups do (serialize_threadgroups=true).
"""
import time

from locust import HttpUser, task, between, events
from utils.app_utils import *
from utils.test_utils import *


class GetQuiz(HttpUser):
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        environment.data = None
        try:
            TestUtils.assert_success(AppUtils.stop_event_detection(), "Stop event detection")
            TestUtils.assert_success(AppUtils.start_event_detection(), "Start event detection")
            TestUtils.assert_success(AppUtils.start_traces(), "Start root span")
            environment.data = AppUtils.create_scenario_data(users=2)
            if not environment.data:
                raise RuntimeError("the scenario data could not be created")
            TestUtils.info("Setup complete!")
        except Exception as e:
            TestUtils.fail(f"Setup Failed: {e}")

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        TestUtils.report_scenario(environment)

    @task
    def scenario(self):
        data = self.environment.data
        if data:
            try:
                self.steps(data)
            except Exception as e:
                TestUtils.fail(f"Scenario error: {e!r}")
        TestUtils.end_scenario(self)

    def steps(self, data):
        c = self.client

        time.sleep(3)
        TestUtils.assert_success(AppUtils.get_quiz(data["quiz_id"], client=c), "Get the quiz")

        time.sleep(3.25)
        TestUtils.assert_success(AppUtils.end_traces(client=c), "End root span")
        time.sleep(3.25)
        TestUtils.assert_success(AppUtils.flush_traces(client=c), "Flush root span")
