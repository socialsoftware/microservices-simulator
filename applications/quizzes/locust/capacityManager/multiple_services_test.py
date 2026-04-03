from locust import HttpUser, task, between, events
import logging
import datetime
from capacity_utils import CapacityAdminUtils, QuizzesInteractionUtils, CapacityValidatorUtils, GATEWAY


class MultiServiceCapacityUser(HttpUser):
    # ! THIS TEST REQUIRES MULTIPLE USERS TO BE EXECUTED PROPERLY
    host = GATEWAY
    wait_time = between(0.01, 0.1)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        logging.info("############# TEST START #############")
        config = {
            "Capacities": {
                "microservices": [
                    {
                        "name": "tournament",
                        "capacity": 40,
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
                            },
                            {
                                "name": "findTournamentStep",
                                "requirement": 1
                            },
                        ]
                    },
                    {
                        "name": "execution",
                        "capacity": 3,
                        "steps": [
                            {
                                "name": "getUserStep",
                                "requirement": 1
                            },
                            {
                                "name": "enrollStudentStep",
                                "requirement": 2
                            }
                        ]
                    }
                ]
            }
        }
        try:
            environment.test_data = QuizzesInteractionUtils.create_base_data()
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
            # Ensure they are processed within capacity limits
            CapacityValidatorUtils.assert_concurrency_range(
                "tournament", report, 1, 8)
            CapacityValidatorUtils.assert_concurrency_range(
                "execution", report, 1, 3)
            CapacityAdminUtils.stop_and_cleanup()

            logging.info("############# TEST END #############")
        except Exception as e:
            logging.error(f"### Validation Error: {e}")

    @task
    def execute_capacity_workflow(self):
        data = self.environment.test_data
        user_id = QuizzesInteractionUtils.create_and_activate_user()

        if not (data and user_id):
            return

        # AddStudentFunctionalitySagas (ExecutionService)
        QuizzesInteractionUtils.enroll_student(
            self.client, data['execution_id'], user_id)

        # CreateTournamentFunctionalitySagas (TournamentService)
        res = QuizzesInteractionUtils.create_tournament(
            self.client, data["execution_id"], user_id, data["topic_id"])

        # FindTournamentFunctionalitySagas (TournamentService)
        if res.status_code == 200:
            t_id = res.json()["aggregateId"]
            QuizzesInteractionUtils.find_tournament(self.client, t_id)
            self.client.get(f"/tournaments/{t_id}", name="FindTournament")
