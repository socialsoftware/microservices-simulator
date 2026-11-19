"""
Migrated from jmeter/tournament/thesis-cases/5b-addParticipant-updateStudentName-processUpdateNameEvent.jmx

Student 2 joins the tournament (created by student 1) and only then has their name updated in the course
execution. Event polling keeps running, so after 10 s the update-name event has been processed and the tournament
must list student 2 with the updated name.

The steps run one after the other, as the .jmx Thread Groups do (serialize_threadgroups=true).
"""
import time

from locust import HttpUser, task, between, events
from utils.app_utils import *
from utils.test_utils import *


class AddParticipantUpdateStudentNameProcessUpdateNameEvent(HttpUser):
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        environment.data = None
        try:
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
        execution, tournament, student_2 = data["execution_id"], data["tournament_id"], data["user_ids"][1]
        c = self.client

        TestUtils.assert_success(AppUtils.add_participant(tournament, student_2, execution, client=c),
                                 "Student 2 joins the tournament")
        TestUtils.assert_success(AppUtils.update_student_name(execution, student_2, "Updated_Student_Name_2", client=c),
                                 "Update student 2's name")

        time.sleep(10)
        r = AppUtils.get_tournament(tournament, client=c)
        TestUtils.assert_success(r, "Get the tournament")
        TestUtils.assert_field(r, "participants.0.name", "Updated_Student_Name_2")
