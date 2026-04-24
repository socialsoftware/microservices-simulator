from locust import HttpUser, task, between, events
import logging
from capacity_utils import CapacityAdminUtils, QuizzesInteractionUtils, CapacityValidatorUtils, GATEWAY


class ValidNodeCapacityUser(HttpUser):
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        logging.info("############# VALID NODE LIMIT TEST START #############")
        config = {
            "Placement": {
                "nodes": [
                    {"name": "Node1", "capacity": 50,
                        "microservices": ["user", "course"]},
                    {"name": "Node2", "capacity": 20,
                        "microservices": ["tournament", "execution"]},
                    {"name": "Node3", "capacity": 50, "microservices": [
                        "quiz", "question", "topic", "answer"]}
                ]
            },
            "Capacities": {
                "microservices": [
                    {"name": "tournament", "capacity": 10, "services": []},
                    {"name": "execution", "capacity": 5, "services": []}
                ]
            }
        }
        try:
            CapacityAdminUtils.start_and_load(config)
            environment.test_data = QuizzesInteractionUtils.create_base_data()
            QuizzesInteractionUtils.create_questions(
                environment.test_data["course_id"], environment.test_data["topic_data"])
            logging.info("### Setup complete (Valid Config) ###")
        except Exception as e:
            logging.error(f"### Setup Failed Unexpectedly: {e}")
            environment.test_data = None

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        try:
            report = CapacityValidatorUtils.get_report()
            logging.info("### RESULTS ###")
            # Node2 sum: 10 + 5 = 15. [Limit is 20 - Valid]

            if "VALIDATION ERROR" in report:
                logging.error(
                    "### >> FAIL: Configuration error found in report when none was expected!")
            else:
                logging.info(
                    "### >> PASS: No configuration errors found in report.")
            CapacityAdminUtils.stop_and_cleanup()
            logging.info("############# TEST END #############")
        except Exception as e:
            logging.error(f"### Validation Error: {e}")

    @task
    def execute_workflow(self):
        data = self.environment.test_data
        if not data:
            return
        user_id = QuizzesInteractionUtils.create_and_activate_user(self.client)
        if user_id:
            QuizzesInteractionUtils.enroll_and_create_tournament(
                self.client, data["execution_id"], user_id, data["topic_id"])
