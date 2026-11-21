"""
Migrated from jmeter/tournament/thesis-cases/DeleteTournament.jmx

With only the creator (student 1) enrolled, the tournament is removed at about 1.5 s. At about 4 s the tournament
must be gone: reading it is rejected (the simulator deletes it, so it answers 400 "does not exist"), and the course
execution's tournament list no longer contains it (that list leaves out DELETED and INACTIVE tournaments).

The groups of steps run at the same time, each with its own connection, as the .jmx parallel Thread Groups do;
the waits reproduce each group's ConstantTimer, which JMeter applies before every request of the group.
"""
import time

from locust import HttpUser, task, between, events
from utils.app_utils import *
from utils.test_utils import *


class DeleteTournament(HttpUser):
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        environment.data = None
        try:
            TestUtils.assert_success(AppUtils.stop_event_detection(), "Stop event detection")
            TestUtils.assert_success(AppUtils.start_event_detection(), "Start event detection")
            TestUtils.assert_success(AppUtils.start_traces(), "Start root span")
            environment.data = AppUtils.create_scenario_data(users=1)
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
        tournament = data["tournament_id"]

        def remove_tournament(client):
            time.sleep(1.5)
            TestUtils.assert_success(AppUtils.remove_tournament(tournament, client=client), "Remove the tournament")

        def verify(client):
            time.sleep(4)
            TestUtils.assert_rejected(AppUtils.get_tournament(tournament, client=client),
                                      "The removed tournament can no longer be read")
            r = AppUtils.get_execution_tournaments(data["execution_id"], client=client)
            TestUtils.assert_success(r, "List the course execution's tournaments")
            TestUtils.assert_empty(r, "aggregateId", "The removed tournament is not listed")

        def cleanup(client):
            time.sleep(4.5)
            TestUtils.assert_success(AppUtils.end_traces(client=client), "End root span")
            time.sleep(4.5)
            TestUtils.assert_success(AppUtils.flush_traces(client=client), "Flush root span")

        TestUtils.run_in_parallel(self, remove_tournament, verify, cleanup)
