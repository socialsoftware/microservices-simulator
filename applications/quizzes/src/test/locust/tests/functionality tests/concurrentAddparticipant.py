"""
Migrated from jmeter/tournament/thesis-cases/concurrentAddparticipant.jmx

Students 1, 2 and 3 join the tournament at the same time (about 1 s after setUp), with event polling running.
At about 5 s the tournament must have exactly 3 participants.

The groups of steps run at the same time, each with its own connection, as the .jmx parallel Thread Groups do;
the waits reproduce each group's ConstantTimer, which JMeter applies before every request of the group.
"""
import time

from locust import HttpUser, task, between, events
from utils.app_utils import *
from utils.test_utils import *


class ConcurrentAddParticipant(HttpUser):
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        environment.data = None
        try:
            TestUtils.assert_success(AppUtils.stop_event_detection(), "Stop event detection")
            TestUtils.assert_success(AppUtils.start_event_detection(), "Start event detection")
            TestUtils.assert_success(AppUtils.load_behaviour("jmeter/faults"), "Load behaviour jmeter/faults")
            TestUtils.assert_success(AppUtils.clean_behaviour_report(), "Clean behaviour report")
            TestUtils.assert_success(AppUtils.start_traces(), "Start root span")
            environment.data = AppUtils.create_scenario_data(users=3)
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
        execution, tournament = data["execution_id"], data["tournament_id"]

        def join(i, student):
            def add_participant(client):
                time.sleep(1)
                TestUtils.assert_success(AppUtils.add_participant(tournament, student, execution, client=client),
                                         f"Student {i} joins the tournament")
            return add_participant

        def verify(client):
            time.sleep(5)
            r = AppUtils.get_tournament(tournament, client=client)
            TestUtils.assert_success(r, "Get the tournament")
            TestUtils.assert_count(r, "participants", 3)

        def cleanup(client):
            time.sleep(7)
            TestUtils.assert_success(AppUtils.end_traces(client=client), "End root span")
            time.sleep(7)
            TestUtils.assert_success(AppUtils.flush_traces(client=client), "Flush root span")

        TestUtils.run_in_parallel(self, *[join(i, student) for i, student in enumerate(data["user_ids"], 1)], verify, cleanup)
