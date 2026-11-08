"""
Migrated from jmeter/tournament/thesis-cases/8-10-concurrent-delete-tournament-add-participant.jmx

The tournament is removed; after a version decrement, student 2's join is concurrent with the removal (TCC) and
must be rejected (any code but 200), and reading the tournament must fail too (any code but 200).

The steps run one after the other, as the .jmx Thread Groups do (serialize_threadgroups=true).
"""
from locust import HttpUser, task, between, events
from utils.app_utils import *
from utils.test_utils import *


class ConcurrentDeleteTournamentAddParticipant(HttpUser):
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

        TestUtils.assert_success(AppUtils.remove_tournament(tournament, client=c), "Remove the tournament")
        TestUtils.assert_success(AppUtils.decrement_version(client=c), "Decrement the version")
        TestUtils.assert_rejected(AppUtils.add_participant(tournament, student_2, execution, client=c),
                                  "Student 2's concurrent join is rejected")
        TestUtils.assert_rejected(AppUtils.get_tournament(tournament, client=c),
                                  "Getting the removed tournament fails")
