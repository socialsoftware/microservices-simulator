from locust import HttpUser, task, between, events
import logging
import datetime
import uuid
from capacity_utils import CapacityAdminUtils, QuizzesInteractionUtils, CapacityValidatorUtils, GATEWAY


class ComplexMultiServiceUser(HttpUser):
    # ! THIS TEST REQUIRES MULTIPLE USERS AND ITERATIONS TO BE EXECUTED PROPERLY
    host = GATEWAY
    wait_time = between(0.01, 0.05)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        logging.info("############# COMPLEX TEST START #############")
        config = {
            "microservices": [
                {"name": "TournamentService", "capacity": 10},
                {"name": "ExecutionService", "capacity": 6},
                {"name": "UserService", "capacity": 4}
            ],
            "endpoints": [
                {"name": "CreateTournamentFunctionalitySagas",
                    "microservice": "TournamentService", "requirement": 5},
                {"name": "FindTournamentFunctionalitySagas",
                    "microservice": "TournamentService", "requirement": 2},
                {"name": "AddStudentFunctionalitySagas",
                    "microservice": "ExecutionService", "requirement": 3},
                {"name": "GetCourseExecutionByIdFunctionalitySagas",
                    "microservice": "ExecutionService", "requirement": 1},
                {"name": "CreateUserFunctionalitySagas",
                    "microservice": "UserService", "requirement": 1},
                {"name": "ActivateUserFunctionalitySagas",
                    "microservice": "UserService", "requirement": 1}
            ]
        }
        try:
            CapacityAdminUtils.start_and_load(config)
            data = QuizzesInteractionUtils.create_base_data()
            QuizzesInteractionUtils.create_questions(
                data["course_id"], data["topic_data"])
            environment.test_data = data
            logging.info(
                "### Multi-service configuration injected and base data created ###")
        except Exception as e:
            logging.error(f"### Setup Failed: {e}")
            environment.test_data = None

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        try:
            report = CapacityValidatorUtils.get_report()
            logging.info("### RESULTS ###")
            # Ensure at least 1 request from each service is executed and they dont surpass the capacity limit
            CapacityValidatorUtils.assert_concurrency_range(
                "TournamentService", report, 1, 5)
            CapacityValidatorUtils.assert_concurrency_range(
                "ExecutionService", report, 1, 6)
            CapacityValidatorUtils.assert_concurrency_range(
                "UserService", report, 1, 4)

            CapacityAdminUtils.stop_and_cleanup()
            logging.info("############# COMPLEX TEST END #############")
        except Exception as e:
            logging.error(f"### Validation Error: {e}")

    def on_start(self):
        if not hasattr(self.environment, 'test_data') or self.environment.test_data is None:
            self.stop(True)

    # TOURNAMENT
    @task(2)
    def task_create_tournament(self):
        data = self.environment.test_data
        user_id = QuizzesInteractionUtils.create_and_activate_user(self.client)
        if user_id:
            QuizzesInteractionUtils.enroll_and_create_tournament(
                self.client, data["execution_id"], user_id, data["topic_id"])

    @task(2)
    def task_find_tournament(self):
        # We use a dummy ID if no tournament created yet, or a random one
        t_id = "non-existent-id"
        self.client.get(f"/tournaments/{t_id}", name="FindTournament")

    # EXECUTION
    @task(2)
    def task_enroll_student(self):
        data = self.environment.test_data
        u_suffix = uuid.uuid4().hex[:6]
        # We need a user first (triggers UserService)
        res = self.client.post(
            "/users/create", json={"name": f"U_{u_suffix}", "username": f"u_{u_suffix}", "role": "STUDENT"})
        if res.status_code == 200:
            user_id = res.json()["aggregateId"]
            self.client.post(
                f"/executions/{data['execution_id']}/students/add?userAggregateId={user_id}", name="EnrollStudent")

    @task(2)
    def task_get_execution_by_id(self):
        data = self.environment.test_data
        self.client.get(
            f"/executions/{data['execution_id']}", name="GetExecutionById")

    # USER
    @task(2)
    def task_create_user_only(self):
        u_suffix = uuid.uuid4().hex[:6]
        self.client.post("/users/create", json={"name": f"U_{u_suffix}",
                         "username": f"u_{u_suffix}", "role": "STUDENT"}, name="CreateUserOnly")

    @task(2)
    def task_activate_user_random(self):
        # Might fail but still hits capacity check
        dummy_id = "1"
        self.client.post(f"/users/{dummy_id}/activate",
                         name="ActivateUserOnly")
