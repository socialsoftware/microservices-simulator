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
    global_tournaments = set()
    # Track which user_ids have joined which tournaments to prevent duplicates
    # even if multiple Locust instances share the same user_id
    global_joined = set()

    def browse_tournaments(self):
        SimInterface.get_tournaments_for_course_execution(
            self.exec_id, client=self.client)

    def on_start(self):
        super().on_start()
        TournamentPeriodWorkload.global_tournaments.add(
            (self.tourn_id, self.exec_id))

    def create_tournament(self):
        tournament = SimInterface.create_tournament(
            self.exec_id, [self.topic_id], self.owner_id, client=self.client)
        if tournament and "aggregateId" in tournament:
            TournamentPeriodWorkload.global_tournaments.add(
                (tournament["aggregateId"], self.exec_id))

    def join_and_solve_tournament(self):
        # Pick from tournaments this user_id hasn't joined yet
        available = [t for t in TournamentPeriodWorkload.global_tournaments
                     if (t[0], self.user_id) not in TournamentPeriodWorkload.global_joined]

        if not available:
            # Fallback to creating one if they've solved everything in the pool
            self.create_tournament()
            return

        t_id, e_id = random.choice(available)

        TournamentPeriodWorkload.global_joined.add((t_id, self.user_id))

        SimInterface.enroll_student(
            self.user_id, e_id, client=self.client)

        SimInterface.join_tournament(
            t_id, e_id, self.user_id, client=self.client)

        SimInterface.solve_quiz(
            t_id, self.user_id, client=self.client)

    @task
    def dynamic_router(self):
        tasks = [
            self.browse_tournaments,
            self.create_tournament,
            self.join_and_solve_tournament
        ]
        weights = [
            35 * self.read_weight,
            15 * self.write_weight,
            50 * self.write_weight
        ]
        random.choices(tasks, weights=weights, k=1)[0]()
