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
                        "name": "TournamentService",
                        "capacity": 5,
                        "endpoints": [
                            {"name": "CreateTournamentFunctionalitySagas",
                                "requirement": 2},
                            {"name": "FindTournamentFunctionalitySagas",
                                "requirement": 1}
                        ]
                    },
                    {
                        "name": "ExecutionService",
                        "capacity": 5,
                        "endpoints": [
                            {"name": "AddStudentFunctionalitySagas", "requirement": 2}
                        ]
                    }
                ]
            }
        }
        try:
            CapacityAdminUtils.start_and_load(config)
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
            # Ensure they are processed simultaneously (at least 2) but within capacity limits
            CapacityValidatorUtils.assert_concurrency_range(
                "TournamentService", report, 2, 5)
            CapacityValidatorUtils.assert_concurrency_range(
                "ExecutionService", report, 2, 2)
            CapacityAdminUtils.stop_and_cleanup()

            logging.info("############# TEST END #############")
        except Exception as e:
            logging.error(f"### Validation Error: {e}")

    @task
    def execute_capacity_workflow(self):
        data = self.environment.test_data
        if not data:
            return

        user_id = QuizzesInteractionUtils.create_and_activate_user(self.client)
        if user_id:
            # AddStudentFunctionalitySagas (ExecutionService)
            self.client.post(
                f"/executions/{data['execution_id']}/students/add?userAggregateId={user_id}", name="EnrollStudent")

            # CreateTournamentFunctionalitySagas (TournamentService)
            now = datetime.datetime.now(datetime.timezone.utc)
            payload = {
                "startTime": (now + datetime.timedelta(hours=1)).isoformat(timespec='milliseconds').replace("+00:00", "Z"),
                "endTime": (now + datetime.timedelta(hours=2)).isoformat(timespec='milliseconds').replace("+00:00", "Z"),
                "numberOfQuestions": 2
            }
            res = self.client.post(f"/executions/{data['execution_id']}/tournaments/create",
                                   json=payload, params={
                                       "userId": user_id, "topicsId": [data["topic_id"]]},
                                   name="CreateTournament")

            if res.status_code == 200:
                t_id = res.json()["aggregateId"]
                # FindTournamentFunctionalitySagas (TournamentService)
                self.client.get(f"/tournaments/{t_id}", name="FindTournament")
