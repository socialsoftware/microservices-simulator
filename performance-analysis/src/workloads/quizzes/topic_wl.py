from locust import task
import random
from workload_class import Workload
from src.simulator_tools.simulator_utils import SimInterface


class TopicWorkload(Workload):

    def on_start(self):
        super().on_start()
        self.topic_pool = [self.topic_id]

    def get_all_topics(self):
        SimInterface.get_topics_for_course(
            self.course_id, client=self.client)

    def create_topic(self):
        topic = SimInterface.create_topic(
            self.course_id, client=self.client)
        if topic and "aggregateId" in topic:
            self.topic_pool.append(topic["aggregateId"])

    def update_topic(self):
        topic_id = random.choice(self.topic_pool)
        SimInterface.update_topic(topic_id, client=self.client)

    @task
    def dynamic_router(self):
        tasks = [
            self.get_all_topics,
            self.create_topic,
            self.update_topic
        ]
        weights = [
            10 * self.read_weight,
            45 * self.write_weight,
            45 * self.write_weight
        ]

        random.choices(tasks, weights=weights, k=1)[0]()
