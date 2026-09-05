package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

public enum OrderSagaState implements SagaAggregate.SagaState {
    IN_PAY_ORDER {
        @Override
        public String getStateName() {
            return "IN_PAY_ORDER";
        }
    },
    IN_COLLECT_TICKET {
        @Override
        public String getStateName() {
            return "IN_COLLECT_TICKET";
        }
    },
    IN_USE_TICKET {
        @Override
        public String getStateName() {
            return "IN_USE_TICKET";
        }
    },
    IN_CANCEL_ORDER {
        @Override
        public String getStateName() {
            return "IN_CANCEL_ORDER";
        }
    },
    IN_DELETE_ORDER {
        @Override
        public String getStateName() {
            return "IN_DELETE_ORDER";
        }
    }
}
