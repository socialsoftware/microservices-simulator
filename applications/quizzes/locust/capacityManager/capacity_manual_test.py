from locust import HttpUser, task, between, events
import logging
import requests
import datetime

GATEWAY = "http://localhost:8080"

class CapacityManagerUser(HttpUser):
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        host = GATEWAY
        capacity_dir = "locust"
        behaviour_dir = "locust"

        logging.info("### TEST START: Initializing system and data")

        try:
            # Load behaviours and configuration
            requests.get(f"{host}/capacity/reset").raise_for_status()
            requests.post(
                f"{host}/behaviour/load?dir={behaviour_dir}").raise_for_status()
            requests.post(
                f"{host}/capacity/load?dir={capacity_dir}").raise_for_status()
            # Start tracing
            requests.get(f"{host}/traces/start").raise_for_status()
            requests.get(f"{host}/scheduler/start").raise_for_status()

            now = datetime.datetime.now(datetime.timezone.utc)
            suffix = int(now.timestamp())

            # Create course execution
            exec_payload = {
                "name": f"Locust Course {suffix}",
                "type": "TECNICO",
                "acronym": f"LC_{suffix}",
                "academicTerm": "2023/2024",
                "endDate": (now + datetime.timedelta(days=365)).isoformat().replace("+00:00", "Z")
            }

            r = requests.post(f"{host}/executions/create", json=exec_payload)
            r.raise_for_status()
            exec_data = r.json()
            execution_id = exec_data["aggregateId"]
            course_id = exec_data["courseAggregateId"]

            # Create Student
            user_payload = {
                "name": "Locust Student",
                "username": f"student_{suffix}",
                "role": "STUDENT"
            }
            r = requests.post(f"{host}/users/create", json=user_payload)
            r.raise_for_status()
            user_id = r.json()["aggregateId"]
            requests.post(
                f"{host}/users/{user_id}/activate").raise_for_status()
            requests.post(
                f"{host}/executions/{execution_id}/students/add?userAggregateId={user_id}").raise_for_status()

            # Create Topic and Questions
            requests.post(f"{host}/courses/{course_id}/create",
                          json={"name": f"TOPIC_{suffix}"}).raise_for_status()
            # Refresh to get topic
            r = requests.get(f"{host}/executions/{execution_id}")
            # Simplified (normally we get it from create topic)
            topic_id = r.json()["courseAggregateId"]

            # Re-fetch topic_id properly
            r = requests.post(f"{host}/courses/{course_id}/create",
                              json={"name": f"TOPIC_ALT_{suffix}"})
            topic_id = r.json()["aggregateId"]

            for i in range(3):
                question_payload = {
                    "title": f"Q{i}_{suffix}",
                    "content": "Content",
                    "topicDto": [{"aggregateId": topic_id, "courseId": course_id}],
                    "optionDtos": [{"sequence": 1, "correct": True, "content": "C"}, {"sequence": 2, "correct": False, "content": "W"}]
                }
                requests.post(f"{host}/courses/{course_id}/questions/create",
                              json=question_payload).raise_for_status()

            # Create Initial Tournament
            start_time = (now + datetime.timedelta(hours=1)
                          ).isoformat(timespec='milliseconds').replace("+00:00", "Z")
            end_time = (now + datetime.timedelta(hours=2)
                        ).isoformat(timespec='milliseconds').replace("+00:00", "Z")

            tournament_payload = {"startTime": start_time,
                                  "endTime": end_time, "numberOfQuestions": 2}
            r = requests.post(f"{host}/executions/{execution_id}/tournaments/create",
                              json=tournament_payload, params={"userId": user_id, "topicsId": [topic_id]})
            r.raise_for_status()
            tournament_id = r.json()["aggregateId"]

            environment.test_data = {
                "execution_id": execution_id,
                "user_id": user_id,
                "topic_id": topic_id,
                "tournament_id": tournament_id
            }
            logging.info(
                f"### Setup complete. Tournament ID: {tournament_id}")

        except Exception as e:
            logging.error(f"### Setup failed: {e}")
            environment.test_data = None

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        host = GATEWAY
        try:
            requests.get(f"{host}/scheduler/stop")
            requests.get(f"{host}/traces/end")
            requests.get(f"{host}/traces/flush")
        except:
            pass

    def on_start(self):
        if not hasattr(self.environment, 'test_data') or self.environment.test_data is None:
            self.stop(True)

    @task(5)
    def find_tournament(self):
        t_id = self.environment.test_data["tournament_id"]
        self.client.get(f"/tournaments/{t_id}", name="find_tournament")

    @task(3)
    def create_tournament(self):
        data = self.environment.test_data
        now = datetime.datetime.now(datetime.timezone.utc)
        start_time = (now + datetime.timedelta(hours=1)
                      ).isoformat(timespec='milliseconds').replace("+00:00", "Z")
        end_time = (now + datetime.timedelta(hours=2)
                    ).isoformat(timespec='milliseconds').replace("+00:00", "Z")

        payload = {"startTime": start_time,
                   "endTime": end_time, "numberOfQuestions": 2}
        params = {"userId": data["user_id"], "topicsId": [data["topic_id"]]}

        self.client.post(f"/executions/{data['execution_id']}/tournaments/create",
                         json=payload, params=params, name="create_tournament")

    @task(1)
    def check_capacity(self):
        with self.client.get("/capacity/status", catch_response=True, name="check_status") as response:
            if response.status_code == 200:
                data = response.json()
                if any(v < 0 for v in data.values()):
                    response.failure(f"Capacity Violation! {data}")
            else:
                response.failure(f"Status failed: {response.status_code}")
