from locust import HttpUser, events
from src.simulator_tools.simulator_utils import SimInterface, GATEWAY
from src.initial_config.baseline_data import BASELINE_SCENARIOS


class Workload(HttpUser):
    abstract = True
    host = GATEWAY

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        print("Starting........................")
        environment.scenario_pool = BASELINE_SCENARIOS.copy()
        SimInterface.start()

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        print("Stoping........................")
        SimInterface.stop()

    def on_start(self):
        if not self.environment.scenario_pool:
            self.stop(True)
            return

        # Pop one scenario atomically
        scenario = self.environment.scenario_pool.pop()

        self.course_id = scenario["course_id"]
        self.topic_id = scenario["topic_id"]
        self.exec_id = scenario["execution_id"]
        self.tourn_id = scenario["tournament_id"]
        self.owner_id = scenario["owner_id"]
        self.user_id = scenario["user_id"]
