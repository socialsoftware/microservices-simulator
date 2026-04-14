package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum LoanSagaState implements SagaState {
    DELETE_LOAN {
        @Override
        public String getStateName() {
            return "DELETE_LOAN";
        }
    },
    READ_LOAN {
        @Override
        public String getStateName() {
            return "READ_LOAN";
        }
    },
    UPDATE_LOAN {
        @Override
        public String getStateName() {
            return "UPDATE_LOAN";
        }
    }
}