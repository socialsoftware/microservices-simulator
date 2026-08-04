package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum TrainSagaState implements SagaState {
    DELETE_TRAIN {
        @Override
        public String getStateName() {
            return "DELETE_TRAIN";
        }
    },
    READ_TRAIN {
        @Override
        public String getStateName() {
            return "READ_TRAIN";
        }
    },
    UPDATE_TRAIN {
        @Override
        public String getStateName() {
            return "UPDATE_TRAIN";
        }
    }
}