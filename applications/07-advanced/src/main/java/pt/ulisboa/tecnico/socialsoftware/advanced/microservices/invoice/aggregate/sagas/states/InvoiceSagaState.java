package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum InvoiceSagaState implements SagaState {
    CREATE_INVOICE {
        @Override
        public String getStateName() {
            return "CREATE_INVOICE";
        }
    },
    DELETE_INVOICE {
        @Override
        public String getStateName() {
            return "DELETE_INVOICE";
        }
    },
    READ_INVOICE {
        @Override
        public String getStateName() {
            return "READ_INVOICE";
        }
    },
    UPDATE_INVOICE {
        @Override
        public String getStateName() {
            return "UPDATE_INVOICE";
        }
    }
}