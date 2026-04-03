from locust import HttpUser, task, between, events
import logging
import requests
import random
from capacity_utils import CapacityAdminUtils, QuizzesInteractionUtils, CapacityValidatorUtils, GATEWAY


class ComplexMultiServiceUser(HttpUser):
    # ! THIS TEST REQUIRES MULTIPLE USERS AND ITERATIONS TO BE EXECUTED PROPERLY
    host = GATEWAY
    wait_time = between(2, 4)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        logging.info("############# TEST START #############")

        req_tr = random.randint(2, 5)
        req_ex = random.randint(2, 5)
        req_us = random.randint(2, 5)
        req_qz = random.randint(2, 5)
        req_qt = random.randint(2, 5)
        req_tp = random.randint(2, 5)
        req_an = random.randint(2, 5)
        environment.reqs = [req_tr, req_ex,
                            req_us, req_qz, req_qt, req_tp, req_an]

        # Configuration including every step for CreateTournament, UpdateTournament, and SolveQuiz
        environment.config = {
            "Capacities": {
                "microservices": [
                    {
                        "name": "tournament",
                        "capacity": 30,
                        "steps": [
                            {"name": "createTournamentStep", "requirement": req_tr},
                            {"name": "updateTournamentStep", "requirement": req_tr},
                            {"name": "solveQuizStep", "requirement": req_tr},
                            {"name": "getTournamentStep", "requirement": req_tr},
                            {"name": "getOriginalTournamentStep",
                                "requirement": req_tr}
                        ]
                    },
                    {
                        "name": "execution",
                        "capacity": 50,
                        "steps": [
                            {"name": "getCourseExecutionStep",
                                "requirement": req_ex},
                            {"name": "getCreatorStep", "requirement": req_ex},
                            {"name": "getCourseExecutionById",
                                "requirement": req_ex},
                            {"name": "getStudentByExecutionIdAndUserId",
                                "requirement": req_ex},
                            {"name": "enrollStudentStep", "requirement": req_ex}
                        ]
                    },
                    {
                        "name": "user",
                        "capacity": 20,
                        "steps": [
                            {"name": "createUserStep", "requirement": req_us},
                            {"name": "getUserStep", "requirement": req_us},
                            {"name": "activateUserStep", "requirement": req_us}
                        ]
                    },
                    {
                        "name": "quiz",
                        "capacity": 25,
                        "steps": [
                            {"name": "generateQuizStep", "requirement": req_qz},
                            {"name": "updateQuizStep", "requirement": req_qz},
                            {"name": "startQuizStep", "requirement": req_qz},
                            {"name": "getQuizById", "requirement": req_qz}
                        ]
                    },
                    {
                        "name": "question",
                        "capacity": 20,
                        "steps": [
                            {"name": "findQuestionsByTopicIdsStep",
                                "requirement": req_qt},
                            {"name": "findQuestionsByTopicIds",
                                "requirement": req_qt},
                            {"name": "getQuestionById", "requirement": req_qt}
                        ]
                    },
                    {
                        "name": "topic",
                        "capacity": 20,
                        "steps": [
                            {"name": "getTopicsStep", "requirement": req_tp}
                        ]
                    },
                    {
                        "name": "answer",
                        "capacity": 20,
                        "steps": [
                            {"name": "startQuizAnswerStep", "requirement": req_an}
                        ]
                    }
                ]
            }
        }

        try:
            CapacityAdminUtils.start_and_load(environment.config)
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
        # Each Locust user works on their own tournament to reduce DB contention
        data = self.environment.test_data
        if not data:
            self.stop(True)
            return

        try:
            # Create user (tournament owner)
            user_id = QuizzesInteractionUtils.create_and_activate_user(
                requests)
            # Create the tournament
            res = QuizzesInteractionUtils.enroll_and_create_tournament(
                requests, data["execution_id"], user_id, data["topic_id"])

            if res.status_code == 200:
                self.t_id = res.json()["aggregateId"]
            else:
                self.stop(True)
        except Exception as e:
            logging.error(f"### User Setup Failed: {e}")
            self.stop(True)

    @task(2)
    def task_update_tournament(self):
        QuizzesInteractionUtils.update_tournament(
            self.client, self.t_id, [self.environment.test_data["topic_id"]])

    @task(1)
    def task_solve_quiz(self):
        data = self.environment.test_data
        user_id = QuizzesInteractionUtils.create_and_activate_user(self.client)
        if user_id:
            QuizzesInteractionUtils.enroll_student(
                self.client, data["execution_id"], user_id)
            QuizzesInteractionUtils.join_tournament(
                self.client, self.t_id, data["execution_id"], user_id)
            QuizzesInteractionUtils.solve_quiz(self.client, self.t_id, user_id)
