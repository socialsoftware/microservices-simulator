from locust import task, between
import random
from workload_class import Workload
from src.simulator_tools.simulator_utils import *


class BaseWorkload(Workload):

    def find_user(self):
        SimInterface.get_user(self.owner_id, client=self.client)

    def find_execution(self):
        SimInterface.get_execution(self.exec_id, client=self.client)

    def find_tournament(self):
        SimInterface.find_tournament(self.tourn_id, client=self.client)

    def task_update_tournament(self):
        SimInterface.update_tournament(
            self.tourn_id, [self.topic_id], client=self.client)

    def task_add_student(self):
        res = SimInterface.get_user_executions(self.user_id)

        # If this specific user has already enrolled, skip this task
        if res and res[0]["aggregateId"] == self.exec_id:
            return

        SimInterface.enroll_student(
            self.user_id, self.exec_id, client=self.client)

    @task
    def dynamic_router(self):
        tasks = [
            self.find_user,
            self.find_execution,
            self.find_tournament,
            self.task_update_tournament,
            self.task_add_student
        ]
        weights = [
            30 * self.read_weight,
            25 * self.read_weight,
            25 * self.read_weight,
            10 * self.write_weight,
            10 * self.write_weight
        ]
        random.choices(tasks, weights=weights, k=1)[0]()
