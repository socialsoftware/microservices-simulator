"""
Migrated from jmeter/tournament/thesis-cases/8-7-add-participant-concurrent-anonymize-event-processing-processing-ends-last.jmx
Profiles: tcc

Despite the JMeter file name, the event is an update name, not an anonymize. With event processing
stopped, the creator's name is updated and the creator joins as if concurrent with it (version
decremented). Processing the update name event, again as if concurrent (version decremented), is
rejected. Once the scheduler is restarted the event is processed and both the creator and the
participant end with the new name.

TCC only: the rejected event processing comes from the conflict the version decrement creates.
Sagas has no such conflict, so the processing succeeds and its check fails.
"""
import time
from locust import HttpUser, task, between, events
from utils.app_utils import *
from utils.test_utils import *

UPDATED_NAME = "Updated_Student_Name_1"
EVENT_PROCESSING_WAIT = 10  # seconds, as the JMeter constant timer


class AddCreatorUpdateNameProcessedLastTester(HttpUser):
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
            creator = data["creator"]

            TestUtils.info("====== SCENARIO ======")
            checks = [
                TestUtils.assert_equals(AppUtils.stop_scheduler(
                    client=self.client), True, "Stop event detection"),
                TestUtils.assert_equals(AppUtils.update_student_name(
                    execution_id, creator["id"], UPDATED_NAME, client=self.client), True, "Update execution creator name"),
                TestUtils.assert_equals(AppUtils.decrement_version(
                    client=self.client), True, "Version decrement"),
                TestUtils.assert_equals(AppUtils.join_tournament(
                    tournament_id, execution_id, creator["id"], client=self.client), True, "Add creator as participant"),
                TestUtils.assert_equals(AppUtils.decrement_version(
                    client=self.client), True, "Version decrement"),
                TestUtils.assert_equals(AppUtils.process_update_name_events(
                    client=self.client), False, "Process update creator name event is rejected"),
                TestUtils.assert_equals(AppUtils.start_scheduler(
                    client=self.client), True, "Start event detection"),
            ]

            time.sleep(EVENT_PROCESSING_WAIT)
            tournament = AppUtils.find_tournament(
                tournament_id, client=self.client)
            tournament_creator = (tournament or {}).get("creator") or {}
            checks += [
                TestUtils.assert_equals(tournament_creator.get(
                    "name"), UPDATED_NAME, "Creator name"),
                TestUtils.assert_equals(tournament_creator.get(
                    "username"), creator["username"], "Creator username"),
                TestUtils.assert_equals(TestUtils.get_participant_names(
                    tournament), [UPDATED_NAME], "Participant names"),
            ]

            self.environment.scenario_passed = all(checks)
        except Exception as e:
            TestUtils.fail(f"Scenario Error: {e}")
        finally:
            TestUtils.end_scenario(self.environment)
