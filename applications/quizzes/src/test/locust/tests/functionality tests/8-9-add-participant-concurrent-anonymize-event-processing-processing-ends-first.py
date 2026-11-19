"""
Migrated from jmeter/tournament/thesis-cases/8-9-add-participant-concurrent-anonymize-event-processing-processing-ends-first.jmx

With event polling stopped, the creator (student 1) is anonymized in the course execution and the anonymize event
is processed on demand (the tournament becomes INACTIVE). After a version decrement, student 2's join is concurrent
with it (TCC) and must be rejected (any code but 200). After 10 s the tournament must be INACTIVE, with an anonymous
creator and no participants.

The steps run one after the other, as the .jmx Thread Groups do (serialize_threadgroups=true).
"""
import time

from locust import HttpUser, task, between, events
from utils.app_utils import *
from utils.test_utils import *


class AddParticipantConcurrentAnonymizeEventProcessingProcessingEndsFirst(HttpUser):
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
        execution, tournament = data["execution_id"], data["tournament_id"]
        creator, student_2 = data["user_ids"]
        c = self.client

        TestUtils.assert_success(AppUtils.stop_event_detection(client=c), "Stop event detection")
        TestUtils.assert_success(AppUtils.anonymize_student(execution, creator, client=c), "Anonymize the creator")
        TestUtils.assert_success(AppUtils.process_anonymize_event(client=c), "Process the anonymize event")
        TestUtils.assert_success(AppUtils.decrement_version(client=c), "Decrement the version")
        TestUtils.assert_rejected(AppUtils.add_participant(tournament, student_2, execution, client=c),
                                  "Student 2's concurrent join is rejected")
        TestUtils.assert_success(AppUtils.start_event_detection(client=c), "Start event detection")

        time.sleep(10)
        r = AppUtils.get_tournament(tournament, client=c)
        TestUtils.assert_success(r, "Get the tournament")
        TestUtils.assert_field(r, "state", "INACTIVE")
        TestUtils.assert_field(r, "creator.name", "ANONYMOUS")
        TestUtils.assert_field(r, "creator.username", "ANONYMOUS")
        TestUtils.assert_empty(r, "participants")
