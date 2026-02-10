# Developer effort estimation for new microservices simulator in kubernetes

## API gateway

1. Create Admin Controller
2. Create application.yaml with the routes

## Application

1. Create a `Command` for each method of a service for all the services
2. Create `application.yaml` with:
    - for stream communication:
      - all the service function definitions that receive the remote commands
      - all the service function definitions that receive the remote events
      - all the bindings (producers and consumers, response channel, event channel and event subscribers)
3. For each microservice (example: user):
    - Make sure if the UserAggregate has entity parameters, they have `@JsonIgnore` annotation on the getters to avoid serialization issues
    - Create a `UserCommandHandler` with the handling of the user commands to call the service methods, `UserGrpcCommandHandler`, `UserStreamCommandHandler` to receive remote commands depending on the communication technology
    - In the functionalities each step uses `commandGateway` instead of direct service calls
    - Create a `UserEnableDisableEventsController` for testing purposes
    - Create a `UserEventSubscriberService` to receive remote events from publisher services

