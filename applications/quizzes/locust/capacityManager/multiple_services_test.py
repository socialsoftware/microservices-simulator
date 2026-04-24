from locust import HttpUser, task, between, events
import logging
import requests
from capacity_utils import CapacityAdminUtils, QuizzesInteractionUtils, CapacityValidatorUtils, GATEWAY


class MultiServiceCapacityUser(HttpUser):
    # ! THIS TEST REQUIRES MULTIPLE USERS TO BE EXECUTED PROPERLY
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        logging.info("############# TEST START #############")
        config = {
            "Capacities": {
                "microservices": [
                    {
                        "name": "tournament",
                        "capacity": 2,
                        "services": [
                            {
                                "name": "GetTournamentById",
                                "requirement": 1
                            },
                            {
                                "name": "AddParticipant",
                                "requirement": 1
                            }
                        ]
                    },
                    {
                        "name": "execution",
                        "capacity": 3,
                        "services": [
                            {
                                "name": "EnrollStudent",
                                "requirement": 1
                            },
                            {
                                "name": "GetStudentByExecutionIdAndUserId",
                                "requirement": 1
                            }
                        ]
                    },
                    {
                        "name": "user",
                        "capacity": 4,
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
            environment.test_data = QuizzesInteractionUtils.create_base_data()
            owner_id = QuizzesInteractionUtils.create_and_activate_user()
            if not owner_id:
                raise RuntimeError(
                    "Failed to create owner for shared tournament")

            tournament_res = QuizzesInteractionUtils.enroll_and_create_tournament(
                requests,
                environment.test_data["execution_id"],
                owner_id,
                environment.test_data["topic_id"])
            if tournament_res.status_code != 200:
                raise RuntimeError(
                    f"Failed to create shared tournament: {tournament_res.text}")

            environment.tournament_id = tournament_res.json()["aggregateId"]
            CapacityAdminUtils.start_and_load(config)
            logging.info("### Setup complete ###")
        except Exception as e:
            logging.error(f"### Setup Failed: {e}")
            environment.test_data = None
            environment.tournament_id = None

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        try:
            report = CapacityValidatorUtils.get_report()
            logging.info("### RESULTS ###")
            # Ensure they are processed within capacity limits
            CapacityValidatorUtils.assert_concurrency_range(
                "tournament", report, 1, 2)
            CapacityValidatorUtils.assert_concurrency_range(
                "execution", report, 1, 3)
            CapacityValidatorUtils.assert_concurrency_range(
                "user", report, 1, 4)
            CapacityAdminUtils.stop_and_cleanup()

            logging.info("############# TEST END #############")
        except Exception as e:
            logging.error(f"### Validation Error: {e}")

    @task
    def execute_capacity_workflow(self):
        data = self.environment.test_data
        t_id = getattr(self.environment, "tournament_id", None)
        user_id = QuizzesInteractionUtils.create_and_activate_user(self.client)

        if not (data and t_id and user_id):
            return

        # AddStudentFunctionalitySagas (Execution service)
        enroll_res = QuizzesInteractionUtils.enroll_student(
            self.client, data["execution_id"], user_id)

        if enroll_res.status_code == 200:
            # AddParticipantFunctionalitySagas (Execution + Tournament services)
            join_res = QuizzesInteractionUtils.join_tournament(
                self.client, t_id, data["execution_id"], user_id)
            if join_res.status_code == 200:
                # FindTournamentFunctionalitySagas (Tournament service)
                QuizzesInteractionUtils.find_tournament(self.client, t_id)
