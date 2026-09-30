from locust import task
import random
from workload_class import Workload
from src.simulator_tools.simulator_utils import SimInterface


class UserWorkload(Workload):

    def get_user(self):
        SimInterface.get_user(self.user_id, client=self.client)

    def get_all_students(self):
        SimInterface.get_students(client=self.client)

    def create_active_user(self):
        SimInterface.create_and_activate_user(client=self.client)

    @task
    def dynamic_router(self):
        tasks = [
            self.get_user,
            self.get_all_students,
            self.create_active_user
        ]
        weights = [
            30 * self.read_weight,
            20 * self.read_weight,
            30 * self.write_weight
        ]

        random.choices(tasks, weights=weights, k=1)[0]()
