from locust import task, between
import random
from workload_class import Workload
from src.simulator_tools.simulator_utils import SimInterface

"""
Represents a typical non-eventful day.
Features a balanced mix of ~80% reads and ~20% writes spread relatively evenly across all microservices.

It forces the agent to establish a baseline topology that provides acceptable performance across 
the board without over-optimizing for any single service at the expense of another.
"""


class SteadyWorkload(Workload):
    active_quizzes = []

    def on_start(self):
        super().on_start()
        self.started_quizzes = set()
        self.enrolled_executions = {self.exec_id}

    def browse_courses_and_executions(self):
        SimInterface.get_course(self.course_id, client=self.client)
        SimInterface.get_execution(self.exec_id, client=self.client)

    def view_user_profile(self):
        SimInterface.get_user(self.user_id, client=self.client)
        SimInterface.get_user_executions(self.user_id, client=self.client)

    def browse_topics_and_questions(self):
        SimInterface.get_topics_for_course(
            self.course_id, client=self.client)
        SimInterface.get_questions_for_course(
            self.course_id, client=self.client)

    def browse_tournaments(self):
        SimInterface.get_tournaments_for_course_execution(
            self.exec_id, client=self.client)

    def teacher_creates_question(self):
        topic_data = SimInterface.get_topics_for_course(
            self.course_id, client=self.client)
        if topic_data and len(topic_data) > 0:
            SimInterface.create_questions(
                self.course_id, topic_data[0], client=self.client, count=1)

    def teacher_creates_quiz(self):
        questions = SimInterface.get_questions_for_course(
            self.course_id, client=self.client)
        if questions and len(questions) > 0:
            quiz_questions = questions[:2]
            quiz = SimInterface.create_quiz(
                self.exec_id, questions=quiz_questions, client=self.client)
            if quiz and "aggregateId" in quiz:
                SteadyWorkload.active_quizzes.append((self.exec_id, quiz))

    def student_solves_quiz(self):
        if not SteadyWorkload.active_quizzes:
            return

        quiz_exec_id, quiz_data = random.choice(SteadyWorkload.active_quizzes)
        quiz_id = quiz_data["aggregateId"]

        if quiz_exec_id not in self.enrolled_executions:
            SimInterface.enroll_student(
                self.user_id, quiz_exec_id, client=self.client)
            self.enrolled_executions.add(quiz_exec_id)

        if quiz_id not in self.started_quizzes:
            has_started = SimInterface.start_quiz(
                quiz_id, quiz_exec_id, self.user_id, client=self.client)
            if has_started:
                self.started_quizzes.add(quiz_id)

        questions = quiz_data.get("questionDtos", [])
        if questions:
            selected_question = random.choice(questions)
            question_id = selected_question["aggregateId"]
            SimInterface.answer_question(
                quiz_id, self.user_id, question_id, client=self.client)

    @task
    def dynamic_router(self):
        tasks = [
            self.browse_courses_and_executions,
            self.view_user_profile,
            self.browse_topics_and_questions,
            self.browse_tournaments,
            self.teacher_creates_question,
            self.teacher_creates_quiz,
            self.student_solves_quiz
        ]
        weights = [
            16 * self.read_weight,
            16 * self.read_weight,
            16 * self.read_weight,
            16 * self.read_weight,
            12 * self.write_weight,
            12 * self.write_weight,
            12 * self.write_weight
        ]
        random.choices(tasks, weights=weights, k=1)[0]()
