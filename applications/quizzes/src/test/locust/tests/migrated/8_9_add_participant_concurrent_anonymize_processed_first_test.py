"""
Migrated from jmeter/tournament/thesis-cases/8-9-add-participant-concurrent-anonymize-event-processing-processing-ends-first.jmx
Profiles: sagas, tcc

With event processing stopped, the creator is anonymized in the course execution and the anonymize
event is processed, which inactivates the tournament. Student 2 then tries to join as if concurrent
with that processing (version decremented) and is rejected. The tournament stays inactive, with an
anonymous creator and no participants.

The JMeter test also sent a body with the anonymize request, which the endpoint ignores, so it is
not sent here.
"""
import time
from locust import HttpUser, task, between, events
from utils.app_utils import *
from utils.test_utils import *

ANONYMOUS = "ANONYMOUS"
EVENT_PROCESSING_WAIT = 10  # seconds, as the JMeter constant timer


class AddParticipantAnonymizeProcessedFirstTester(HttpUser):
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

            TestUtils.info("====== SCENARIO ======")
            checks = [
                TestUtils.assert_equals(AppUtils.stop_scheduler(
                    client=self.client), True, "Stop event detection"),
                TestUtils.assert_equals(AppUtils.anonymize_student(
                    execution_id, data["creator"]["id"], client=self.client), True, "Anonymize execution creator"),
                TestUtils.assert_equals(AppUtils.process_anonymize_events(
                    client=self.client), True, "Process anonymize student event"),
                TestUtils.assert_equals(AppUtils.decrement_version(
                    client=self.client), True, "Version decrement"),
                TestUtils.assert_equals(AppUtils.join_tournament(
                    tournament_id, execution_id, data["student"]["id"], client=self.client), False, "Add student 2 as participant is rejected"),
                TestUtils.assert_equals(AppUtils.start_scheduler(
                    client=self.client), True, "Start event detection"),
            ]

            time.sleep(EVENT_PROCESSING_WAIT)
            tournament = AppUtils.find_tournament(
                tournament_id, client=self.client)
            tournament_creator = (tournament or {}).get("creator") or {}
            checks += [
                TestUtils.assert_equals((tournament or {}).get(
                    "state"), "INACTIVE", "Tournament state"),
                TestUtils.assert_equals(tournament_creator.get(
                    "name"), ANONYMOUS, "Creator name"),
                TestUtils.assert_equals(tournament_creator.get(
                    "username"), ANONYMOUS, "Creator username"),
                TestUtils.assert_equals(TestUtils.get_participant_names(
                    tournament), [], "Participant names"),
            ]

            self.environment.scenario_passed = all(checks)
        except Exception as e:
            TestUtils.fail(f"Scenario Error: {e}")
        finally:
            TestUtils.end_scenario(self.environment)
