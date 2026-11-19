from locust import HttpUser, task, between, events
import logging
import uuid
import datetime
from utils.app_utils import *
from utils.test_utils import *

config = {
    "Placement": {
        "nodes": [
            {"name": "Node1", "microservices": [
                "user", "execution", "course"]},
            {"name": "Node2", "microservices": ["tournament", "quiz"]},
            {"name": "Node3", "microservices": ["question", "topic", "answer"]}
        ]},
    "Delays": {
        "intraservice": {"uni": [1, 2]},
        "intranode":    {"uni": [5, 10]},
        "internode":    {"uni": [10, 15]}
    }
}


class FullSweepDelaysTester(HttpUser):
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        try:
            AppUtils.start()
            AppUtils.inject_configuration(config)
            TestUtils.info("Setup complete!")
        except Exception as e:
            TestUtils.fail(f"Setup Failed: {e}")

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        try:
            report = AppUtils.get_impairment_report()
            AppUtils.stop()

            TestUtils.info("====== RESULTS ======")
            # Ignore these commands because they belong to version service which is not part of placement
            ignored_commands = {
                "IncrementVersionCommand", "GetNextVersionCommand"}
            TestUtils.assert_all_captured_steps_have_delays(
                report, ignored_commands)
        except Exception as e:
            TestUtils.fail(f"Validation Error: {e}")

    @task
    def full_system_flow(self):
        now = datetime.datetime.now(datetime.timezone.utc)
        suffix = int(now.timestamp())

        # --- USER SERVICE ---
        user_data = AppUtils.create_user(client=self.client)
        if not user_data:
            return
        user_id = user_data["aggregateId"]
        AppUtils.activate_user(user_id, client=self.client)
        AppUtils.get_students(client=self.client)

        # --- EXECUTION + COURSE SERVICE ---
        exec_data = AppUtils.create_execution(client=self.client)
        if not exec_data:
            return
        exec_id, course_id = exec_data["aggregateId"], exec_data["courseAggregateId"]
        AppUtils.enroll_student(user_id, exec_id, client=self.client)

        # --- TOPIC SERVICE ---
        topic_data = AppUtils.create_topic(course_id, client=self.client)
        if not topic_data:
            return
        topic_id = topic_data["aggregateId"]

        # --- QUESTION SERVICE ---
        questions = AppUtils.create_questions(
            course_id, topic_data, count=2, client=self.client)
        if not questions:
            return
        q_data = questions[0]
        q_id = q_data["aggregateId"]

        # --- QUIZ SERVICE ---
        quiz_data = AppUtils.create_quiz(
            execution_id=exec_id,
            questions=questions,
            client=self.client
        )
        if not quiz_data:
            return

        # --- TOURNAMENT SERVICE ---
        tournament = AppUtils.create_tournament(
            exec_id, [topic_id], user_id, client=self.client)
        if not tournament:
            return

        t_id = tournament["aggregateId"]
        AppUtils.join_tournament(
            t_id, exec_id, user_id, client=self.client)

        # solve_quiz 'starts' the quiz answer aggregate
        tournament_quiz = AppUtils.solve_quiz(
            t_id, user_id, client=self.client)

        if tournament_quiz and "aggregateId" in tournament_quiz:
            # TODO: Currently not working
            # --- ANSWER SERVICE ---
            AppUtils.answer_question(
                quiz_id=tournament_quiz["aggregateId"],
                user_id=user_id,
                question_id=tournament_quiz["questionDtos"][0]["aggregateId"],
                client=self.client
            )
