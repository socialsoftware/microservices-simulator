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
        # Format: [msName][operationName] ACTION: requestId | Active: [id1, id2] | Waiting: [] | Available: X
        pattern = rf"\[{ms_name.lower()}\]\[.*?\] .* \| Active: \[(.*?)\]"
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
        requests.get(f"{GATEWAY}/capacity/reset").raise_for_status()
        requests.get(f"{GATEWAY}/behaviour/reset").raise_for_status()
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
    def _perform_request(client, method, path, **kwargs):
        """Helper to handle both requests module and Locust client"""
        is_requests = (getattr(client, "__name__", "") == "requests")
        url = f"{GATEWAY}{path}" if is_requests else path

        res = getattr(client, method.lower())(url, **kwargs)

        if is_requests:
            res.raise_for_status()
        return res

    # *Isolated Functionality Methods*

    @staticmethod
    def create_execution(client, name, acronym, academic_term, end_date):
        payload = {
            "name": name, "type": "TECNICO", "acronym": acronym,
            "academicTerm": academic_term, "endDate": end_date
        }
        return QuizzesInteractionUtils._perform_request(client, "POST", "/executions/create", json=payload)

    @staticmethod
    def create_topic(client, course_id, name):
        return QuizzesInteractionUtils._perform_request(client, "POST", f"/courses/{course_id}/create", json={"name": name})

    @staticmethod
    def create_question(client, course_id, title, content, options, topics):
        payload = {
            "title": title, "content": "Content",
            "optionDtos": options,
            "topicDto": topics
        }
        return QuizzesInteractionUtils._perform_request(client, "POST", f"/courses/{course_id}/questions/create", json=payload)

    @staticmethod
    def create_user(client, name=None, username=None, role="STUDENT"):
        u_suffix = uuid.uuid4().hex[:6]
        if not name:
            name = f"U_{u_suffix}"
        
        if not username:
            username = f"u_{u_suffix}"
        
        payload = {"name": name, "username": username, "role": role}
        return QuizzesInteractionUtils._perform_request(client, "POST", "/users/create", json=payload)

    @staticmethod
    def activate_user(client, user_id):
        return QuizzesInteractionUtils._perform_request(client, "POST", f"/users/{user_id}/activate")

    @staticmethod
    def enroll_student(client, execution_id, user_id):
        path = f"/executions/{execution_id}/students/add?userAggregateId={user_id}"
        return QuizzesInteractionUtils._perform_request(client, "POST", path)

    @staticmethod
    def join_tournament(client, tournament_id, execution_id, user_id):
        path = f"/tournaments/{tournament_id}/join"
        params = {
            "executionAggregateId": execution_id,
            "userAggregateId": user_id
        }
        return QuizzesInteractionUtils._perform_request(client, "POST", path, params=params)

    @staticmethod
    def create_tournament(client, execution_id, user_id, topic_ids, num_questions=2):
        now = datetime.datetime.now(datetime.timezone.utc)
        start_time = (now + datetime.timedelta(hours=1)
                      ).isoformat(timespec='milliseconds').replace("+00:00", "Z")
        end_time = (now + datetime.timedelta(hours=2)
                    ).isoformat(timespec='milliseconds').replace("+00:00", "Z")

        payload = {
            "startTime": start_time,
            "endTime": end_time,
            "numberOfQuestions": num_questions
        }
        params = {"userId": user_id, "topicsId": topic_ids}
        return QuizzesInteractionUtils._perform_request(client, "POST", f"/executions/{execution_id}/tournaments/create", json=payload, params=params)

    @staticmethod
    def find_tournament(client, tournament_id):
        path = f"/tournaments/{tournament_id}"
        return QuizzesInteractionUtils._perform_request(client, "GET", path)

    @staticmethod
    def get_execution(client, execution_id):
        return QuizzesInteractionUtils._perform_request(client, "GET", f"/executions/{execution_id}")

    @staticmethod
    def update_tournament(client, tournament_id, topic_ids, num_questions=3):
        """Updates an existing tournament using the correct PostMapping and body structure."""
        now = datetime.datetime.now(datetime.timezone.utc)
        payload = {
            "aggregateId": tournament_id,
            "startTime": (now + datetime.timedelta(hours=1)).isoformat(timespec='milliseconds').replace("+00:00", "Z"),
            "endTime": (now + datetime.timedelta(hours=2)).isoformat(timespec='milliseconds').replace("+00:00", "Z"),
            "numberOfQuestions": num_questions
        }
        return QuizzesInteractionUtils._perform_request(client, "POST", "/tournaments/update", json=payload, params={"topicsId": topic_ids})

    @staticmethod
    def solve_quiz(client, tournament_id, user_id):
        return QuizzesInteractionUtils._perform_request(client, "POST", f"/tournaments/{tournament_id}/solveQuiz", params={"userAggregateId": user_id})

    # *Complex Functionality Methods*

    @staticmethod
    def create_and_activate_user(client=requests):
        """Creates a new user and sets its status to Active"""
        res = QuizzesInteractionUtils.create_user(client)
        if res.status_code == 200:
            user_id = res.json()["aggregateId"]
            QuizzesInteractionUtils.activate_user(client, user_id)
            return user_id
        return None

    @staticmethod
    def create_questions(course_id, topic_data, count=3, client=requests):
        """Helper to create multiple questions"""
        for i in range(count):
            options = [{"sequence": 1, "correct": True, "content": "C"}, {
                "sequence": 2, "correct": False, "content": "W"}]
            QuizzesInteractionUtils.create_question(
                client, course_id, f"Q_{i}_{uuid.uuid4().hex[:4]}", "Content", options, [topic_data])

    @staticmethod
    def enroll_and_create_tournament(client, exec_id, user_id, topic_id):
        """Enrolls a student and creates a tournament 1 hour in the future."""
        QuizzesInteractionUtils.enroll_student(client, exec_id, user_id)
        return QuizzesInteractionUtils.create_tournament(client, exec_id, user_id, [topic_id])

    @staticmethod
    def create_base_data(client=requests):
        """Creates an execution, topic, and initial questions."""
        now = datetime.datetime.now(datetime.timezone.utc)
        suffix = int(now.timestamp())
        end_date = (now + datetime.timedelta(days=365)
                    ).isoformat().replace("+00:00", "Z")

        # 1. Execution
        exec_res = QuizzesInteractionUtils.create_execution(
            client, f"Test_{suffix}", f"T_{suffix}", "2023/2024", end_date)
        exec_data = exec_res.json()

        # 2. Topic
        topic_res = QuizzesInteractionUtils.create_topic(
            client, exec_data['courseAggregateId'], f"Topic_{suffix}")
        topic_data = topic_res.json()

        # 3. Questions (Need at least 2 for the tournament)
        QuizzesInteractionUtils.create_questions(
            exec_data['courseAggregateId'], topic_data, count=5, client=client)

        return {
            "execution_id": exec_data["aggregateId"],
            "course_id": exec_data["courseAggregateId"],
            "topic_id": topic_data["aggregateId"],
            "topic_data": topic_data,
            "suffix": suffix
        }
