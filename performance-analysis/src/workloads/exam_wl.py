from locust import task, between
from workload_class import Workload
from src.simulator_tools.simulator_utils import SimInterface

"""
Rationale: This simulates a massive influx of students taking a final exam simultaneously. Traffic is overwhelmingly focused on the  quiz  and    
answer  microservices. 
Challenge for the RL Agent: Capacity Problem. The agent must realize that quiz and answer are getting hammered. It must drain capacity from  
idle services (like tournament or course ) and allocate it to quiz and answer . If these two services require more capacity than a single  
node can provide, the agent is forced to split them across nodes and accept the network delay penalty to avoid queue starvation.
"""


class ExamWorkload(Workload):

    # ! TODO: this is currently not working

    @task(1)
    def start_exam(self):
        if not self.answer_id:
            ans = SimInterface.start_quiz(
                self.user_id, self.exam_quiz_id, client=self.client)
            if ans and "id" in ans:
                self.answer_id = ans["id"]

    @task(10)
    def answer_exam_question(self):
        # Heavy write contention on Answer service
        if self.answer_id:
            # Simulate answering random questions rapidly
            SimInterface.submit_answer(
                self.answer_id, question_id=1, option_id=2, client=self.client)

    @task(1)
    def conclude_exam(self):
        if self.answer_id:
            SimInterface.conclude_quiz(self.answer_id, client=self.client)
            self.answer_id = None  # Ready for another run (if repeating)
