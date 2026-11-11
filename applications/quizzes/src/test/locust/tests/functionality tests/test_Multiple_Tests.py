"""
Migrated from jmeter/tournament/thesis-cases/test_Multiple_Tests.jmx

Right after setUp, student 1 (the tournament creator) joins their own tournament and has their name updated in
the course execution, both at once. With event polling running, the tournament must list student 1 with the updated
name at about 10 s. The CleanUp group's timer applies to both of its samplers: flush at about 10 s, end at about
20 s (the timer sits after "Flush Root Span" in the tree, but its scope is the whole group).

The groups of steps run at the same time, each with its own connection, as the .jmx parallel Thread Groups do;
the waits reproduce each group's ConstantTimer, which JMeter applies before every request of the group.
"""
import time

from locust import HttpUser, task, between, events
from utils.app_utils import *
from utils.test_utils import *


class MultipleTests(HttpUser):
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        environment.data = None
        try:
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
        execution, tournament, creator = data["execution_id"], data["tournament_id"], data["user_ids"][0]

        def add_participant(client):
            TestUtils.assert_success(AppUtils.add_participant(tournament, creator, execution, client=client),
                                     "The creator joins the tournament")

        def update_name(client):
            TestUtils.assert_success(
                AppUtils.update_student_name(execution, creator, "Updated_Student_Name_1", client=client),
                "Update the creator's name")

        def verify(client):
            time.sleep(10)
            r = AppUtils.get_tournament(tournament, client=client)
            TestUtils.assert_success(r, "Get the tournament")
            TestUtils.assert_field(r, "participants.0.name", "Updated_Student_Name_1")

        def cleanup(client):
            time.sleep(10)
            TestUtils.assert_success(AppUtils.flush_traces(client=client), "Flush root span")
            time.sleep(10)
            TestUtils.assert_success(AppUtils.end_traces(client=client), "End root span")

        TestUtils.run_in_parallel(self, add_participant, update_name, verify, cleanup)
