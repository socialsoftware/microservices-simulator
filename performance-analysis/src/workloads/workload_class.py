import random
from locust import HttpUser, events
from src.simulator_tools.simulator_utils import SimInterface
from src.initial_config.baseline_data import BASELINE_SCENARIOS


@events.init_command_line_parser.add_listener
def _(parser):
    parser.add_argument("--read-weight", type=float,
                        default=1.0, help="Multiplier for read tasks")
    parser.add_argument("--write-weight", type=float,
                        default=1.0, help="Multiplier for write tasks")
    parser.add_argument("--wait-time", type=float,
                        default=0.5, help="Base wait time between tasks")


class Workload(HttpUser):
    abstract = True
    host = SimInterface.get_gateway()

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        print("Starting........................")
        environment.scenario_pool = BASELINE_SCENARIOS.copy()
        SimInterface.start()

    def wait_time(self):
        """This method overrides Locust's default behaviour, 
        calling it instead of accessing a static wait_time attibute"""
        base_wait = self.environment.parsed_options.wait_time
        return random.uniform(base_wait, base_wait + 0.2)

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        print("Stoping........................")
        SimInterface.stop()

    def on_start(self):
        if not self.environment.scenario_pool:
            self.stop(True)
            return

        self.read_weight = self.environment.parsed_options.read_weight
        self.write_weight = self.environment.parsed_options.write_weight

        # Pop one scenario atomically
        scenario = self.environment.scenario_pool.pop()

        self.course_id = scenario["course_id"]
        self.topic_id = scenario["topic_id"]
        self.exec_id = scenario["execution_id"]
        self.tourn_id = scenario["tournament_id"]
        self.owner_id = scenario["owner_id"]
        self.user_id = scenario["user_id"]
