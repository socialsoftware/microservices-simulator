"""
Migrated from jmeter/tournament/thesis-cases/8-10-concurrent-delete-tournament-add-participant.jmx
Profiles: sagas, tcc

The tournament is removed and student 2 then tries to join as if concurrent with the removal
(version decremented). The join is rejected and the tournament can no longer be read.
"""
from locust import HttpUser, task, between, events
from utils.app_utils import *
from utils.test_utils import *


class ConcurrentRemoveTournamentAddParticipantTester(HttpUser):
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
                TestUtils.assert_equals(AppUtils.remove_tournament(
                    tournament_id, client=self.client), True, "Delete tournament"),
                TestUtils.assert_equals(AppUtils.decrement_version(
                    client=self.client), True, "Version decrement"),
                TestUtils.assert_equals(AppUtils.join_tournament(
                    tournament_id, execution_id, data["student"]["id"], client=self.client), False, "Add student 2 as participant is rejected"),
                TestUtils.assert_equals(AppUtils.find_tournament(
                    tournament_id, client=self.client), None, "Request deleted tournament is rejected"),
            ]

            self.environment.scenario_passed = all(checks)
        except Exception as e:
            TestUtils.fail(f"Scenario Error: {e}")
        finally:
            TestUtils.end_scenario(self.environment)
