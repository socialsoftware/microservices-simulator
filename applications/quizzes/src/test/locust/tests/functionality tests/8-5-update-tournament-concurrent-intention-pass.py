"""
Migrated from jmeter/tournament/thesis-cases/8-5-update-tournament-concurrent-intention-pass.jmx

The tournament (1 question, topic 1) is updated to topic 2; the version counter is then decremented, so a second
update to topic 3 runs concurrently with the first (TCC). Both intentions merge without conflict: the quiz must
end with only question 3 and the tournament with only topic 3, still pointing at the same quiz.

The steps run one after the other, as the .jmx Thread Groups do (serialize_threadgroups=true).
"""
from locust import HttpUser, task, between, events
from utils.app_utils import *
from utils.test_utils import *


class UpdateTournamentConcurrentIntentionPass(HttpUser):
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        environment.data = None
        try:
            environment.data = AppUtils.create_scenario_data(users=2, topics=3, questions=3, tournament_topics=(1,), num_questions=1)
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
        tournament, quiz, topic_ids = data["tournament_id"], data["quiz_id"], data["topic_ids"]
        c = self.client

        TestUtils.assert_success(AppUtils.update_tournament_topics(tournament, [topic_ids[1]], 1, client=c),
                                 "Change the tournament to topic 2")
        TestUtils.assert_success(AppUtils.decrement_version(client=c), "Decrement the version")
        TestUtils.assert_success(AppUtils.update_tournament_topics(tournament, [topic_ids[2]], 1, client=c),
                                 "Change the tournament to topic 3")

        r = AppUtils.get_quiz(quiz, client=c)
        TestUtils.assert_success(r, "Get the quiz")
        TestUtils.assert_at_most_one(r, "questionDtos")
        TestUtils.assert_field(r, "questionDtos.0.title", "question title 3")

        r = AppUtils.get_tournament(tournament, client=c)
        TestUtils.assert_success(r, "Get the tournament")
        TestUtils.assert_at_most_one(r, "topics")
        TestUtils.assert_matches(r, "topics.0.name", "(.*)TOPIC3(.*)")
        TestUtils.assert_field(r, "quiz.aggregateId", quiz)
