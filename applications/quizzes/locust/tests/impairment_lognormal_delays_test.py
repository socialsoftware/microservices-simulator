from locust import HttpUser, task, between, events
import logging
from utils.app_utils import *
from utils.test_utils import *

intraservice_mean = 3
intranode_mean = 55
internode_mean = 1100

config = {
    "Placement": {
        "nodes": [
            {"name": "Node1", "microservices": ["user", "execution"]},
            {"name": "Node2", "microservices": ["tournament", "quiz"]}
        ]},
    "Delays": {
        "intraservice": {"exp": [1.0, 0.1]},
        "intranode":    {"exp": [4.0, 0.1]},
        "internode":    {"exp": [7.0, 0.1]}
    }
}


class LogNormalDelaysTester(HttpUser):
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        try:
            AppUtils.start()
            AppUtils.inject_configuration(config)
            environment.test_data = AppUtils.create_base_data()
            TestUtils.info("Setup complete!")
        except Exception as e:
            TestUtils.fail(f"Setup Failed: {e}")
            environment.test_data = None

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        try:
            report = AppUtils.get_impairment_report()
            AppUtils.stop()

            TestUtils.info("====== RESULTS ======")

            # Verify CreateUser
            delays = TestUtils.get_delays_from_func(report, "createUser")
            if not delays:
                TestUtils.fail(
                    "No delays identified for functionality createUser")
            TestUtils.assert_values_statistical_range(
                delays.get("CreateUserCommand"), intraservice_mean)

            # Verify ActivateUser
            delays = TestUtils.get_delays_from_func(report, "activateUser")
            if not delays:
                TestUtils.fail(
                    "No delays identified for functionality activateUser")
            TestUtils.assert_values_statistical_range(
                delays.get("GetUserByIdCommand"), intraservice_mean)
            TestUtils.assert_values_statistical_range(
                delays.get("ActivateUserCommand"), intraservice_mean)

            # Verify EnrollStudent
            delays = TestUtils.get_delays_from_func(report, "addStudent")
            if not delays:
                TestUtils.fail(
                    "No delays identified for functionality addStudent")
            TestUtils.assert_values_statistical_range(
                delays.get("GetUserByIdCommand"), intranode_mean)
            TestUtils.assert_values_statistical_range(
                delays.get("EnrollStudentCommand"), intraservice_mean)

            # Verify CreateTournament
            delays = TestUtils.get_delays_from_func(report, "createTournament")
            if not delays:
                TestUtils.fail(
                    "No delays identified for functionality createTournament")
            TestUtils.assert_values_statistical_range(  # Tournament Service
                delays.get("CreateTournamentCommand"), intraservice_mean)
            TestUtils.assert_values_statistical_range(  # Quizz Service
                delays.get("GenerateQuizCommand"), intranode_mean)
            TestUtils.assert_values_statistical_range(  # Execution Service
                delays.get("GetCourseExecutionByIdCommand"), internode_mean)
            TestUtils.assert_values_statistical_range(  # Execution Service
                delays.get("GetStudentByExecutionIdAndUserIdCommand"), internode_mean)
            TestUtils.assert_values_statistical_range(  # Execution Service
                delays.get("GetCourseExecutionByIdCommand"), internode_mean)

        except Exception as e:
            TestUtils.fail(f"Validation Error: {e}")

    @task
    def enroll_student_and_create_tournament(self):
        data = self.environment.test_data
        if not data:
            return

        user_id = AppUtils.create_and_activate_user(client=self.client)

        if not user_id:
            return

        tournament = AppUtils.enroll_and_create_tournament(
            exec_id=data["execution_id"],
            user_id=user_id,
            topic_id=data["topic_id"],
            client=self.client
        )

        if not tournament:
            return
