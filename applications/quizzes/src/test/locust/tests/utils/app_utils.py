import logging
import requests
import datetime
import uuid

GATEWAY = "http://localhost:8080"


class AppUtils:
    @staticmethod
    def _get(query, params=None, client=requests):
        try:
            return client.get(f"{GATEWAY}/{query.lstrip('/')}", params=params)
        except Exception as e:
            logging.error(f"GET Request to {query} failed critically: {e}")
            return None

    @staticmethod
    def _post(query, json=None, params=None, client=requests):
        try:
            return client.post(f"{GATEWAY}/{query.lstrip('/')}", json=json, params=params)
        except Exception as e:
            logging.error(f"POST Request to {query} failed critically: {e}")
            return None

    @staticmethod
    def get_impairment_report():
        r = AppUtils._get("behaviour/report")
        if r is not None and r.status_code == 200:
            return r.text
        logging.error(
            f"Failed to fetch impairment report. Status: {r.status_code if r else 'Network Error'}")
        return ""

    @staticmethod
    def get_capacity_report():
        r = AppUtils._get("capacity/report/read")
        if r is not None and r.status_code == 200:
            return r.text
        logging.error(
            f"Failed to fetch capacity report. Status: {r.status_code if r else 'Network Error'}")
        return ""

    @staticmethod
    def start():
        """Clean logs and start tracing. We ignore returns here as they are background tasks."""
        AppUtils._get("behaviour/clean")
        AppUtils._get("capacity/report/clean")
        AppUtils._get("traces/start")
        AppUtils._get("scheduler/start")

    @staticmethod
    def stop():
        """Stops and flushes tracing"""
        AppUtils._get("scheduler/stop")
        AppUtils._get("traces/end")
        AppUtils._get("traces/flush")

    @staticmethod
    def inject_configuration(config):
        AppUtils._post("behaviour/inject", json=config)
        AppUtils._post("capacity/inject", json=config)

    """
    ===========================
    || Requests
    ===========================
    """

    @staticmethod
    def create_user(name=None, username=None, role="STUDENT", client=requests):
        suffix = uuid.uuid4().hex[:6]
        payload = {
            "name": name or f"N_{suffix}",
            "username": username or f"U_{suffix}",
            "role": role
        }

        r = AppUtils._post("/users/create", json=payload, client=client)
        if r is not None and r.status_code in [200, 201]:
            return r.json()

        logging.error(
            f"Failed to create user: {r.text if r else 'No response'}")
        return None

    def get_user(user_id, client=requests):
        r = AppUtils._get(f"/users/{user_id}", client=client)
        if r is not None and r.status_code == 200:
            return r.json()
        return None

    @staticmethod
    def activate_user(user_id, client=requests):
        r = AppUtils._post(f"/users/{user_id}/activate", client=client)
        if r is not None and r.status_code in [200, 201, 204]:
            return True

        logging.error(
            f"Failed to activate user {user_id}: {r.text if r else 'No response'}")
        return False

    @staticmethod
    def create_execution(name=None, acronym=None, academic_term=None, end_date=None, client=requests):
        suffix = uuid.uuid4().hex[:6]
        now = datetime.datetime.now(datetime.timezone.utc)

        payload = {
            "name": name or f"N_{suffix}",
            "type": "TECNICO",
            "acronym": acronym or f"A_{suffix}",
            "academicTerm": academic_term or "2025/2026",
            "endDate": end_date or (now + datetime.timedelta(days=365)).isoformat().replace("+00:00", "Z")
        }

        r = AppUtils._post("executions/create", json=payload, client=client)
        if r is not None and r.status_code in [200, 201]:
            return r.json()

        logging.error(
            f"Failed to create execution: {r.text if r else 'No response'}")
        return None

    @staticmethod
    def get_execution(execution_id, client=requests):
        r = AppUtils._get(f"/executions/{execution_id}", client=client)
        if r is not None and r.status_code == 200:
            return r.json()
        return None

    @staticmethod
    def enroll_student(user_id, exec_id, client=requests):
        r = AppUtils._post(f"/executions/{exec_id}/students/add",
                           params={"userAggregateId": user_id}, client=client)
        if r is not None and r.status_code in [200, 201, 204]:
            return True

        logging.error(
            f"Failed to enroll student: {r.text if r else 'No response'}")
        return False

    @staticmethod
    def create_topic(course_id, name=None, client=requests):
        name = name or f"T_{uuid.uuid4().hex[:6]}"
        r = AppUtils._post(
            f"courses/{course_id}/create", json={"name": name}, client=client)

        if r is not None and r.status_code in [200, 201]:
            return r.json()

        logging.error(
            f"Failed to create topic: {r.text if r else 'No response'}")
        return None

    @staticmethod
    def create_questions(course_id, topic_data, count=3, client=requests):
        """Creates questions and returns a list of created question data."""
        created_questions = []
        for i in range(count):
            payload = {
                "title": f"Q_{i}_{uuid.uuid4().hex[:4]}",
                "content": "Content",
                "optionDtos": [
                    {"sequence": 1, "correct": True, "content": "C"},
                    {"sequence": 2, "correct": False, "content": "W"}
                ],
                "topicDto": [topic_data]
            }
            r = AppUtils._post(
                f"courses/{course_id}/questions/create", json=payload, client=client)
            if r is None or r.status_code not in [200, 201]:
                logging.error(
                    f"Failed to create question {i}: {r.text if r else 'No response'}")
            else:
                created_questions.append(r.json())

        return created_questions

    @staticmethod
    def create_question(course_id, topic_id, title, content, client=requests):
        """Creates one question on one topic, with a correct option 1 and a wrong option 2."""
        payload = {
            "title": title,
            "content": content,
            "topicDto": [{"aggregateId": topic_id, "courseId": course_id}],
            "optionDtos": [
                {"sequence": 1, "correct": True, "content": "Option content 1"},
                {"sequence": 2, "correct": False, "content": "Option content 2"}
            ]
        }
        r = AppUtils._post(f"courses/{course_id}/questions/create", json=payload, client=client)
        if r is not None and r.status_code in [200, 201]:
            return r.json()

        logging.error(
            f"Failed to create question: {r.text if r else 'No response'}")
        return None

    @staticmethod
    def create_tournament(execution_id, topic_ids, user_id=None, num_questions=2, start_time=None, end_time=None,
                          client=requests):
        if not user_id:
            user_id = AppUtils.create_and_activate_user(client=client)
            if not user_id:
                return None
            AppUtils.enroll_student(user_id, execution_id, client=client)

        now = datetime.datetime.now(datetime.timezone.utc)
        payload = {
            "startTime": start_time or (now + datetime.timedelta(hours=1)).isoformat(timespec='milliseconds').replace("+00:00", "Z"),
            "endTime": end_time or (now + datetime.timedelta(hours=2)).isoformat(timespec='milliseconds').replace("+00:00", "Z"),
            "numberOfQuestions": num_questions
        }

        r = AppUtils._post(f"/executions/{execution_id}/tournaments/create", json=payload, params={
                           "userId": user_id, "topicsId": topic_ids}, client=client)
        if r is not None and r.status_code in [200, 201]:
            return r.json()

        logging.error(
            f"Failed to create tournament: {r.text if r else 'No response'}")
        return None

    @staticmethod
    def find_tournament(tournament_id, client=requests):
        r = AppUtils._get(f"/tournaments/{tournament_id}", client=client)
        if r is not None and r.status_code == 200:
            return r.json()
        return None

    @staticmethod
    def join_tournament(tournament_id, execution_id, user_id, client=requests):
        params = {"executionAggregateId": execution_id,
                  "userAggregateId": user_id}
        r = AppUtils._post(
            f"/tournaments/{tournament_id}/join", params=params, client=client)

        if r is not None and r.status_code in [200, 201]:
            return True

        logging.error(
            f"Failed to join tournament: {r.text if r else 'No response'}")
        return False

    @staticmethod
    def update_tournament(tournament_id, topic_ids, num_questions=3, client=requests):
        now = datetime.datetime.now(datetime.timezone.utc)
        payload = {
            "aggregateId": tournament_id,
            "startTime": (now + datetime.timedelta(hours=1)).isoformat(timespec='milliseconds').replace("+00:00", "Z"),
            "endTime": (now + datetime.timedelta(hours=2)).isoformat(timespec='milliseconds').replace("+00:00", "Z"),
            "numberOfQuestions": num_questions
        }

        r = AppUtils._post("/tournaments/update", json=payload,
                           params={"topicsId": topic_ids}, client=client)
        if r is not None and r.status_code in [200, 201]:
            return r

        logging.error(
            f"Failed to update tournament: {r.text if r else 'No response'}")
        return None

    @staticmethod
    def solve_quiz(tournament_id, user_id, client=requests):
        r = AppUtils._post(f"/tournaments/{tournament_id}/solveQuiz", params={
                           "userAggregateId": user_id}, client=client)
        if r is not None and r.status_code in [200, 201]:
            return r.json()

        logging.error(
            f"Failed to solve quiz: {r.text if r else 'No response'}")
        return None

    @staticmethod
    def get_students(client=requests):
        r = AppUtils._get("/users/students", client=client)
        if r is not None and r.status_code == 200:
            return r.json()
        return None

    @staticmethod
    def create_quiz(execution_id, title=None, available_date=None, conclusion_date=None, results_date=None, questions=None, client=requests):
        suffix = uuid.uuid4().hex[:6]
        now = datetime.datetime.now(datetime.timezone.utc)
        payload = {
            "title": title or f"T_{suffix}",
            "availableDate": available_date or now.isoformat().replace("+00:00", "Z"),
            "conclusionDate": conclusion_date or (now + datetime.timedelta(hours=1)).isoformat().replace("+00:00", "Z"),
            "resultsDate": results_date or (now + datetime.timedelta(hours=1)).isoformat().replace("+00:00", "Z"),
            "questionDtos": questions or []
        }
        r = AppUtils._post(
            f"/executions/{execution_id}", json=payload, client=client)
        if r is not None and r.status_code in [200, 201]:
            return r.json()
        return None

    @staticmethod
    def answer_question(quiz_id, user_id, question_id, sequence=1, time_taken=10, option_key=1, client=requests):
        payload = {
            "sequence": sequence,
            "questionAggregateId": question_id,
            "timeTaken": time_taken,
            "optionKey": option_key
        }
        r = AppUtils._post(f"/quizzes/{quiz_id}/answer", params={
                           "userAggregateId": user_id}, json=payload, client=client)
        if r is not None and r.status_code in [200, 201]:
            return r.json()
        return None

    @staticmethod
    def create_base_data(client=requests):
        """Creates an execution, topic, and questions gracefully."""
        exec_data = AppUtils.create_execution(client=client)
        if not exec_data:
            logging.error("create_base_data aborted: create_execution failed.")
            return None

        course_id = exec_data.get("courseAggregateId")
        topic_data = AppUtils.create_topic(course_id, client=client)
        if not topic_data:
            logging.error("create_base_data aborted: create_topic failed.")
            return None

        AppUtils.create_questions(course_id, topic_data, client=client)

        return {
            "execution_id": exec_data.get("aggregateId"),
            "course_id": course_id,
            "topic_id": topic_data.get("aggregateId"),
            "topic_data": topic_data,
        }

    @staticmethod
    def create_and_activate_user(client=requests):
        """Creates a user and activates it. Returns user_id or None."""
        user_data = AppUtils.create_user(client=client)

        if user_data:
            user_id = user_data.get("aggregateId")
            if user_id and AppUtils.activate_user(user_id, client=client):
                return user_id

        return None

    @staticmethod
    def enroll_and_create_tournament(exec_id, user_id, topic_id, client=requests):
        """Enrolls user and creates a tournament. Returns tournament data or None."""
        if AppUtils.enroll_student(user_id, exec_id, client=client):
            return AppUtils.create_tournament(exec_id, [topic_id], user_id, client=client)

        logging.error(
            "enroll_and_create_tournament aborted: enroll_student failed.")
        return None

    """
    ===========================
    || Functionality tests (scenarios migrated from jmeter/tournament/thesis-cases)
    ===========================
    The scenario steps below return the HTTP response (None if the request could not be sent), so the test can
    check its status code with TestUtils, including the steps that are expected to be rejected.
    """

    @staticmethod
    def create_scenario_data(users=2, topics=2, questions=2, tournament_topics=(1, 2), num_questions=2,
                             client=requests):
        """The data every thesis-case scenario starts from, as the .jmx setUp creates it: a course execution with the
        fixed acronym TESTBLCM; students Student_Name_<i> / Student_Username_<i>, created, activated and enrolled;
        topics TOPIC<i>; question <i> ("question title <i>") on topic <i>; and a tournament created by student 1 on
        `tournament_topics` (1-based), from tomorrow to the day after. Returns the ids or None."""
        execution = AppUtils.create_execution(name="BLCM", acronym="TESTBLCM", academic_term="2022/2023",
                                              end_date="2023-09-20T15:37:58.003198Z", client=client)
        if not execution:
            logging.error("create_scenario_data aborted: create_execution failed.")
            return None
        execution_id, course_id = execution["aggregateId"], execution["courseAggregateId"]

        user_ids = []
        for i in range(1, users + 1):
            user = AppUtils.create_user(name=f"Student_Name_{i}", username=f"Student_Username_{i}", client=client)
            if not user or not AppUtils.activate_user(user["aggregateId"], client=client) \
                    or not AppUtils.enroll_student(user["aggregateId"], execution_id, client=client):
                logging.error(f"create_scenario_data aborted: student {i} could not be created and enrolled.")
                return None
            user_ids.append(user["aggregateId"])

        topic_ids = []
        for i in range(1, topics + 1):
            topic = AppUtils.create_topic(course_id, name=f"TOPIC{i}", client=client)
            if not topic:
                logging.error("create_scenario_data aborted: create_topic failed.")
                return None
            topic_ids.append(topic["aggregateId"])

        for i in range(1, questions + 1):
            if not AppUtils.create_question(course_id, topic_ids[i - 1], f"question title {i}",
                                            f"question content {i}", client=client):
                logging.error("create_scenario_data aborted: create_question failed.")
                return None

        now = datetime.datetime.now(datetime.timezone.utc)
        tournament = AppUtils.create_tournament(
            execution_id, [topic_ids[t - 1] for t in tournament_topics], user_ids[0], num_questions,
            start_time=(now + datetime.timedelta(days=1)).isoformat(timespec='milliseconds').replace("+00:00", "Z"),
            end_time=(now + datetime.timedelta(days=2)).isoformat(timespec='milliseconds').replace("+00:00", "Z"),
            client=client)
        if not tournament:
            logging.error("create_scenario_data aborted: create_tournament failed.")
            return None

        return {
            "execution_id": execution_id,
            "course_id": course_id,
            "user_ids": user_ids,
            "topic_ids": topic_ids,
            "tournament_id": tournament["aggregateId"],
            "quiz_id": tournament["quiz"]["aggregateId"],
        }

    # --- simulator controls -----------------------------------------------------------------------------------------
    @staticmethod
    def stop_event_detection(client=requests):
        return AppUtils._get("scheduler/stop", client=client)

    @staticmethod
    def start_event_detection(client=requests):
        return AppUtils._get("scheduler/start", client=client)

    @staticmethod
    def start_traces(client=requests):
        return AppUtils._get("traces/start", client=client)

    @staticmethod
    def end_traces(client=requests):
        return AppUtils._get("traces/end", client=client)

    @staticmethod
    def flush_traces(client=requests):
        return AppUtils._get("traces/flush", client=client)

    @staticmethod
    def decrement_version(client=requests):
        """Decrements the version counter, so the next operation gets the same version as the previous one and is
        concurrent with it (TCC)."""
        return AppUtils._post("versions/decrement", client=client)

    @staticmethod
    def process_update_name_event(client=requests):
        """Makes the tournament service process the pending update-student-name events now."""
        return AppUtils._post("tournament/process/updateExecutionStudentName", client=client)

    @staticmethod
    def process_anonymize_event(client=requests):
        """Makes the tournament service process the pending anonymize-student events now."""
        return AppUtils._post("tournament/process/anonymize", client=client)

    # --- application requests ---------------------------------------------------------------------------------------
    @staticmethod
    def update_student_name(execution_id, user_id, name, client=requests):
        return AppUtils._post(f"executions/{execution_id}/students/{user_id}/update/name", json={"name": name},
                              client=client)

    @staticmethod
    def anonymize_student(execution_id, user_id, client=requests):
        return AppUtils._post(f"executions/{execution_id}/anonymize", params={"userAggregateId": user_id},
                              client=client)

    @staticmethod
    def add_participant(tournament_id, user_id, execution_id=None, client=requests):
        """Joins the tournament. Without `execution_id` the request misses executionAggregateId (8-8)."""
        params = {"executionAggregateId": execution_id, "userAggregateId": user_id}
        return AppUtils._post(f"tournaments/{tournament_id}/join",
                              params={k: v for k, v in params.items() if v is not None}, client=client)

    @staticmethod
    def update_tournament_topics(tournament_id, topic_ids, num_questions, client=requests):
        """Changes only the topics and number of questions (start and end times are left as they are)."""
        return AppUtils._post("tournaments/update", json={"aggregateId": tournament_id,
                                                          "numberOfQuestions": num_questions},
                              params={"topicsId": topic_ids}, client=client)

    @staticmethod
    def remove_tournament(tournament_id, client=requests):
        return AppUtils._post(f"tournaments/{tournament_id}/remove", client=client)

    @staticmethod
    def deactivate_user(user_id, client=requests):
        return AppUtils._post(f"users/{user_id}/deactivate", client=client)

    @staticmethod
    def delete_user(user_id, client=requests):
        return AppUtils._post(f"users/{user_id}/delete", client=client)

    @staticmethod
    def get_tournament(tournament_id, client=requests):
        return AppUtils._get(f"tournaments/{tournament_id}", client=client)

    @staticmethod
    def get_execution_tournaments(execution_id, client=requests):
        """The course execution's tournaments, without the removed (DELETED) and INACTIVE ones."""
        return AppUtils._get(f"executions/{execution_id}/tournaments", client=client)

    @staticmethod
    def read_user(user_id, client=requests):
        """GET /users/{id} as a response (get_user returns the parsed body, or None)."""
        return AppUtils._get(f"users/{user_id}", client=client)

    @staticmethod
    def get_quiz(quiz_id, client=requests):
        return AppUtils._get(f"quizzes/{quiz_id}", client=client)

    @staticmethod
    def get_course_execution(execution_id, client=requests):
        return AppUtils._get(f"executions/{execution_id}", client=client)

    @staticmethod
    def list_students(client=requests):
        return AppUtils._get("users/students", client=client)
