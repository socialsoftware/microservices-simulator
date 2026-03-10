from locust import HttpUser, task, between, events
import logging
import requests
import datetime
import uuid

GATEWAY = "http://localhost:8080"

class NetworkStochasticUser(HttpUser):
    host = GATEWAY
    wait_time = between(1, 2)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        host = GATEWAY
        logging.info("### TEST START: Initializing Stochastic Mode")

        try:
            # Set mode to STOCHASTIC
            requests.post(f"{host}/behaviour/mode?mode=STOCHASTIC").raise_for_status()
            
            # Start tracing and infrastructure
            requests.get(f"{host}/traces/start").raise_for_status()
            requests.get(f"{host}/scheduler/start").raise_for_status()

            now = datetime.datetime.now(datetime.timezone.utc)
            suffix = int(now.timestamp())

            # 1. Create course execution
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

            # 2. Create Topic
            # Note: Verify if the endpoint is /courses/{id}/topics/create or /courses/{id}/create
            r = requests.post(f"{host}/courses/{course_id}/create", json={"name": f"T_{suffix}"})
            r.raise_for_status()
            topic_id = r.json()["aggregateId"]

            # 3. Create Questions for the topic
            for i in range(2):
                q_payload = {
                    "title": f"Q{i}_{suffix}",
                    "content": "Content",
                    "topicDto": [{"aggregateId": topic_id, "courseId": course_id}],
                    "optionDtos": [{"sequence": 1, "correct": True, "content": "Yes"}, {"sequence": 2, "correct": False, "content": "No"}]
                }
                requests.post(f"{host}/courses/{course_id}/questions/create", json=q_payload).raise_for_status()

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
        except:
            pass

    def on_start(self):
        """ Stop the user if the global test setup failed """
        if not hasattr(self.environment, 'test_data') or self.environment.test_data is None:
            logging.error("Stopping User: Setup data missing")
            self.user.stop(True)

    @task(1)
    def complex_tournament_flow(self):
        """
        Task with various steps to trigger multiple microservice delays:
        User Creation -> Activation -> Execution Enrollment -> Tournament Creation -> Tournament Search
        """
        data = self.environment.test_data
        if not data:
            return

        user_suffix = uuid.uuid4().hex[:6]
        
        # 1. Create User
        user_payload = {
            "name": f"User {user_suffix}",
            "username": f"user_{user_suffix}",
            "role": "STUDENT"
        }
        user_res = self.client.post("/users/create", json=user_payload, name="01_CreateUser")
        if user_res.status_code != 200: return
        user_id = user_res.json()["aggregateId"]

        # 2. Activate User
        self.client.post(f"/users/{user_id}/activate", name="02_ActivateUser")

        # 3. Add Student to Execution
        # Fixed URL: added missing '?' and verified 'add' path
        self.client.post(f"/executions/{data['execution_id']}/students/add?userAggregateId={user_id}", name="03_EnrollStudent")

        # 4. Create Tournament
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
            # 5. Find Tournament
            self.client.get(f"/tournaments/{tournament_id}", name="05_FindTournament")
