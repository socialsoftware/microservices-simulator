package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

public enum TournamentSagaState implements SagaState {
    IN_ADD_PARTICIPANT {
        @Override
        public String getStateName() {
            return "IN_ADD_PARTICIPANT";
        }
    },
    IN_UPDATE_TOURNAMENT {
        @Override
        public String getStateName() {
            return "IN_UPDATE_TOURNAMENT";
        }
    },
    IN_CANCEL_TOURNAMENT {
        @Override
        public String getStateName() {
            return "IN_CANCEL_TOURNAMENT";
        }
    },
    IN_DELETE_TOURNAMENT {
        @Override
        public String getStateName() {
            return "IN_DELETE_TOURNAMENT";
        }
    },
}
