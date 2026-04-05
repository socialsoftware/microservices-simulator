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

        # Configuration including every step for CreateTournament, UpdateTournament, and SolveQuiz
        environment.config = {
            "Capacities": {
                "microservices": [
                    {
                        "name": "tournament",
                        "capacity": 30,
                        "steps": [
                            {"name": "createTournamentStep",
                                "requirement": reqs[0]},
                            {"name": "updateTournamentStep",
                                "requirement": reqs[0]},
                            {"name": "solveQuizStep", "requirement": reqs[0]},
                            {"name": "getTournamentStep",
                                "requirement": reqs[0]},
                            {"name": "getOriginalTournamentStep",
                                "requirement": reqs[0]}
                        ]
                    },
                    {
                        "name": "execution",
                        "capacity": 50,
                        "steps": [
                            {"name": "getCourseExecutionStep",
                                "requirement": reqs[1]},
                            {"name": "getCreatorStep", "requirement": reqs[1]},
                            {"name": "getCourseExecutionById",
                                "requirement": reqs[1]},
                            {"name": "getStudentByExecutionIdAndUserId",
                                "requirement": reqs[1]},
                            {"name": "enrollStudentStep",
                                "requirement": reqs[1]}
                        ]
                    },
                    {
                        "name": "user",
                        "capacity": 20,
                        "steps": [
                            {"name": "createUserStep", "requirement": reqs[2]},
                            {"name": "getUserStep", "requirement": reqs[2]},
                            {"name": "activateUserStep",
                                "requirement": reqs[2]}
                        ]
                    },
                    {
                        "name": "quiz",
                        "capacity": 25,
                        "steps": [
                            {"name": "generateQuizStep",
                                "requirement": reqs[3]},
                            {"name": "updateQuizStep", "requirement": reqs[3]},
                            {"name": "startQuizStep", "requirement": reqs[3]},
                            {"name": "getQuizById", "requirement": reqs[3]}
                        ]
                    },
                    {
                        "name": "question",
                        "capacity": 20,
                        "steps": [
                            {"name": "findQuestionsByTopicIdsStep",
                                "requirement": reqs[4]},
                            {"name": "findQuestionsByTopicIds",
                                "requirement": reqs[4]},
                            {"name": "getQuestionById", "requirement": reqs[4]}
                        ]
                    },
                    {
                        "name": "topic",
                        "capacity": 20,
                        "steps": [
                            {"name": "getTopicsStep", "requirement": reqs[5]}
                        ]
                    },
                    {
                        "name": "answer",
                        "capacity": 20,
                        "steps": [
                            {"name": "startQuizAnswerStep",
                                "requirement": reqs[6]}
                        ]
                    }
                ]
            }
        }

        try:
            environment.test_data_pool = []
            for i in range(10):
                data = QuizzesInteractionUtils.create_base_data()
                environment.test_data_pool.append(data)

            CapacityAdminUtils.start_and_load(environment.config)
            logging.info("### Setup complete ###")
        except Exception as e:
            logging.error(f"### Setup Failed: {e}")
            environment.test_data_pool = []

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        try:
            report = CapacityValidatorUtils.get_report()
            logging.info("### RESULTS ###")

            reqs = environment.reqs
            CapacityValidatorUtils.assert_max_capacity(
                "tournament", report, int(30/reqs[0]))
            CapacityValidatorUtils.assert_max_capacity(
                "execution", report, int(50/reqs[1]))
            CapacityValidatorUtils.assert_max_capacity(
                "user", report, int(20/reqs[2]))
            CapacityValidatorUtils.assert_max_capacity(
                "quiz", report, int(25/reqs[3]))
            CapacityValidatorUtils.assert_max_capacity(
                "question", report, int(20/reqs[4]))
            CapacityValidatorUtils.assert_max_capacity(
                "topic", report, int(20/reqs[5]))
            CapacityValidatorUtils.assert_max_capacity(
                "answer", report, int(20/reqs[6]))
            CapacityAdminUtils.stop_and_cleanup()
            logging.info("############# COMPLEX TEST END #############")
        except Exception as e:
            logging.error(f"### Validation Error: {e}")

    def on_start(self):
        if not self.environment.test_data_pool:
            self.stop(True)
            return

        print("start")
        # Pick a random data-set
        self.my_data = random.choice(self.environment.test_data_pool)

        try:
            # Create user (tournament owner)
            user_id = QuizzesInteractionUtils.create_and_activate_user(
                requests)

            # Create the tournament
            res = QuizzesInteractionUtils.enroll_and_create_tournament(
                requests, self.my_data["execution_id"], user_id, self.my_data["topic_id"])

            if res.status_code == 200:
                self.t_id = res.json()["aggregateId"]
            else:
                logging.error(f"### Tournament creation failed: {res.text}")
                self.stop(True)
        except Exception as e:
            logging.error(f"### User Setup Failed: {e}")
            self.stop(True)

    @task(2)
    def task_update_tournament(self):
        QuizzesInteractionUtils.update_tournament(
            self.client, self.t_id, [self.my_data["topic_id"]])

    @task(1)
    def task_solve_quiz(self):
        user_id = QuizzesInteractionUtils.create_and_activate_user(self.client)
        if user_id:
            QuizzesInteractionUtils.enroll_student(
                self.client, self.my_data["execution_id"], user_id)
            QuizzesInteractionUtils.join_tournament(
                self.client, self.t_id, self.my_data["execution_id"], user_id)
            QuizzesInteractionUtils.solve_quiz(self.client, self.t_id, user_id)
