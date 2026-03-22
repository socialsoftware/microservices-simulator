from locust import HttpUser, task, between, events
import logging
from network_utils import SimulatorAdminUtils, QuizzesInteractionUtils, NetworkValidatorUtils, GATEWAY


class NetworkAutomationUser(HttpUser):
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        logging.info("############# TEST START #############")
        config = {
            "nodes": [
                {"name": "Node1", "microservices": ["user", "execution"]},
                {"name": "Node2", "microservices": ["tournament", "quiz"]}
            ],
            "delays": {
                "intraservice": {"uni": [0, 10]},
                "intranode":    {"uni": [50, 100]},
                "internode":    {"uni": [100, 200]}
            }
        }
        try:
            SimulatorAdminUtils.start_stochastic(config)
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
            NetworkValidatorUtils.assert_range(
                "Intraservice", NetworkValidatorUtils.get_delays(report, "createUserStep"), 0, 10)
            NetworkValidatorUtils.assert_range("Intranode", NetworkValidatorUtils.get_delays(
                report, "generateQuizStep"), 50, 100)
            NetworkValidatorUtils.assert_range("Internode", NetworkValidatorUtils.get_delays(
                report, "getCourseExecutionStep"), 100, 200)
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
