from locust import HttpUser, task, between, events
import logging
from capacity_utils import CapacityAdminUtils, QuizzesInteractionUtils, CapacityValidatorUtils, GATEWAY


class CapacityAutomationUser(HttpUser):
    # ! THIS TEST REQUIRES MORE THAN 1 USER TO BE EXECUTED PROPERLY
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        logging.info("############# TEST START #############")
        # Limit UserService to only 1 request at a time
        config = {
            "Capacities": {
                "microservices": [
                    {
                        "name": "user",
                        "capacity": 1,
                        "services": [
                            {
                                "name": "CreateUser",
                                "requirement": 1
                            },
                            {
                                "name": "ActivateUser",
                                "requirement": 1
                            },
                            {
                                "name": "GetUserById",
                                "requirement": 1
                            }
                        ]
                    }
                ]
            }
        }
        try:
            CapacityAdminUtils.start_and_load(config)
            logging.info("### Setup complete ###")
        except Exception as e:
            logging.error(f"### Setup Failed: {e}")
            environment.test_data = None

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        try:
            report = CapacityValidatorUtils.get_report()
            logging.info("### RESULTS ###")
            # Verify that UserService never exceeded 1 concurrent request
            CapacityValidatorUtils.assert_concurrency_range(
                "user", report, 1, 1)
            CapacityAdminUtils.stop_and_cleanup()

            logging.info("############# TEST END #############")
        except Exception as e:
            logging.error(f"### Validation Error: {e}")

    @task
    def execute_capacity_workflow(self):
        QuizzesInteractionUtils.create_and_activate_user(self.client)
