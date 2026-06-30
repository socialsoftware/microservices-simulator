from locust import task, between
import random
from workload_class import Workload
from src.simulator_tools.simulator_utils import SimInterface

"""
Simulates an event where students are creating and playing user-generated tournaments.
This heavily utilizes tournament, quiz, question, topic, and answer.

Creating and playing a tournament triggers a massive cascade of microservice communication.
- Tournament talks to Quiz to generate.
- Quiz talks to Question and Topic to select questions.
- Solving it triggers Answer and fires events back to Tournament to update scores.
The agent must learn to group these highly-chatty services on the same physical node.
"""


class TournamentPeriodWorkload(Workload):
    wait_time = between(0.1, 0.5)

    def on_start(self):
        super().on_start()
        self.active_tourns = []

    def create_tournament(self):
        tourn = SimInterface.create_tournament(
            self.exec_id, [self.topic_id], self.owner_id, client=self.client)
        if tourn and "id" in tourn:
            self.active_tourns.append((tourn["id"], self.exec_id))

    def join_tournament(self):
        if self.active_tourns:
            tourn_id = self.active_tourns[-1][0]
            exec_id = self.active_tourns[-1][1]
            SimInterface.enroll_student(self.user_id, exec_id)
            SimInterface.join_tournament(tourn_id, exec_id, self.user_id)
            SimInterface.enroll_in_tournament(
                tourn_id, self.user_id, client=self.client)

    def solve_tournament(self):
        if self.active_tourns:
            tourn_id = self.active_tourns[-1]
            SimInterface.solve_quiz(
                tourn_id, self.user_id, client=self.client)

    @task
    def dynamic_router(self):
        tasks = [
            self.create_tournament,
            self.join_tournament,
            self.solve_tournament
        ]
        weights = [
            10 * self.write_weight,
            20 * self.write_weight,
            70 * self.write_weight
        ]
        random.choices(tasks, weights=weights, k=1)[0]()
