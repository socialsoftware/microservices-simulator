from locust import task
import random
from workload_class import Workload
from src.simulator_tools.simulator_utils import SimInterface


class ChangeOfYearWorkload(Workload):
    global_users = set()
    global_topics = set()
    global_questions = set()

    def on_start(self):
        super().on_start()
        ChangeOfYearWorkload.global_users.add(self.user_id)
        ChangeOfYearWorkload.global_topics.add((self.topic_id, self.course_id))

    def create_and_activate_user(self):
        user = SimInterface.create_user(client=self.client)
        if user and "aggregateId" in user:
            uid = user["aggregateId"]
            SimInterface.activate_user(uid, client=self.client)
            ChangeOfYearWorkload.global_users.add(uid)

    def create_topic(self):
        topic = SimInterface.create_topic(self.course_id, client=self.client)
        if topic and "aggregateId" in topic:
            ChangeOfYearWorkload.global_topics.add(
                (topic["aggregateId"], self.course_id))

    def create_questions(self):
        try:
            topic_id, course_id = ChangeOfYearWorkload.global_topics.pop()
        except KeyError:
            self.create_topic()
            return

        try:
            questions = SimInterface.create_questions(
                course_id, {"aggregateId": topic_id}, count=1, client=self.client)
            if questions and len(questions) > 0 and "aggregateId" in questions[0]:
                ChangeOfYearWorkload.global_questions.add(
                    questions[0]["aggregateId"])
        finally:
            ChangeOfYearWorkload.global_topics.add((topic_id, course_id))

    def cleanup_user(self):
        try:
            uid = ChangeOfYearWorkload.global_users.pop()
        except KeyError:
            self.create_and_activate_user()
            return

        SimInterface.deactivate_user(uid, client=self.client)
        if random.random() > 0.5:
            SimInterface.delete_user(uid, client=self.client)

    def cleanup_topic(self):
        try:
            topic_id, _ = ChangeOfYearWorkload.global_topics.pop()
        except KeyError:
            self.create_topic()
            return
        SimInterface.delete_topic(topic_id, client=self.client)

    def cleanup_question(self):
        try:
            question_id = ChangeOfYearWorkload.global_questions.pop()
        except KeyError:
            self.create_questions()
            return
        SimInterface.remove_question(question_id, client=self.client)

    def update_quiz(self):
        tourn = SimInterface.find_tournament(self.tourn_id, client=self.client)

        if tourn and "quiz" in tourn and tourn["quiz"]:
            quiz_id = tourn["quiz"]["aggregateId"]
            quiz_data = SimInterface.get_quiz(quiz_id, client=self.client)
            if quiz_data:
                questions = quiz_data.get("questionDtos", [])
                SimInterface.update_quiz(
                    quiz_id,
                    questions=questions,
                    client=self.client
                )

    @task
    def dynamic_router(self):
        tasks = [
            self.create_and_activate_user,
            self.create_topic,
            self.create_questions,
            self.update_quiz,
            self.cleanup_user,
            self.cleanup_topic,
            self.cleanup_question
        ]

        w = self.write_weight
        weights = [
            10 * w,  # create user
            10 * w,  # create topic
            10 * w,  # create question
            17 * w,  # update quiz
            17 * w,  # cleanup user
            17 * w,  # cleanup topic
            17 * w,  # cleanup question
        ]

        random.choices(tasks, weights=weights, k=1)[0]()
