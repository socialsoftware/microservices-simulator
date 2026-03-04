package pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum TaskSagaState implements SagaState {
    DELETE_TASK {
        @Override
        public String getStateName() {
            return "DELETE_TASK";
        }
    },
    READ_TASK {
        @Override
        public String getStateName() {
            return "READ_TASK";
        }
    },
    UPDATE_TASK {
        @Override
        public String getStateName() {
            return "UPDATE_TASK";
        }
    }
}