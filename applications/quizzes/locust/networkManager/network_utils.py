import logging
import requests
import datetime
import uuid
import re
import json

GATEWAY = "http://localhost:8080"


class NetworkValidatorUtils:
    """Auxiliar class: Parses BehaviourReport and validates delays"""

    @staticmethod
    def get_report():
        try:
            return requests.get(f"{GATEWAY}/behaviour/report").text
        except Exception as e:
            logging.error(f"Failed to fetch report: {e}")
            return ""

    @staticmethod
    def get_delays(report, step_name):
        """Extracts delayBefore values for a specific step in the report"""
        # Java map format: stepName=[fault, delayBefore, delayAfter]
        pattern = rf"{step_name}=\[(\d+),\s*(\d+),\s*(\d+)\]"
        matches = re.findall(pattern, report)
        return [int(m[1]) for m in matches]

    @staticmethod
    def assert_range(name, values, min_val, max_val):
        """Asserts if delays fall within expected bounds"""
        if not values:
            logging.warning(f"### CHECK SKIPPED: No data for {name}")
            return
        for v in values:
            if min_val <= v <= max_val:
                logging.info(
                    f"### >> PASS: {name} ({v}ms is within {min_val}-{max_val}ms)")
            else:
                logging.error(
                    f"### >> FAIL: {name} violation! Found {v}ms, expected {min_val}-{max_val}ms")

    @staticmethod
    def assert_statistical_range(name, values, expected_mean):
        """Asserts delays against a statistical mean (Log-Normal)"""
        if not values:
            logging.warning(f"### CHECK SKIPPED: No data for {name}")
            return
        lower_bound, upper_bound = expected_mean * 0.5, expected_mean * 2.0
        for v in values:
            if lower_bound <= v <= upper_bound:
                logging.info(
                    f"### >> PASS: {name} ({v}ms within statistical bounds {int(lower_bound)}-{int(upper_bound)}ms)")
            else:
                logging.error(
                    f"### >> FAIL: {name} violation! Found {v}ms, expected ~{int(expected_mean)}ms")


class SimulatorAdminUtils:
    """Auxiliar class: Manages simulator's state"""

    @staticmethod
    def start_stochastic(config=None):
        """Starts the simulador and loads behaviour in stochastic mode"""
        requests.get(f"{GATEWAY}/behaviour/clean").raise_for_status()
        requests.post(
            f"{GATEWAY}/behaviour/mode?mode=STOCHASTIC").raise_for_status()
        requests.post(
            f"{GATEWAY}/behaviour/load?dir=locust").raise_for_status()
        if config:
            requests.post(f"{GATEWAY}/behaviour/placement",
                          json=config).raise_for_status()
        requests.get(f"{GATEWAY}/traces/start").raise_for_status()
        requests.get(f"{GATEWAY}/scheduler/start").raise_for_status()

    @staticmethod
    def stop_and_cleanup():
        """Stops the simulator"""
        requests.get(f"{GATEWAY}/scheduler/stop").raise_for_status()
        requests.get(f"{GATEWAY}/traces/end").raise_for_status()
        requests.get(f"{GATEWAY}/traces/flush").raise_for_status()
        requests.post(
            f"{GATEWAY}/behaviour/mode?mode=DETERMINISTIC").raise_for_status()


class QuizzesInteractionUtils:
    """Auxiliar class: Interacts with quizzes tutor via flow and setup tasks"""

    @staticmethod
    def create_base_data():
        """
        Creates an execution and topic.
        Returns:
            execution_id,
            course_id,
            topic_id,
            topic_data,
            suffix
        """
        now = datetime.datetime.now(datetime.timezone.utc)
        suffix = int(now.timestamp())

        # 1. Execution
        r = requests.post(f"{GATEWAY}/executions/create", json={
            "name": f"Test_{suffix}", "type": "TECNICO", "acronym": f"T_{suffix}",
            "academicTerm": "2023/2024", "endDate": (now + datetime.timedelta(days=365)).isoformat().replace("+00:00", "Z")
        })
        r.raise_for_status()
        exec_data = r.json()

        # 2. Topic
        r = requests.post(
            f"{GATEWAY}/courses/{exec_data['courseAggregateId']}/create", json={"name": f"Topic_{suffix}"})
        r.raise_for_status()
        topic_data = r.json()

        return {
            "execution_id": exec_data["aggregateId"],
            "course_id": exec_data["courseAggregateId"],
            "topic_id": topic_data["aggregateId"],
            "topic_data": topic_data,
            "suffix": suffix
        }

    @staticmethod
    def create_questions(course_id, topic_data, count=3):
        """Creates 3 questions"""
        for i in range(count):
            requests.post(f"{GATEWAY}/courses/{course_id}/questions/create", json={
                "title": f"Q_{i}", "content": "Content",
                "optionDtos": [{"sequence": 1, "correct": True, "content": "C"}, {"sequence": 2, "correct": False, "content": "W"}],
                "topicDto": [topic_data]
            }).raise_for_status()

    @staticmethod
    def create_and_activate_user(client):
        """
        Creates a user with role STUDENT and activates it.
        Returns:
            user_id
        """
        u_suffix = uuid.uuid4().hex[:6]
        res = client.post("/users/create", json={
            "name": f"U_{u_suffix}", "username": f"u_{u_suffix}", "role": "STUDENT"
        })
        if res.status_code == 200:
            user_id = res.json()["aggregateId"]
            client.post(f"/users/{user_id}/activate")
            return user_id
        return None

    @staticmethod
    def enroll_and_create_tournament(client, exec_id, user_id, topic_id):
        """
        Enrolls a user into a course execution and creates a tournament.
        Returns:
            Server-Response
        """
        client.post(
            f"/executions/{exec_id}/students/add?userAggregateId={user_id}")
        now = datetime.datetime.now(datetime.timezone.utc)
        payload = {
            "startTime": (now + datetime.timedelta(hours=1)).isoformat(timespec='milliseconds').replace("+00:00", "Z"),
            "endTime": (now + datetime.timedelta(hours=2)).isoformat(timespec='milliseconds').replace("+00:00", "Z"),
            "numberOfQuestions": 2
        }
        return client.post(f"/executions/{exec_id}/tournaments/create", json=payload, params={"userId": user_id, "topicsId": [topic_id]})

    @staticmethod
    def find_tournament(client, tournament_id):
        """
        Executes FindTournament functionality
        Returns:
            Server-Response
        """
        return client.get(f"/tournaments/{tournament_id}", name="05_FindTournament")
