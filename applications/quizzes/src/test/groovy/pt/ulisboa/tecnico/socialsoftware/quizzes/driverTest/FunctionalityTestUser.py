from locust import HttpUser, task, between

class QuizSagaUser(HttpUser):
    wait_time = between(1, 3)  # Simulate wait time between requests
    host = "http://localhost:8080"

    # Setup for the test
    def on_start(self):
        # Hardcoding the setup data as "given"
        self.unit_of_work = {
            "id": "some-unit-of-work-id",  # Replace this with the actual ID you want to use
        }
        self.constructor_args = [
            # The constructor args based on your system
            "some_constructor_arg_1",  # Replace this with actual data
            "some_constructor_arg_2",  # Replace with actual data if needed
            self.unit_of_work["id"]
        ]

    @task
    def execute_functionality(self):
        functionality_name = "pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.user.CreateUserFunctionalitySagas"  # Replace with actual class name
        response = self.client.post(
            f"/functionality/execute/{functionality_name}",
            json=self.constructor_args
        )

        # Check if the response status code is as expected
        if response.status_code == 200:
            print("Functionality executed successfully.")
        else:
            print(f"Failed to execute functionality: {response.status_code}")

"""
CourseExecutionService courseExecutionService, 
SagaUnitOfWorkService unitOfWorkService, 
CourseExecutionDto courseExecutionDto, 
SagaUnitOfWork unitOfWork
"""