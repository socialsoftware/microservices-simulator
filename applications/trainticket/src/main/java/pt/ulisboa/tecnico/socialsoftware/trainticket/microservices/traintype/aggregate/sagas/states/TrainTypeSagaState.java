package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

public enum TrainTypeSagaState implements SagaAggregate.SagaState {
    READ_TRAIN_TYPE {
        @Override
        public String getStateName() {
            return "READ_TRAIN_TYPE";
        }
    }
}
