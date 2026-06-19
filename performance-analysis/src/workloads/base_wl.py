from locust import task, between
from workload_class import Workload
from src.simulator_tools.simulator_utils import *


class BaseWorkload(Workload):
    wait_time = between(0.1, 0.5)

    @task(30)
    def find_user(self):
        SimInterface.get_user(self.owner_id, client=self.client)

    @task(25)
    def find_execution(self):
        SimInterface.get_execution(self.exec_id, client=self.client)

    @task(25)
    def find_tournament(self):
        SimInterface.find_tournament(self.tourn_id, client=self.client)

    @task(10)
    def task_update_tournament(self):
        SimInterface.update_tournament(
            self.tourn_id, [self.topic_id], client=self.client)

    @task(10)
    def task_add_student(self):
        res = SimInterface.get_user_executions(self.user_id)

        # If this specific user has already enrolled, skip this task
        if res and res[0]["aggregateId"] == self.exec_id:
            return

        SimInterface.enroll_student(
            self.user_id, self.exec_id, client=self.client)
