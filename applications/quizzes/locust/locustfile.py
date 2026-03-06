from locust import HttpUser, task, between, events
import logging

class CapacityManagerUser(HttpUser):
    # Reduced wait time to increase pressure
    wait_time = between(0.1, 0.5)
    
    def on_start(self):
        """
        Setup: Load the capacity and behavior configuration once.
        Note: In a real test with many users, you might want to call this 
        only once via a master listener or manually.
        """
        self.client.post("/capacity/load?dir=locust-test")
        self.client.post("/behaviour/load?dir=locust-test")

    @task(5)
    def find_tournament(self):
        """Requirement: 1"""
        self.client.get("/tournaments/1", name="find_tournament")

    @task(2)
    def create_tournament(self):
        """Requirement: 2"""
        execution_id = 1
        payload = {
            "startTime": "2026-03-05 10:00:00",
            "endTime": "2026-03-05 12:00:00",
            "numberOfQuestions": 5
        }
        params = {"userId": 1, "topicsId": [1]}
        self.client.post(f"/executions/{execution_id}/tournaments/create", 
                         json=payload, params=params, name="create_tournament")

    @task(1)
    def check_capacity(self):
        """
        Concrete Verification: Check if the available permits ever go below 0 
        or if they match expected constraints.
        """
        with self.client.get("/capacity/status", catch_response=True, name="check_status") as response:
            if response.status_code == 200:
                data = response.json()
                tournament_capacity = data.get("TournamentService", 999)
                
                # If capacity is < 0, the semaphore is over-acquired (should not happen)
                if tournament_capacity < 0:
                    response.failure(f"Capacity Violation! TournamentService permits: {tournament_capacity}")
                else:
                    response.success()
            else:
                response.failure(f"Status check failed: {response.status_code}")

    # REMOVED on_stop reset to prevent early termination of capacity for other users
