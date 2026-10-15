"""
Migrated from jmeter/tournament/thesis-cases/8-5-update-tournament-concurrent-intention-pass.jmx
Profiles: sagas, tcc

A tournament over TOPIC1 with one question is updated to TOPIC2 and then, as if concurrently
(version decremented), to TOPIC3. Both updates only change the topics, so the last one prevails:
the tournament ends with TOPIC3 and its quiz with the TOPIC3 question.
"""
from locust import HttpUser, task, between, events
from utils.app_utils import *
from utils.test_utils import *


def update_tournament_topics(tournament_id, topic_id, client):
    """
    As in the JMeter test, sends only the topics and number of questions. AppUtils.update_tournament
    also sends new start and end times, which would add intentions to the concurrent updates.
    """
    r = AppUtils._post("/tournaments/update", json={"aggregateId": tournament_id, "numberOfQuestions": 1},
                       params={"topicsId": [topic_id]}, client=client)
    return r is not None and r.status_code == 200


class ConcurrentUpdateTournamentTester(HttpUser):
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        try:
            # start() registers the event polling again, clear it first so events are polled once
            AppUtils.stop_scheduler()
            AppUtils.start()
            environment.test_data = AppUtils.create_tournament_base_data(
                num_topics=3, tournament_topic_count=1, num_questions=1)
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
            tournament_id = data["tournament_id"]
            quiz_id = data["quiz_id"]
            topic_ids = data["topic_ids"]

            TestUtils.info("====== SCENARIO ======")
            checks = [
                TestUtils.assert_equals(update_tournament_topics(
                    tournament_id, topic_ids[1], client=self.client), True, "Update tournament (change to topic 2)"),
                TestUtils.assert_equals(AppUtils.decrement_version(
                    client=self.client), True, "Version decrement"),
                TestUtils.assert_equals(update_tournament_topics(
                    tournament_id, topic_ids[2], client=self.client), True, "Update tournament (change to topic 3)"),
            ]

            quiz = AppUtils.find_quiz(quiz_id, client=self.client) or {}
            tournament = AppUtils.find_tournament(
                tournament_id, client=self.client) or {}
            checks += [
                TestUtils.assert_equals([q.get("title") for q in quiz.get("questionDtos") or []], [
                                        "question title 3"], "Quiz question titles"),
                TestUtils.assert_equals([t.get("name") for t in tournament.get(
                    "topics") or []], ["TOPIC3"], "Tournament topic names"),
                TestUtils.assert_equals((tournament.get("quiz") or {}).get(
                    "aggregateId"), quiz_id, "Tournament quiz"),
            ]

            self.environment.scenario_passed = all(checks)
        except Exception as e:
            TestUtils.fail(f"Scenario Error: {e}")
        finally:
            TestUtils.end_scenario(self.environment)
