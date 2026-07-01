from locust import task, between
import random
from workload_class import Workload
from src.simulator_tools.simulator_utils import SimInterface

"""
Simulates the first week of classes.
Everyone is logging in, checking their schedules, and getting enrolled.
Heavy traffic on user, course , and  execution.

This tests the agent's ability to scale foundational identity and organization services.
Because these services are often referenced by others, optimizing their placement relative to each 
other is critical for peformance.                                                                                                                                                
"""


class StartOfSemesterWorkload(Workload):

    def onboarding_flow(self):
        new_user = SimInterface.create_user(client=self.client)
        if new_user and "aggregateId" in new_user:
            uid = new_user["aggregateId"]

            SimInterface.activate_user(uid, client=self.client)

            # Read Course Info
            SimInterface.get_execution(self.exec_id, client=self.client)

            SimInterface.enroll_student(uid, self.exec_id, client=self.client)

    def heavy_reads(self):
        # Simulating students refreshing their course pages
        SimInterface.get_execution(self.exec_id, client=self.client)

    @task
    def dynamic_router(self):
        tasks = [self.onboarding_flow, self.heavy_reads]
        weights = [
            5 * self.write_weight,
            10 * self.read_weight
        ]
        random.choices(tasks, weights=weights, k=1)[0]()
