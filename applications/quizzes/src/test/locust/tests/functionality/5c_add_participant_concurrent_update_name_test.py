"""
Migrated from jmeter/tournament/thesis-cases/5c-updateStudentName1-addParticipant-updateStudentName2-processUpdateNameEvent.jmx
Profiles: sagas, tcc

Despite the JMeter file name, there is a single name update. Student 2 joins the tournament and the
version is decremented, so student 2's name update in the course execution runs as if concurrent
with the join. The participant name still converges once the update name event is processed.
"""
import time
from locust import HttpUser, task, between, events
from utils.app_utils import *
from utils.test_utils import *

UPDATED_NAME = "Updated_Student_Name_2"
EVENT_PROCESSING_WAIT = 20  # seconds, as the JMeter constant timer


class AddParticipantConcurrentUpdateNameTester(HttpUser):
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        try:
            # start() registers the event polling again, clear it first so events are polled once
            AppUtils.stop_scheduler()
            AppUtils.start()
            environment.test_data = AppUtils.create_tournament_base_data()
            if environment.test_data:
                TestUtils.info("Setup complete!")
            else:
                TestUtils.fail("Setup Failed: could not create the tournament base data")
        except Exception as e:
            TestUtils.fail(f"Setup Failed: {e}")
            environment.test_data = None

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        try:
            AppUtils.stop()
            TestUtils.report_scenario(environment)
        except Exception as e:
            TestUtils.fail(f"Validation Error: {e}")

    @task
    def scenario(self):
        data = self.environment.test_data
        try:
            if not data:
                return
            execution_id = data["execution_id"]
            tournament_id = data["tournament_id"]
            student_id = data["student"]["id"]

            TestUtils.info("====== SCENARIO ======")
            checks = [
                TestUtils.assert_equals(AppUtils.join_tournament(
                    tournament_id, execution_id, student_id, client=self.client), True, "Add student 2 as participant"),
                TestUtils.assert_equals(AppUtils.decrement_version(
                    client=self.client), True, "Version decrement"),
                TestUtils.assert_equals(AppUtils.update_student_name(
                    execution_id, student_id, UPDATED_NAME, client=self.client), True, "Update execution student 2 name"),
            ]

            time.sleep(EVENT_PROCESSING_WAIT)
            tournament = AppUtils.find_tournament(
                tournament_id, client=self.client)
            checks.append(TestUtils.assert_equals(TestUtils.get_participant_names(
                tournament), [UPDATED_NAME], "Participant names"))

            self.environment.scenario_passed = all(checks)
        except Exception as e:
            TestUtils.fail(f"Scenario Error: {e}")
        finally:
            TestUtils.end_scenario(self.environment)
