package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

public enum PriceConfigSagaState implements SagaAggregate.SagaState {
    READ_PRICE_CONFIG {
        @Override
        public String getStateName() {
            return "READ_PRICE_CONFIG";
        }
    },
    IN_UPDATE_PRICE_CONFIG {
        @Override
        public String getStateName() {
            return "IN_UPDATE_PRICE_CONFIG";
        }
    },
    IN_DELETE_PRICE_CONFIG {
        @Override
        public String getStateName() {
            return "IN_DELETE_PRICE_CONFIG";
        }
    }
}
