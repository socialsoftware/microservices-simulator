package pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate;

public enum GenericSagaState implements SagaAggregate.SagaState {
    NOT_IN_SAGA {
        @Override
        public String getStateName() {
            return "NOT_IN_SAGA";
        }
    },
    IN_SAGA {
        @Override
        public String getStateName() {
            return "IN_SAGA";
        }
    }
}
