from locust import HttpUser, task, between, events
import logging
import requests
import random
import uuid
from capacity_utils import CapacityAdminUtils, QuizzesInteractionUtils, CapacityValidatorUtils, GATEWAY


class ComplexMultiServiceUser(HttpUser):
    host = GATEWAY
    wait_time = between(2, 4)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        logging.info("############# TEST START #############")

        # Randomized requirements
        reqs = [random.randint(2, 5) for _ in range(7)]
        environment.reqs = reqs

        # Configuration including every command for CreateTournament, UpdateTournament, and SolveQuiz
        environment.config = {
            "Capacities": {
                "microservices": [
                    {
                        "name": "tournament",
                        "capacity": 15,
                        "services": [
                            {"name": "UpdateTournament",
                                "requirement": reqs[0]},
                            {"name": "SolveQuiz",
                                "requirement": reqs[0]},
                            {"name": "GetTournamentById",
                                "requirement": reqs[0]},
                            {"name": "AddParticipant",
                                "requirement": reqs[0]}
                        ]
                    },
                    {
                        "name": "execution",
                        "capacity": 15,
                        "services": [
                            {"name": "GetStudentByExecutionIdAndUserId",
                                "requirement": reqs[1]},
                            {"name": "EnrollStudent",
                                "requirement": reqs[1]}
                        ]
                    },
                    {
                        "name": "user",
                        "capacity": 15,
                        "services": [
                            {"name": "CreateUser",
                                "requirement": reqs[2]},
                            {"name": "GetUserById",
                                "requirement": reqs[2]},
                            {"name": "ActivateUser",
                                "requirement": reqs[2]}
                        ]
                    },
                    {
                        "name": "quiz",
                        "capacity": 15,
                        "services": [
                            {"name": "StartTournamentQuiz",
                                "requirement": reqs[3]},
                            {"name": "GetQuizById",
                                "requirement": reqs[3]},
                            {"name": "UpdateGeneratedQuiz",
                                "requirement": reqs[3]}
                        ]
                    },
                    {
                        "name": "question",
                        "capacity": 10,
                        "services": [
                            {"name": "FindQuestionsByTopicIds",
                                "requirement": reqs[4]},
                            {"name": "GetQuestionById",
                                "requirement": reqs[4]},
                        ]
                    },
                    {
                        "name": "topic",
                        "capacity": 5,
                        "services": [
                            {"name": "GetTopicById",
                             "requirement": reqs[5]}
                        ]
                    },
                    {
                        "name": "answer",
                        "capacity": 5,
                        "services": [
                            {"name": "StartQuiz",
                                "requirement": reqs[6]}
                        ]
                    }
                ]
            }
        }

        try:
            environment.scenarios = []
            for i in range(10):
                data = QuizzesInteractionUtils.create_base_data()

                owner_id = QuizzesInteractionUtils.create_and_activate_user(requests)
                if not owner_id:
                    logging.warning("### Skipping scenario: failed to create owner user")
                    continue

                res = QuizzesInteractionUtils.enroll_and_create_tournament(
                    requests, data["execution_id"], owner_id, data["topic_id"])
                if res.status_code != 200:
                    logging.warning(f"### Skipping scenario: tournament creation failed: {res.text}")
                    continue

                environment.scenarios.append({
                    "data": data,
                    "tournament_id": res.json()["aggregateId"]
                })

            environment.scenario_index = 0

            if not environment.scenarios:
                raise RuntimeError("No valid scenarios could be prepared")

            CapacityAdminUtils.start_and_load(environment.config)
            logging.info("### Setup complete ###")
        except Exception as e:
            logging.error(f"### Setup Failed: {e}")
            environment.scenarios = []

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        try:
            report = CapacityValidatorUtils.get_report()
            logging.info("### RESULTS ###")

            reqs = environment.reqs
            CapacityValidatorUtils.assert_max_capacity(
                "tournament", report, int(15/reqs[0]))
            CapacityValidatorUtils.assert_max_capacity(
                "execution", report, int(15/reqs[1]))
            CapacityValidatorUtils.assert_max_capacity(
                "user", report, int(15/reqs[2]))
            CapacityValidatorUtils.assert_max_capacity(
                "quiz", report, int(15/reqs[3]))
            CapacityValidatorUtils.assert_max_capacity(
                "question", report, int(10))
            CapacityValidatorUtils.assert_max_capacity(
                "topic", report, int(5/reqs[5]))
            CapacityValidatorUtils.assert_max_capacity(
                "answer", report, int(5/reqs[6]))
            CapacityAdminUtils.stop_and_cleanup()
            logging.info("############# COMPLEX TEST END #############")
        except Exception as e:
            logging.error(f"### Validation Error: {e}")

    def on_start(self):
        if not self.environment.scenarios:
            self.stop(True)
            return

        idx = self.environment.scenario_index % len(self.environment.scenarios)
        self.environment.scenario_index += 1
        scenario = self.environment.scenarios[idx]
        self.my_data = scenario["data"]
        self.t_id = scenario["tournament_id"]

    @task(2)
    def task_update_tournament(self):
        QuizzesInteractionUtils.update_tournament(
            self.client, self.t_id, [self.my_data["topic_id"]])

    @task(1)
    def task_solve_quiz(self):
        user_id = QuizzesInteractionUtils.create_and_activate_user(self.client)
        if user_id:
            enroll_res = QuizzesInteractionUtils.enroll_student(
                self.client, self.my_data["execution_id"], user_id)

            if enroll_res.status_code != 200:
                return

            join_res = QuizzesInteractionUtils.join_tournament(
                self.client, self.t_id, self.my_data["execution_id"], user_id)

            if join_res.status_code == 200:
                QuizzesInteractionUtils.solve_quiz(self.client, self.t_id, user_id)
