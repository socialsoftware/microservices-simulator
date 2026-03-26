from locust import HttpUser, task, between, events
import logging
from capacity_utils import CapacityAdminUtils, QuizzesInteractionUtils, CapacityValidatorUtils, GATEWAY


class CapacityAutomationUser(HttpUser):
    # ! THIS TEST REQUIRES MORE THAN 1 USER TO BE EXECUTED PROPERLY
    host = GATEWAY
    wait_time = between(0.01, 0.1)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        logging.info("############# TEST START #############")
        # Limit TournamentService to only 1 request at a time
        config = {
            "Capacities": {
                "microservices": [
                    {
                        "name": "TournamentService",
                        "capacity": 1,
                        "endpoints": [
                            {"name": "CreateTournamentFunctionalitySagas",
                                "requirement": 1}
                        ]
                    }
                ]
            }
        }
        try:
            CapacityAdminUtils.start_and_load(config)
            environment.test_data = QuizzesInteractionUtils.create_base_data()
            logging.info("### Setup complete ###")
        except Exception as e:
            logging.error(f"### Setup Failed: {e}")
            environment.test_data = None

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        try:
            report = CapacityValidatorUtils.get_report()
            logging.info("### RESULTS ###")
            # Verify that TournamentService never exceeded 1 concurrent request
            CapacityValidatorUtils.assert_max_capacity(
                "TournamentService", report, 1)
            CapacityAdminUtils.stop_and_cleanup()

            logging.info("############# TEST END #############")
        except Exception as e:
            logging.error(f"### Validation Error: {e}")

    @task
    def execute_capacity_workflow(self):
        data = self.environment.test_data
        if not data:
            return

        user_id = QuizzesInteractionUtils.create_and_activate_user(self.client)
        if user_id:
            # CreateTournamentFunctionalitySagas (TournamentService)
            QuizzesInteractionUtils.enroll_and_create_tournament(
                self.client, data["execution_id"], user_id, data["topic_id"])
