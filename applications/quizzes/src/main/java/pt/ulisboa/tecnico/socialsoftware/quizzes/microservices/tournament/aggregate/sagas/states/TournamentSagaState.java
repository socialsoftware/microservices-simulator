package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum TournamentSagaState implements SagaState {
    // TODO change to more generic states
    IN_UPDATE_TOURNAMENT {
        @Override
        public String getStateName() {
            return "IN_UPDATE_TOURNAMENT";
        }
    },
    IN_DELETE_TOURNAMENT {
        @Override
        public String getStateName() {
            return "IN_DELETE_TOURNAMENT";
        }
    },
    IN_ADD_PARTICIPANT {
        @Override
        public String getStateName() {
            return "IN_ADD_PARTICIPANT";
        }
    },
    READ_TOURNAMENT {
        @Override
        public String getStateName() {
            return "READ_TOURNAMENT";
        }
    },
    READ_UPDATED_TOPICS {
        @Override
        public String getStateName() {
            return "READ_UPDATED_TOPICS";
        }
    }
}
