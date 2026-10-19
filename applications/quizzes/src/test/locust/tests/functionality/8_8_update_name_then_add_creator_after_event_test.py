"""
Migrated from jmeter/tournament/thesis-cases/8-8-update-execution-student-add-participant-process-event-add-participant.jmx
Profiles: sagas, tcc

With event processing stopped, the creator's name is updated. A first join of the creator is sent
without executionAggregateId, exactly as in the JMeter test, so it is rejected with 400 by request
validation before reaching the functionality. After the scheduler processes the update name event,
the creator joins and both the creator and the participant have the new name.
"""
import time
from locust import HttpUser, task, between, events
from utils.app_utils import *
from utils.test_utils import *

UPDATED_NAME = "Updated_Student_Name_1"
EVENT_PROCESSING_WAIT = 20  # seconds, as the JMeter constant timer


class UpdateNameThenAddCreatorTester(HttpUser):
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
            ]

            # Missing executionAggregateId, as in the JMeter test
            r = AppUtils._post(f"/tournaments/{tournament_id}/join",
                               params={"userAggregateId": creator["id"]}, client=self.client)
            checks += [
                TestUtils.assert_equals(r.status_code if r is not None else None,
                                        400, "Add creator as participant without execution"),
                TestUtils.assert_equals(AppUtils.start_scheduler(
                    client=self.client), True, "Start event detection"),
            ]

            time.sleep(EVENT_PROCESSING_WAIT)
            checks.append(TestUtils.assert_equals(AppUtils.join_tournament(
                tournament_id, execution_id, creator["id"], client=self.client), True, "Add creator as participant"))

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
