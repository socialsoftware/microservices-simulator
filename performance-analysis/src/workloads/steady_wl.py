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

# ! TODO - How to use quizzes?


class SteadyWorkload(Workload):
    wait_time = between(0.1, 0.5)

    # --- READS (80% weight) ---

    def browse_courses_and_executions(self):
        SimInterface.get_course(self.course_id, client=self.client)
        SimInterface.get_execution(self.exec_id, client=self.client)

    def view_user_profile(self):
        SimInterface.get_user(self.user_id, client=self.client)

    def browse_topics_and_questions(self):
        SimInterface.get_topics_for_course(
            self.course_id, client=self.client)
        SimInterface.get_questions_for_course(
            self.course_id, client=self.client)

    def browse_tournaments(self):
        SimInterface.get_tournaments_for_course_execution(
            self.exec_id, client=self.client)

    # --- WRITES (20% weight) ---
    def teacher_creates_question(self):
        topic_data = SimInterface.get_topics_for_course
        if topic_data:
            SimInterface.create_questions(
                self.course_id, topic_data, client=self.client, count=1)

    def student_solves_quiz(self):
        # Simulates starting, answering, and concluding a low-stakes quiz
        if SimInterface.enroll_student(self.user_id, self.exec_id, client=self.client):
            if SimInterface.join_tournament(self.tourn_id, self.exec_id, self.user_id, client=self.client):
                SimInterface.solve_quiz(
                    self.tourn_id, self.user_id, client=self.client)

    @task
    def dynamic_router(self):
        tasks = [
            self.browse_courses_and_executions,
            self.view_user_profile,
            self.browse_topics_and_questions,
            self.browse_tournaments,
            self.teacher_creates_question,
            self.student_solves_quiz
        ]
        weights = [
            20 * self.read_weight,
            20 * self.read_weight,
            20 * self.read_weight,
            20 * self.read_weight,
            5 * self.write_weight,
            10 * self.write_weight
        ]
        random.choices(tasks, weights=weights, k=1)[0]()
