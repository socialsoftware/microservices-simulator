package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum RoomSagaState implements SagaState {
    CREATE_ROOM {
        @Override
        public String getStateName() {
            return "CREATE_ROOM";
        }
    },
    DELETE_ROOM {
        @Override
        public String getStateName() {
            return "DELETE_ROOM";
        }
    },
    READ_ROOM {
        @Override
        public String getStateName() {
            return "READ_ROOM";
        }
    },
    UPDATE_ROOM {
        @Override
        public String getStateName() {
            return "UPDATE_ROOM";
        }
    }
}