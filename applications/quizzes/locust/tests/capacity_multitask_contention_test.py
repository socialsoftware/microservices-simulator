from locust import HttpUser, task, between, events
from utils.app_utils import *
from utils.test_utils import *

"""
Why those services: Sagas for creating/joining/solving tournaments span tournament, 
execution, quiz, user and (optionally) answer/question services — intentionally overlapping 
the most-active cross-aggregate paths to create contention.
"""

config = {
    "Capacities": {
        "microservices": [
            {
                "name": "tournament",
                "capacity": 6,
                "services": [
                    {"name": "UpdateTournament", "requirement": 3},
                    {"name": "SolveQuiz", "requirement": 3},
                    {"name": "AddParticipant", "requirement": 2},
                    {"name": "GetTournamentById", "requirement": 1}
                ]
            },
            {
                "name": "quiz",
                "capacity": 6,
                "services": [
                    {"name": "UpdateGeneratedQuiz", "requirement": 3},
                    {"name": "StartTournamentQuiz", "requirement": 3},
                    {"name": "GetQuizById", "requirement": 1}
                ]
            },
            {
                "name": "execution",
                "capacity": 4,
                "services": [
                    {"name": "EnrollStudent", "requirement": 2},
                    {"name": "GetStudentByExecutionIdAndUserId", "requirement": 1}
                ]
            },
            {
                "name": "user",
                "capacity": 4,
                "services": [
                    {"name": "CreateUser", "requirement": 2},
                    {"name": "ActivateUser", "requirement": 1},
                    {"name": "GetUserById", "requirement": 1}
                ]
            },
            {
                "name": "question",
                "capacity": 4,
                "services": [
                    {"name": "FindQuestionsByTopicIds", "requirement": 2},
                    {"name": "GetQuestionById", "requirement": 1}
                ]
            },
            {
                "name": "topic",
                "capacity": 3,
                "services": [
                    {"name": "GetTopicById", "requirement": 1}
                ]
            },
            {
                "name": "answer",
                "capacity": 3,
                "services": [
                    {"name": "StartQuiz", "requirement": 2}
                ]
            }
        ]
    }
}


def _get_microservice(config, ms_name):
    for ms in config["Capacities"]["microservices"]:
        if ms["name"] == ms_name:
            return ms
    return None


def _get_min_requirement(config, ms_name):
    ms = _get_microservice(config, ms_name)
    if not ms:
        return 1
    return min(service["requirement"] for service in ms.get("services", []))


class ComplexCapacityContentionTester(HttpUser):
    # ! THIS TEST REQUIRES MORE THAN 1 USER TO BE EXECUTED PROPERLY
    host = GATEWAY
    wait_time = between(1, 3)

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

            for ms_name in ["tournament", "quiz", "execution", "user", "question", "topic", "answer"]:
                min_req = _get_min_requirement(config, ms_name)
                ms = _get_microservice(config, ms_name)
                capacity = ms["capacity"]
                expected_max = int(capacity / min_req)
                TestUtils.assert_max_capacity(ms_name, report, expected_max)

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

    @task(2)
    def task_update_tournament(self):
        AppUtils.update_tournament(
            self.t_id, [self.my_data["topic_id"]], client=self.client)

    @task(1)
    def task_enroll_join_solve(self):
        user_id = AppUtils.create_and_activate_user(client=self.client)

        if not user_id:
            return

        if AppUtils.enroll_student(user_id, self.my_data["execution_id"], client=self.client):
            if AppUtils.join_tournament(self.t_id, self.my_data["execution_id"], user_id, client=self.client):
                AppUtils.solve_quiz(self.t_id, user_id, client=self.client)
