from locust import HttpUser, task, between, events
import logging
import requests
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
                        "name": "tournament",
                        "capacity": 1,
                        "steps": [
                            {
                                "name": "getTopicsStep",
                                "requirement": 1
                            },
                            {
                                "name": "getCourseExecutionStep",
                                "requirement": 1
                            },
                            {
                                "name": "findQuestionsByTopicIdsStep",
                                "requirement": 1
                            },
                            {
                                "name": "getCreatorStep",
                                "requirement": 1
                            },
                            {
                                "name": "getCourseExecutionById",
                                "requirement": 1
                            },
                            {
                                "name": "generateQuizStep",
                                "requirement": 1
                            },
                            {
                                "name": "createTournamentStep",
                                "requirement": 1
                            }
                        ]
                    }
                ]
            }
        }
        try:
            environment.test_data = QuizzesInteractionUtils.create_base_data()
            environment.user_id = QuizzesInteractionUtils.create_and_activate_user()
            QuizzesInteractionUtils.enroll_student(
                requests, environment.test_data["execution_id"], environment.user_id)

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
            # Verify that TournamentService never exceeded 1 concurrent request
            CapacityValidatorUtils.assert_concurrency_range(
                "tournament", report, 1, 1)
            CapacityAdminUtils.stop_and_cleanup()

            logging.info("############# TEST END #############")
        except Exception as e:
            logging.error(f"### Validation Error: {e}")

    @task
    def execute_capacity_workflow(self):
        data = self.environment.test_data
        user_id = self.environment.user_id
        if not (data and user_id):
            return

        # CreateTournamentFunctionalitySagas (TournamentService)
        QuizzesInteractionUtils.create_tournament(
            self.client, data["execution_id"], user_id, data["topic_id"])
