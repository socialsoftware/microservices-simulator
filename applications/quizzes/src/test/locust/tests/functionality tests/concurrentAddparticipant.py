"""
Migrated from jmeter/tournament/thesis-cases/concurrentAddparticipant.jmx

Students 1, 2 and 3 join the tournament at the same time (about 1 s after setUp), with event polling running.
The simulator may accept all three joins or reject some of them as conflicting (e.g. tcc: a serialization failure),
so the joins' answers are not checked. At about 5 s the tournament must be readable and its participants must be
exactly the students whose join was accepted: an accepted join that is missing (a lost update) fails, and so does
a tournament that can no longer be read (e.g. two joins committed the same version).

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

        accepted = {}

        def join(i, student):
            def add_participant(client):
                time.sleep(1)
                accepted[student] = TestUtils.outcome(
                    AppUtils.add_participant(tournament, student, execution, client=client),
                    f"Student {i} joins the tournament")
            return add_participant

        def verify(client):
            time.sleep(5)
            r = AppUtils.get_tournament(tournament, client=client)
            TestUtils.assert_success(r, "Get the tournament")
            if len(accepted) < len(data["user_ids"]):
                TestUtils.fail("Not every join had answered when the tournament was read")
            TestUtils.assert_ids(r, "participants", [s for s, ok in accepted.items() if ok],
                                 "The participants are exactly the students whose join was accepted")

        def cleanup(client):
            time.sleep(7)
            TestUtils.assert_success(AppUtils.end_traces(client=client), "End root span")
            time.sleep(7)
            TestUtils.assert_success(AppUtils.flush_traces(client=client), "Flush root span")

        TestUtils.run_in_parallel(self, *[join(i, student) for i, student in enumerate(data["user_ids"], 1)], verify, cleanup)
