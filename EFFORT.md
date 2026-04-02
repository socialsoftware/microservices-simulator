# Developer effort estimation for a new application using the simulator 

## Application

1. Create a `Command` for each method of a service for all the services
2. Create `application.yaml` with:
    - Use the `application.yaml.example` template to fill the values
    - Use the `application-service.yaml.example` template to fill the values for each microservice
3. For each microservice (example: user):
    - Make sure if the UserAggregate has entity parameters, they have `@JsonIgnore` annotation on the getters to avoid serialization issues
    - Create a `UserCommandHandler` with the handling of the user commands to call the service methods
