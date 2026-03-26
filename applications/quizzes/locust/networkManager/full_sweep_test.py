from locust import HttpUser, task, between, events
import logging
import uuid
import datetime
import re
from network_utils import SimulatorAdminUtils, NetworkValidatorUtils, GATEWAY


class FullSweepNetworkUser(HttpUser):
    host = GATEWAY
    wait_time = between(1, 2)

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        logging.info("############# TEST START #############")
        config = {
            "Placement": {
                "nodes": [
                    {"name": "Node1", "microservices": [
                        "user", "execution", "course"]},
                    {"name": "Node2", "microservices": ["tournament", "quiz"]},
                    {"name": "Node3", "microservices": [
                        "question", "topic", "answer"]}
                ]},
            "Delays": {
                "USE_CSV_INJECTION": False,
                "USE_RANDOM_DISTRIBUTIONS": True,
                "intraservice": {"uni": [1, 2]},
                "intranode":    {"uni": [5, 10]},
                "internode":    {"uni": [10, 15]}
            }
        }
        try:
            SimulatorAdminUtils.start_and_load(config)
            logging.info("### Setup Complete ###")
        except Exception as e:
            logging.error(f"### Setup Failed: {e}")

    @staticmethod
    def validate_full_sweep(report):
        if not report or "Functionality" not in report:
            logging.error("### >> FAIL: Report is empty or invalid.")
            return False

        errors = []
        zero_delays = re.findall(r"(\w+)=\[0,\s*0,\s*0\]", report)
        if zero_delays:
            errors.append(f"Steps with zero delay: {set(zero_delays)}")

        non_defined = re.findall(r"Non Defined Steps: \[(.+)\]", report)
        for nd in non_defined:
            if nd.strip():
                errors.append(f"Non Defined Steps detected: [{nd}]")

        if errors:
            for err in errors:
                logging.error(f"### >> FAIL: {err}")
            return False

        logging.info("### >> PASS: All captured steps have injected delays.")
        return True

    @events.test_stop.add_listener
    def on_test_stop(environment, **kwargs):
        try:
            logging.info("### RESULTS ###")
            report = NetworkValidatorUtils.get_report()
            FullSweepNetworkUser.validate_full_sweep(report)
            SimulatorAdminUtils.stop_and_cleanup()

            logging.info("############# TEST END #############")
        except Exception as e:
            logging.error(f"### Final Validation Failed: {e}")

    @task
    def full_system_flow(self):
        now = datetime.datetime.now(datetime.timezone.utc)
        suffix = int(now.timestamp())
        u_suffix = uuid.uuid4().hex[:6]

        # --- USER SERVICE ---
        user_res = self.client.post(
            "/users/create", json={"name": f"U_{u_suffix}", "username": f"u_{u_suffix}", "role": "STUDENT"}, name="U01_CreateUser")
        if user_res.status_code != 200:
            return
        user_id = user_res.json()["aggregateId"]
        self.client.post(f"/users/{user_id}/activate", name="U02_ActivateUser")
        self.client.get("/users/students", name="U03_GetStudents")

        # --- EXECUTION + COURSE SERVICE ---
        exec_res = self.client.post("/executions/create", json={
            "name": f"E_{suffix}", "type": "TECNICO", "acronym": f"E_{suffix}",
            "academicTerm": "2023/2024", "endDate": (now + datetime.timedelta(days=365)).isoformat().replace("+00:00", "Z")
        }, name="E01_CreateExec")
        if exec_res.status_code != 200:
            return
        exec_data = exec_res.json()
        exec_id, course_id = exec_data["aggregateId"], exec_data["courseAggregateId"]
        self.client.post(
            f"/executions/{exec_id}/students/add?userAggregateId={user_id}", name="E02_EnrollStudent")

        # --- TOPIC SERVICE ---
        topic_res = self.client.post(
            f"/courses/{course_id}/create", json={"name": f"T_{suffix}"}, name="T01_CreateTopic")
        if topic_res.status_code != 200:
            return
        topic_data = topic_res.json()
        topic_id = topic_data["aggregateId"]

        # --- QUESTION SERVICE ---
        q_res = self.client.post(f"/courses/{course_id}/questions/create", json={
            "title": f"Q_{suffix}", "content": "C",
            "optionDtos": [{"sequence": 1, "correct": True, "content": "C"}, {"sequence": 2, "correct": False, "content": "W"}],
            "topicDto": [topic_data]
        }, name="Q01_CreateQuestion")
        if q_res.status_code != 200:
            return
        q_data = q_res.json()
        q_id = q_data["aggregateId"]

        # --- QUIZ SERVICE ---
        quiz_res = self.client.post(f"/executions/{exec_id}", json={
            "title": f"Quiz_{suffix}", "availableDate": now.isoformat().replace("+00:00", "Z"),
            "conclusionDate": (now + datetime.timedelta(hours=1)).isoformat().replace("+00:00", "Z"),
            "resultsDate": (now + datetime.timedelta(hours=1)).isoformat().replace("+00:00", "Z"),
            "questionDtos": [q_data]
        }, name="Z01_CreateQuiz")
        if quiz_res.status_code != 200:
            return
        quiz_id = quiz_res.json()["aggregateId"]

        # --- ANSWER SERVICE ---
        self.client.post(f"/quizzes/{quiz_id}/answer", params={"userAggregateId": user_id}, json={
            "sequence": 1, "questionAggregateId": q_id, "timeTaken": 10, "optionKey": 1
        }, name="A01_AnswerQuestion")

        # --- TOURNAMENT SERVICE ---
        t_res = self.client.post(f"/executions/{exec_id}/tournaments/create",
                                 json={"startTime": (now + datetime.timedelta(minutes=5)).isoformat(timespec='milliseconds').replace("+00:00", "Z"),
                                       "endTime": (now + datetime.timedelta(hours=1)).isoformat(timespec='milliseconds').replace("+00:00", "Z"),
                                       "numberOfQuestions": 1},
                                 params={"userId": user_id, "topicsId": [topic_id]}, name="R01_CreateTour"
                                 )
        if t_res.status_code == 200:
            t_id = t_res.json()["aggregateId"]
            self.client.post(f"/tournaments/{t_id}/join", params={
                             "executionAggregateId": exec_id, "userAggregateId": user_id}, name="R03_JoinTour")
            self.client.post(f"/tournaments/{t_id}/solveQuiz", params={
                             "userAggregateId": user_id}, name="R04_SolveQuiz")
