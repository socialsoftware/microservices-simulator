from locust import task, between
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

    @task(10)
    def create_tournament(self):
        tourn = SimInterface.create_tournament(
            self.exec_id, [self.topic_id], self.owner_id, client=self.client)
        if tourn and "id" in tourn:
            self.active_tourns.append((tourn["id"], self.exec_id))

    @task(20)
    def join_tournament(self):
        if self.active_tourns:
            tourn_id = self.active_tourns[-1][0]
            exec_id = self.active_tourns[-1][1]
            SimInterface.enroll_student(self.user_id, exec_id)
            SimInterface.join_tournament(tourn_id, exec_id, self.user_id)
            SimInterface.enroll_in_tournament(
                tourn_id, self.user_id, client=self.client)

    @task(70)
    def solve_tournament(self):
        if self.active_tourns:
            tourn_id = self.active_tourns[-1]
            SimInterface.solve_quiz(
                tourn_id, self.user_id, client=self.client)
