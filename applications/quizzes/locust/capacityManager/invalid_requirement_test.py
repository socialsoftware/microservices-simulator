from locust import HttpUser, task, between, events
import logging
from capacity_utils import CapacityAdminUtils, CapacityValidatorUtils, GATEWAY


class InvalidNodeCapacityUser(HttpUser):
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        logging.info(
            "############# INVALID NODE LIMIT TEST START #############")
        config = {
            "Placement": {
                "nodes": [
                    {"name": "Node1", "capacity": 50,
                        "microservices": ["user", "course"]},
                    {"name": "Node2", "capacity": 50,
                        "microservices": ["tournament", "execution"]},
                    {"name": "Node3", "capacity": 50, "microservices": [
                        "quiz", "question", "topic", "answer"]}
                ]
            },
            "Capacities": {
                "microservices": [
                    {"name": "tournament", "capacity": 10, "steps": []},
                    {"name": "execution", "capacity": 5, "steps": [{"name": "getCourseExecutionStep",
                                                                    "requirement": -1}]}
                ]
            }
        }
        try:
            # We expect this call to fail with 500 because of SimulatorException
            CapacityAdminUtils.start_and_load(config)
            logging.error(
                "### >> FAIL: Setup succeeded but should have failed!")
        except Exception as e:
            logging.info(f"### Setup failed as expected: {e}")

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        try:
            report = CapacityValidatorUtils.get_report()
            logging.info("### RESULTS ###")
            # Node2 sum: 10 + 5 = 15. [Limit is 5 - Invalid]

            if "CONFIGURATION ERROR" in report and "Invalid requirement for step" in report:
                logging.info(
                    "### >> PASS: Correct validation error found in report.")
            else:
                logging.error(
                    "### >> FAIL: Validation error NOT found in report, or report is missing.")

            CapacityAdminUtils.stop_and_cleanup()
            logging.info("############# TEST END #############")
        except Exception as e:
            logging.error(f"### Validation Error: {e}")

    @task
    def execute_workflow(self):
        # This task should not run
        pass
