package pt.ulisboa.tecnico.socialsoftware.showcase.shared.sagaStates;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum ReservationSagaStates implements SagaState {
    IN_BOOK_ROOM {
        @Override
        public String getStateName() {
            return "IN_BOOK_ROOM";
        }
    },
    IN_CANCEL_BOOKING {
        @Override
        public String getStateName() {
            return "IN_CANCEL_BOOKING";
        }
    }
}
