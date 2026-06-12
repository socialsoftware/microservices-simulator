from locust import HttpUser, task, between, events
import logging
from src.simulator_tools.simulator_utils import *


class ComplexFindUpdateTester(HttpUser):
    host = GATEWAY
    wait_time = between(1, 3)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        try:
            environment.scenarios = []
            for _ in range(6):
                data = SimInterface.create_base_data()
                if not data:
                    continue

                owner_id = SimInterface.create_and_activate_user()
                if not owner_id:
                    continue

                res_data = SimInterface.enroll_and_create_tournament(
                    data["execution_id"], owner_id, data["topic_id"])

                if not res_data:
                    continue

                environment.scenarios.append({
                    "data": data,
                    "tournament_id": res_data["aggregateId"],
                    "owner_id": owner_id
                })

            environment.scenario_index = 0

            if not environment.scenarios:
                raise RuntimeError("No valid scenarios could be prepared")

            SimInterface.start()
        except Exception as e:
            logging.error(f"Setup Failed: {e}")
            environment.scenarios = []

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        try:
            SimInterface.stop()
        except Exception as e:
            logging.error(f"Validation Error: {e}")

    def on_start(self):
        if not self.environment.scenarios:
            self.stop(True)
            return

        idx = self.environment.scenario_index % len(self.environment.scenarios)
        self.environment.scenario_index += 1
        scenario = self.environment.scenarios[idx]
        self.my_data = scenario["data"]
        self.t_id = scenario["tournament_id"]
        self.owner_id = scenario["owner_id"]

    @task(3)
    def find_user(self):
        SimInterface.get_user(self.owner_id, client=self.client)

    @task(3)
    def find_execution(self):
        SimInterface.get_execution(
            self.my_data["execution_id"], client=self.client)

    @task(3)
    def find_tournament(self):
        SimInterface.find_tournament(self.t_id, client=self.client)

    @task(1)
    def task_update_execution(self):
        SimInterface.update_tournament(
            self.t_id, [self.my_data["topic_id"]], client=self.client)

    @task(1)
    def task_update_execution(self):
        new_user_id = SimInterface.create_user(client=self.client)
        if new_user_id:
            SimInterface.activate_user(
                new_user_id["aggregateId"], client=self.client)
            SimInterface.enroll_student(
                new_user_id["aggregateId"], self.my_data["execution_id"], client=self.client)
