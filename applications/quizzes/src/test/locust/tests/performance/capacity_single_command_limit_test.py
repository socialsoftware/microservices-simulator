from locust import HttpUser, task, between, events
from utils.app_utils import *
from utils.test_utils import *

config = {
    "Capacities": {
        "microservices": [
            {
                "name": "user",
                "capacity": 1,
                "services": [
                    {"name": "CreateUser", "requirement": 1},
                    {"name": "ActivateUser", "requirement": 1},
                    {"name": "GetUserById", "requirement": 1}
                ]
            }
        ]
    }
}


class SingleCommandLimitTester(HttpUser):
    # ! THIS TEST REQUIRES MORE THAN 1 USER TO BE EXECUTED PROPERLY
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        try:
            AppUtils.start()
            AppUtils.inject_configuration(config)
            TestUtils.info("Setup complete!")
        except Exception as e:
            TestUtils.fail(f"Setup Failed: {e}")

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        try:
            report = AppUtils.get_capacity_report()
            AppUtils.stop()

            TestUtils.info("====== RESULTS ======")
            # Verify that UserService never exceeded 1 concurrent request
            TestUtils.assert_concurrency_range("user", report, 1, 1)
        except Exception as e:
            TestUtils.fail(f"Validation Error: {e}")

    @task
    def execute_capacity_workflow(self):
        AppUtils.create_and_activate_user(client=self.client)
