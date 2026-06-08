from locust import HttpUser, task, between, events
from utils.app_utils import *
from utils.test_utils import *

config = {
    "Placement": {
        "nodes": [
            {"name": "Node1", "capacity": 10,
                "microservices": ["tournament", "execution"]},
        ]
    },
    "Capacities": {
        "microservices": [
            {"name": "tournament", "capacity": 10, "services": []},
            {"name": "execution", "capacity": 10, "services": []}
        ]
    }
}


class InvalidNodeCapacityTester(HttpUser):
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        try:
            AppUtils.start()
            AppUtils.inject_configuration(config)
            TestUtils.info("Setup complete!")
        except Exception as e:
            TestUtils.fail(f"Setup failed as expected: {e}")

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        try:
            report = AppUtils.get_capacity_report()
            AppUtils.stop()

            TestUtils.info("====== RESULTS ======")

            if "ConfigurationError" in report and "capacity exceeded" in report:
                TestUtils.succeed()
            else:
                TestUtils.fail(
                    "Expected validation error NOT found in capacity report.")
        except Exception as e:
            TestUtils.fail(f"Validation Error: {e}")

    @task
    def noop(self):
        pass
