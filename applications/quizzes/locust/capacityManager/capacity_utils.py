import logging
import requests
import datetime
import uuid
import re
import json

GATEWAY = "http://localhost:8080"


class CapacityValidatorUtils:
    """Auxiliar class: Parses CapacityReport and validates capacity limits"""

    @staticmethod
    def get_report():
        try:
            return requests.get(f"{GATEWAY}/capacity/report").text
        except Exception as e:
            logging.error(f"Failed to fetch capacity report: {e}")
            return ""

    @staticmethod
    def get_max_concurrent(report, ms_name):
        """Extracts the maximum number of active requests for a microservice from the report"""
        # Format: [msName] ACTION: logId | Active: [id1, id2] | Waiting: [] | Available: X
        pattern = rf"\[{ms_name}\] .* \| Active: \[(.*?)\]"
        matches = re.findall(pattern, report)
        max_active = 0
        for m in matches:
            if not m.strip():
                count = 0
            else:
                count = len(m.split(","))
            if count > max_active:
                max_active = count
        return max_active

    @staticmethod
    def assert_max_capacity(ms_name, report, expected_max):
        """Asserts the number of active requests of a sevice does not surpass the limit"""
        max_active = CapacityValidatorUtils.get_max_concurrent(report, ms_name)
        if max_active <= expected_max:
            logging.info(
                f"### >> PASS: {ms_name} max concurrent requests: {max_active} (expected <= {expected_max})")
        else:
            logging.error(
                f"### >> FAIL: {ms_name} capacity violation! Found {max_active} concurrent requests, expected <= {expected_max}")

    @staticmethod
    def assert_concurrency_range(ms_name, report, min_expected, max_expected):
        """Asserts the number of active requests of a sevice fall within expected bounds"""
        max_active = CapacityValidatorUtils.get_max_concurrent(report, ms_name)
        if min_expected <= max_active <= max_expected:
            logging.info(
                f"### >> PASS: {ms_name} max concurrent requests: {max_active} (within expected {min_expected}-{max_expected})")
        else:
            logging.error(
                f"### >> FAIL: {ms_name} concurrency violation! Found {max_active}, expected between {min_expected} and {max_expected}")


class CapacityAdminUtils:
    """Auxiliar class: Manages simulator's capacity state"""

    @staticmethod
    def start_and_load(config=None):
        """Starts the simulator and loads capacity configuration"""
        requests.get(f"{GATEWAY}/capacity/clean").raise_for_status()
        requests.post(f"{GATEWAY}/capacity/load?dir=locust").raise_for_status()
        if config:
            requests.post(f"{GATEWAY}/capacity/inject",
                          json=config).raise_for_status()
        requests.get(f"{GATEWAY}/traces/start").raise_for_status()
        requests.get(f"{GATEWAY}/scheduler/start").raise_for_status()

    @staticmethod
    def stop_and_cleanup():
        """Stops the simulator"""
        requests.get(f"{GATEWAY}/scheduler/stop").raise_for_status()
        requests.get(f"{GATEWAY}/traces/end").raise_for_status()
        requests.get(f"{GATEWAY}/traces/flush").raise_for_status()


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
    def create_and_activate_user(client):
        u_suffix = uuid.uuid4().hex[:6]
        # Check if client is the requests module or a session/locust client
        # Needed so the method works on both setup and tasks
        is_requests = hasattr(client, 'post') and not hasattr(
            client, 'base_url') and client.__name__ == 'requests'
        url = f"{GATEWAY}/users/create" if is_requests else "/users/create"

        res = client.post(url, json={
            "name": f"U_{u_suffix}", "username": f"u_{u_suffix}", "role": "STUDENT"
        })
        if res.status_code == 200:
            user_id = res.json()["aggregateId"]
            act_url = f"{GATEWAY}/users/{user_id}/activate" if is_requests else f"/users/{user_id}/activate"
            client.post(act_url)
            return user_id
        return None

    @staticmethod
    def create_questions(course_id, topic_data, count=3):
        """Creates questions for the topic"""
        for i in range(count):
            requests.post(f"{GATEWAY}/courses/{course_id}/questions/create", json={
                "title": f"Q_{i}_{uuid.uuid4().hex[:4]}", "content": "Content",
                "optionDtos": [{"sequence": 1, "correct": True, "content": "C"}, {"sequence": 2, "correct": False, "content": "W"}],
                "topicDto": [topic_data]
            }).raise_for_status()

    @staticmethod
    def enroll_and_create_tournament(client, exec_id, user_id, topic_id):
        """
        Enrolls a user into a course execution and creates a tournament.
        Returns:
            Server-Response
        """
        # Check if client is requests
        # Needed so the method works on both setup and tasks
        is_requests = hasattr(client, 'post') and not hasattr(
            client, 'base_url') and getattr(client, '__name__', '') == 'requests'

        enroll_url = f"{GATEWAY}/executions/{exec_id}/students/add?userAggregateId={user_id}" if is_requests else f"/executions/{exec_id}/students/add?userAggregateId={user_id}"
        client.post(enroll_url)

        now = datetime.datetime.now(datetime.timezone.utc)
        payload = {
            "startTime": (now + datetime.timedelta(hours=1)).isoformat(timespec='milliseconds').replace("+00:00", "Z"),
            "endTime": (now + datetime.timedelta(hours=2)).isoformat(timespec='milliseconds').replace("+00:00", "Z"),
            "numberOfQuestions": 2
        }

        create_url = f"{GATEWAY}/executions/{exec_id}/tournaments/create" if is_requests else f"/executions/{exec_id}/tournaments/create"
        return client.post(create_url, json=payload, params={"userId": user_id, "topicsId": [topic_id]})
