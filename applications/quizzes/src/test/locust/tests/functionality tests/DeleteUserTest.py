"""
Migrated from jmeter/tournament/thesis-cases/DeleteUserTest.jmx

Student 1 (the tournament creator, the only student) joins the tournament at about 1 s, and is deactivated and
deleted from about 1.5 s. From about 4 s the tournament must have no participants, the course execution no
students, and /users/students must answer successfully.

The groups of steps run at the same time, each with its own connection, as the .jmx parallel Thread Groups do;
the waits reproduce each group's ConstantTimer, which JMeter applies before every request of the group.
"""
import time

from locust import HttpUser, task, between, events
from utils.app_utils import *
from utils.test_utils import *


class DeleteUserTest(HttpUser):
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
        execution, tournament, creator = data["execution_id"], data["tournament_id"], data["user_ids"][0]

        def add_participant(client):
            time.sleep(1)
            TestUtils.assert_success(AppUtils.add_participant(tournament, creator, execution, client=client),
                                     "The creator joins the tournament")

        def delete_user(client):
            time.sleep(1.5)
            TestUtils.assert_success(AppUtils.deactivate_user(creator, client=client), "Deactivate the creator")
            time.sleep(1.5)
            TestUtils.assert_success(AppUtils.delete_user(creator, client=client), "Delete the creator")

        def verify(client):
            time.sleep(4)
            r = AppUtils.get_tournament(tournament, client=client)
            TestUtils.assert_success(r, "Get the tournament")
            TestUtils.assert_count(r, "participants", 0)
            time.sleep(4)
            r = AppUtils.get_course_execution(execution, client=client)
            TestUtils.assert_success(r, "Get the course execution")
            TestUtils.assert_count(r, "students", 0)
            time.sleep(4)
            TestUtils.assert_success(AppUtils.list_students(client=client), "List the students")

        def cleanup(client):
            time.sleep(20)
            TestUtils.assert_success(AppUtils.end_traces(client=client), "End root span")
            time.sleep(20)
            TestUtils.assert_success(AppUtils.flush_traces(client=client), "Flush root span")

        TestUtils.run_in_parallel(self, add_participant, delete_user, verify, cleanup)
