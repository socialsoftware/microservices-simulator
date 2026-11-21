"""
Migrated from jmeter/tournament/thesis-cases/8-6-add-participant-concurrent-update-execution-student-name-processing-ends-first.jmx

With event polling stopped, the creator's (student 1) name is updated and the update-name event is processed on
demand. The version counter is then decremented, so the creator's join is concurrent with the name update (TCC) and
must be rejected (any code but 200). After polling restarts, the tournament must show the creator with the new name
and no participants.

The steps run one after the other, as the .jmx Thread Groups do (serialize_threadgroups=true).
"""
from locust import HttpUser, task, between, events
from utils.app_utils import *
from utils.test_utils import *


class AddParticipantConcurrentUpdateExecutionStudentNameProcessingEndsFirst(HttpUser):
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
        execution, tournament, creator = data["execution_id"], data["tournament_id"], data["user_ids"][0]
        c = self.client

        TestUtils.assert_success(AppUtils.stop_event_detection(client=c), "Stop event detection")
        TestUtils.assert_success(AppUtils.update_student_name(execution, creator, "Updated_Student_Name_1", client=c),
                                 "Update the creator's name")
        TestUtils.assert_success(AppUtils.process_update_name_event(client=c), "Process the update-name event")
        TestUtils.assert_success(AppUtils.decrement_version(client=c), "Decrement the version")
        TestUtils.assert_rejected(AppUtils.add_participant(tournament, creator, execution, client=c),
                                  "The creator's concurrent join is rejected")
        TestUtils.assert_success(AppUtils.start_event_detection(client=c), "Start event detection")

        r = AppUtils.get_tournament(tournament, client=c)
        TestUtils.assert_success(r, "Get the tournament")
        TestUtils.assert_field(r, "creator.name", "Updated_Student_Name_1")
        TestUtils.assert_field(r, "creator.username", "Student_Username_1")
        TestUtils.assert_empty(r, "participants")
