import random
import gevent
from locust import HttpUser, events
from src.simulator_tools.simulator_utils import SimInterface
from src.initial_config.baseline_data import BASELINE_SCENARIOS


# Maximum time (in seconds) to wait for in-flight requests to finish after reaching the iteration target
ITERATION_GRACE_PERIOD_SEC = 5.0


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

        options = environment.parsed_options
        runner = environment.runner

        if getattr(options, "iterations", 0) > 0:
            def iteration_watcher():
                target_reached_time = None
                while runner.state not in ["stopped", "cleanup"]:
                    gevent.sleep(0.05)
                    iterations_started = getattr(runner, "iterations_started", 0)
                    target_reached = getattr(runner, "iteration_target_reached", False) or (iterations_started >= options.iterations)

                    if target_reached:
                        if target_reached_time is None:
                            target_reached_time = gevent.time.time()

                        if runner.user_count == 0 or (gevent.time.time() - target_reached_time > ITERATION_GRACE_PERIOD_SEC):
                            gevent.sleep(0.1)
                            runner.quit()
                            break

            gevent.spawn(iteration_watcher)

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

        self.read_weight = self.environment.parsed_options.read_weight
        self.write_weight = self.environment.parsed_options.write_weight

        # Pick a random scenario so we never run out, preventing infinite spawn loops
        scenario = random.choice(self.environment.scenario_pool)

        self.course_id = scenario["course_id"]
        self.topic_id = scenario["topic_id"]
        self.exec_id = scenario["execution_id"]
        self.tourn_id = scenario["tournament_id"]
        self.owner_id = scenario["owner_id"]
        self.user_id = scenario["user_id"]
