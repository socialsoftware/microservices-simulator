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
            environment.scenarios = []
            for _ in range(6):
                data = AppUtils.create_base_data()
                if not data:
                    continue

                owner_id = AppUtils.create_and_activate_user()
                if not owner_id:
                    continue

                res_data = AppUtils.enroll_and_create_tournament(
                    data["execution_id"], owner_id, data["topic_id"])

                if not res_data:
                    continue

                environment.scenarios.append({
                    "data": data,
                    "tournament_id": res_data["aggregateId"]
                })

            environment.scenario_index = 0

            if not environment.scenarios:
                raise RuntimeError("No valid scenarios could be prepared")

            AppUtils.start()
            AppUtils.inject_configuration(config)
            TestUtils.info("Setup complete!")
        except Exception as e:
            TestUtils.fail(f"Setup Failed: {e}")
            environment.scenarios = []

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

    def on_start(self):
        if not self.environment.scenarios:
            self.stop(True)
            return

        idx = self.environment.scenario_index % len(self.environment.scenarios)
        self.environment.scenario_index += 1
        scenario = self.environment.scenarios[idx]
        self.my_data = scenario["data"]
        self.t_id = scenario["tournament_id"]

    @task
    def execute_capacity_workflow(self):
        # CreateUser and ActivateUser (User service)
        user_id = AppUtils.create_and_activate_user(client=self.client)

        if not user_id:
            return

        # AddStudentFunctionalitySagas (Execution service)
        if AppUtils.enroll_student(user_id, self.my_data["execution_id"], client=self.client):
            # AddParticipantFunctionalitySagas (Execution + Tournament services)
            if AppUtils.join_tournament(self.t_id, self.my_data["execution_id"], user_id, client=self.client):
                # FindTournamentFunctionalitySagas (Tournament service)
                AppUtils.find_tournament(self.t_id, client=self.client)
