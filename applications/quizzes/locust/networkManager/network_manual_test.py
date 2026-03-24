from locust import HttpUser, task, between, events
import logging
from network_utils import SimulatorAdminUtils, QuizzesInteractionUtils, GATEWAY


class NetworkStochasticUser(HttpUser):
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        logging.info("############# TEST START #############")
        try:
            SimulatorAdminUtils.start_and_load()
            data = QuizzesInteractionUtils.create_base_data()
            QuizzesInteractionUtils.create_questions(
                data["course_id"], data["topic_data"])
            environment.test_data = data
            logging.info(
                f"### Setup complete | Execution: {data['execution_id']}")
        except Exception as e:
            logging.error(f"### Setup failed: {e}")
            environment.test_data = None

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        SimulatorAdminUtils.stop_and_cleanup()
        logging.info("############# TEST END #############")

    def on_start(self):
        if not hasattr(self.environment, 'test_data') or self.environment.test_data is None:
            self.user.stop(True)

    @task
    def complex_tournament_flow(self):
        data = self.environment.test_data
        if not data:
            return

        user_id = QuizzesInteractionUtils.create_and_activate_user(self.client)
        if user_id:
            t_res = QuizzesInteractionUtils.enroll_and_create_tournament(
                self.client, data["execution_id"], user_id, data["topic_id"])
            if t_res.status_code == 200:
                t_id = t_res.json()["aggregateId"]
                QuizzesInteractionUtils.find_tournament(self.client, t_id)
