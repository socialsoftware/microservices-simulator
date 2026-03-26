from locust import HttpUser, task, between, events
import logging
import requests
import datetime
from capacity_utils import CapacityAdminUtils, QuizzesInteractionUtils, CapacityValidatorUtils, GATEWAY


class DefaultCapacityUser(HttpUser):
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        logging.info("############# TEST START #############")
        try:
            CapacityAdminUtils.start_and_load()
            data = QuizzesInteractionUtils.create_base_data()
            QuizzesInteractionUtils.create_questions(
                data["course_id"], data["topic_data"])

            # Setup an initial tournament for find_tournament task
            user_id = QuizzesInteractionUtils.create_and_activate_user(
                requests)
            if user_id:
                res = QuizzesInteractionUtils.enroll_and_create_tournament(
                    requests, data["execution_id"], user_id, data["topic_id"])
                if res.status_code == 200:
                    data["tournament_id"] = res.json()["aggregateId"]

            environment.test_data = data
            logging.info(
                f"### Setup complete. Initial Tournament ID: {data.get('tournament_id')}")
        except Exception as e:
            logging.error(f"### Setup Failed: {e}")
            environment.test_data = None

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        try:
            CapacityAdminUtils.stop_and_cleanup()
            logging.info("############# TEST END #############")
        except Exception as e:
            logging.error(f"### Validation Error: {e}")

    def on_start(self):
        if not hasattr(self.environment, 'test_data') or self.environment.test_data is None:
            self.stop(True)

    @task(5)
    def find_tournament(self):
        data = self.environment.test_data
        if data and "tournament_id" in data:
            self.client.get(
                f"/tournaments/{data['tournament_id']}", name="FindTournament")

    @task(5)
    def create_tournament(self):
        data = self.environment.test_data
        if not data:
            return

        user_id = QuizzesInteractionUtils.create_and_activate_user(self.client)
        if user_id:
            QuizzesInteractionUtils.enroll_and_create_tournament(
                self.client, data["execution_id"], user_id, data["topic_id"])
