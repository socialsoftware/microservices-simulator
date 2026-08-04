package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum PriceConfigSagaState implements SagaState {
    DELETE_PRICECONFIG {
        @Override
        public String getStateName() {
            return "DELETE_PRICECONFIG";
        }
    },
    READ_PRICECONFIG {
        @Override
        public String getStateName() {
            return "READ_PRICECONFIG";
        }
    },
    UPDATE_PRICECONFIG {
        @Override
        public String getStateName() {
            return "UPDATE_PRICECONFIG";
        }
    }
}