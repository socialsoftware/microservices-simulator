from locust import HttpUser, task, between, events
import logging
import requests
import datetime
import uuid

GATEWAY = "http://localhost:8080"


class NetworkStochasticUser(HttpUser):
    host = GATEWAY
    wait_time = between(0.1, 0.5)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        host = GATEWAY
        behaviour_dir = "locust"

        logging.info(
            "### TEST START: Initializing Stochastic Mode and Placement")

        try:
            # Load behaviours and change mode
            requests.get(f"{host}/behaviour/clean").raise_for_status()
            requests.post(
                f"{host}/behaviour/mode?mode=STOCHASTIC").raise_for_status()
            requests.post(
                f"{host}/behaviour/load?dir={behaviour_dir}").raise_for_status()
            # Start tracing
            requests.get(f"{host}/traces/start").raise_for_status()
            requests.get(f"{host}/scheduler/start").raise_for_status()

            now = datetime.datetime.now(datetime.timezone.utc)
            suffix = int(now.timestamp())

            # Create course execution
            exec_payload = {
                "name": f"Stochastic Course {suffix}",
                "type": "TECNICO",
                "acronym": f"SC_{suffix}",
                "academicTerm": "2023/2024",
                "endDate": (now + datetime.timedelta(days=365)).isoformat().replace("+00:00", "Z")
            }

            r = requests.post(f"{host}/executions/create", json=exec_payload)
            r.raise_for_status()
            exec_data = r.json()
            execution_id = exec_data["aggregateId"]
            course_id = exec_data["courseAggregateId"]

            # Create Topic
            r = requests.post(
                f"{host}/courses/{course_id}/create", json={"name": f"T_{suffix}"})
            r.raise_for_status()
            topic_data = r.json()
            topic_id = topic_data["aggregateId"]

            # Create Questions
            for i in range(3):
                question_payload = {
                    "title": f"Question {i} {suffix}",
                    "content": f"Content {i} {suffix}",
                    "optionDtos": [
                        {"sequence": 1, "correct": True, "content": "Correct"},
                        {"sequence": 2, "correct": False, "content": "Wrong"}
                    ],
                    "topicDto": [topic_data]
                }
                r = requests.post(
                    f"{host}/courses/{course_id}/questions/create", json=question_payload)
                r.raise_for_status()

            environment.test_data = {
                "execution_id": execution_id,
                "course_id": course_id,
                "topic_id": topic_id,
                "suffix": suffix
            }
            logging.info(f"### Setup complete. Execution: {execution_id}")

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
            requests.post(
                f"{host}/behaviour/mode?mode=DETERMINISTIC").raise_for_status()
        except:
            pass

    def on_start(self):
        if not hasattr(self.environment, 'test_data') or self.environment.test_data is None:
            self.user.stop(True)

    @task(1)
    def complex_tournament_flow(self):
        data = self.environment.test_data
        if not data:
            return

        user_suffix = uuid.uuid4().hex[:6]

        # 1 - Create User
        user_res = self.client.post("/users/create", json={
            "name": f"User {user_suffix}",
            "username": f"user_{user_suffix}",
            "role": "STUDENT"
        }, name="01_CreateUser")
        if user_res.status_code != 200:
            return
        user_id = user_res.json()["aggregateId"]

        # 2 - Activate User
        self.client.post(f"/users/{user_id}/activate", name="02_ActivateUser")

        # 3 - Add Student to Execution
        self.client.post(
            f"/executions/{data['execution_id']}/students/add?userAggregateId={user_id}", name="03_EnrollStudent")

        # 4 - Create Tournament
        now = datetime.datetime.now(datetime.timezone.utc)
        tournament_payload = {
            "startTime": (now + datetime.timedelta(hours=1)).isoformat(timespec='milliseconds').replace("+00:00", "Z"),
            "endTime": (now + datetime.timedelta(hours=2)).isoformat(timespec='milliseconds').replace("+00:00", "Z"),
            "numberOfQuestions": 2
        }
        t_res = self.client.post(
            f"/executions/{data['execution_id']}/tournaments/create",
            json=tournament_payload,
            params={"userId": user_id, "topicsId": [data["topic_id"]]},
            name="04_CreateTournament"
        )

        if t_res.status_code == 200:
            tournament_id = t_res.json()["aggregateId"]
            self.client.get(
                f"/tournaments/{tournament_id}", name="05_FindTournament")
