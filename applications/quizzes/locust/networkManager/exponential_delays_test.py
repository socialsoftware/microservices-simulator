from locust import HttpUser, task, between, events
import logging
from network_utils import SimulatorAdminUtils, QuizzesInteractionUtils, NetworkValidatorUtils, GATEWAY


class NetworkExponentialUser(HttpUser):
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        logging.info("############# TEST START #############")
        config = {
            "Placement": {
                "nodes": [
                    {"name": "Node1", "microservices": ["user", "execution"]},
                    {"name": "Node2", "microservices": ["tournament", "quiz"]}
                ]},
            "Delays": {
                "USE_CSV_INJECTION": False,
                "USE_RANDOM_DISTRIBUTIONS": True,
                "intraservice": {"exp": [1.0, 0.1]},
                "intranode":    {"exp": [4.0, 0.1]},
                "internode":    {"exp": [7.0, 0.1]}
            }
        }
        try:
            SimulatorAdminUtils.start_and_load(config)
            environment.test_data = QuizzesInteractionUtils.create_base_data()
            logging.info("### Setup complete ###")
        except Exception as e:
            logging.error(f"### Setup Failed: {e}")
            environment.test_data = None

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        try:
            report = NetworkValidatorUtils.get_report()
            logging.info("### RESULTS ###")
            NetworkValidatorUtils.assert_statistical_range(
                "Intraservice", NetworkValidatorUtils.get_delays(report, "CreateUserCommand"), 3)
            NetworkValidatorUtils.assert_statistical_range(
                "Intranode", NetworkValidatorUtils.get_delays(report, "GenerateQuizCommand"), 55)
            NetworkValidatorUtils.assert_statistical_range(
                "Internode", NetworkValidatorUtils.get_delays(report, "GetCourseExecutionByIdCommand"), 1100)
            SimulatorAdminUtils.stop_and_cleanup()

            logging.info("############# TEST END #############")
        except Exception as e:
            logging.error(f"### Validation Error: {e}")

    @task
    def execute_validation_workflow(self):
        data = self.environment.test_data
        if not data:
            return

        user_id = QuizzesInteractionUtils.create_and_activate_user(self.client)
        if user_id:
            QuizzesInteractionUtils.enroll_and_create_tournament(
                self.client, data["execution_id"], user_id, data["topic_id"])
