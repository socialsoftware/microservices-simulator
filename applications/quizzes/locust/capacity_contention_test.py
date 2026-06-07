from locust import HttpUser, task, between, events
from utils.app_utils import *
from utils.test_utils import *

tourn_cap = 2
exec_cap = 3
user_cap = 4

config = {
    "Capacities": {
        "microservices": [
            {
                "name": "tournament",
                "capacity": tourn_cap,
                "services": [
                    {"name": "GetTournamentById", "requirement": 1},
                    {"name": "AddParticipant", "requirement": 1}
                ]
            },
            {
                "name": "execution",
                "capacity": exec_cap,
                "services": [
                    {"name": "EnrollStudent", "requirement": 1},
                    {"name": "GetStudentByExecutionIdAndUserId", "requirement": 1}
                ]
            },
            {
                "name": "user",
                "capacity": user_cap,
                "services": [
                    {"name": "CreateUser", "requirement": 1},
                    {"name": "ActivateUser", "requirement": 1},
                    {"name": "GetUserById", "requirement": 1}
                ]
            }
        ]
    }
}


class CapacityContentionTester(HttpUser):
    # ! THIS TEST REQUIRES MULTIPLE USERS TO BE EXECUTED PROPERLY
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        try:
            environment.test_data = AppUtils.create_base_data()
            owner_id = AppUtils.create_and_activate_user()
            if not owner_id:
                raise RuntimeError(
                    "Failed to create owner for shared tournament")

            tournament_data = AppUtils.enroll_and_create_tournament(
                environment.test_data["execution_id"],
                owner_id,
                environment.test_data["topic_id"])

            if not tournament_data:
                raise RuntimeError("Failed to create shared tournament")

            environment.tournament_id = tournament_data["aggregateId"]

            AppUtils.start()
            AppUtils.inject_configuration(config)
            TestUtils.info("Setup complete!")
        except Exception as e:
            TestUtils.fail(f"Setup Failed: {e}")
            environment.test_data = None
            environment.tournament_id = None

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        try:
            report = AppUtils.get_capacity_report()
            AppUtils.stop()

            TestUtils.info("====== RESULTS ======")
            TestUtils.assert_concurrency_range(
                "tournament", report, 1, tourn_cap)
            TestUtils.assert_concurrency_range(
                "execution", report, 1, exec_cap)
            TestUtils.assert_concurrency_range(
                "user", report, 1, user_cap)
        except Exception as e:
            TestUtils.fail(f"Validation Error: {e}")

    @task
    def execute_capacity_workflow(self):
        data = self.environment.test_data
        t_id = getattr(self.environment, "tournament_id", None)

        # CreateUser and ActivateUser (User service)
        user_id = AppUtils.create_and_activate_user(client=self.client)

        if not (data and t_id and user_id):
            return

        # AddStudentFunctionalitySagas (Execution service)
        if AppUtils.enroll_student(user_id, data["execution_id"], client=self.client):
            # AddParticipantFunctionalitySagas (Execution + Tournament services)
            if AppUtils.join_tournament(t_id, data["execution_id"], user_id, client=self.client):
                # FindTournamentFunctionalitySagas (Tournament service)
                AppUtils.find_tournament(t_id, client=self.client)
