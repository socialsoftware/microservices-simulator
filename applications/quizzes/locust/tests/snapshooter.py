import logging
import json
from locust import HttpUser, task, events
from utils.app_utils import *
from utils.test_utils import *

# docker exec postgres pg_dump -U postgres msdb --data-only --inserts > performance-analysis/src/initial_config/baseline_data.sql
# remove "SELECT pg_catalog.set_config"
# remove restrict and unrestrict

class ComplexFindUpdateTester(HttpUser):
    host = GATEWAY

    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        AppUtils.start()

        try:
            for _ in range(50):
                data = AppUtils.create_base_data()
                if not data:
                    continue

                owner_id = AppUtils.create_and_activate_user()
                if not owner_id:
                    continue

                res_data = AppUtils.enroll_and_create_tournament(
                    data["execution_id"], owner_id, data["topic_id"])

                if not res_data:
                    continue

                user_id = AppUtils.create_and_activate_user()

                info = {
                    "course_id": data["course_id"],
                    "topic_id": data["topic_id"],
                    "execution_id": data["execution_id"],
                    "owner_id": owner_id,
                    "tournament_id": res_data["aggregateId"],
                    "user_id": user_id
                }
                print(json.dumps(info)+",")
        except Exception as e:
            logging.error(f"Setup Failed: {e}")

    @task
    def noop(self):
        pass
