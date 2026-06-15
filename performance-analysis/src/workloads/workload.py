from locust import HttpUser, task, between, events
import logging
from src.simulator_tools.simulator_utils import *
from src.initial_config.baseline_data import BASELINE_SCENARIOS


class ComplexFindUpdateTester(HttpUser):
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        environment.scenario_pool = BASELINE_SCENARIOS.copy()
        SimInterface.start()

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        try:
            SimInterface.stop()
        except Exception as e:
            logging.error(f"Validation Error: {e}")

    def on_start(self):
        # Pop one scenario atomically
        if not self.environment.scenario_pool:
            self.stop(True)
            return

        scenario = self.environment.scenario_pool.pop()

        self.course_id = scenario["course_id"]
        self.topic_id = scenario["topic_id"]
        self.exec_id = scenario["execution_id"]
        self.tourn_id = scenario["tournament_id"]
        self.owner_id = scenario["owner_id"]

    @task(3)
    def find_user(self):
        SimInterface.get_user(self.owner_id, client=self.client)

    @task(2)
    def find_execution(self):
        SimInterface.get_execution(self.exec_id, client=self.client)

    @task(2)
    def find_tournament(self):
        SimInterface.find_tournament(self.tourn_id, client=self.client)

    @task(2)
    def task_update_tournament(self):
        SimInterface.update_tournament(
            self.tourn_id, [self.topic_id], client=self.client)

    @task(1)
    def task_add_student(self):
        new_user = SimInterface.create_user(client=self.client)
        if new_user and "aggregateId" in new_user:
            uid = new_user["aggregateId"]
            if SimInterface.activate_user(uid, client=self.client):
                SimInterface.enroll_student(
                    uid, self.exec_id, client=self.client)
