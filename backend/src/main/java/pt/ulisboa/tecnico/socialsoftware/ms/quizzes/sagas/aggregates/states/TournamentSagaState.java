package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum TournamentSagaState implements SagaState {
    // TODO change to more generic states
    ADD_PARTICIPANT_READ_TOURNAMENT {
        @Override
        public String getStateName() {
            return "ADD_PARTICIPANT_READ_TOURNAMENT";
        }
    },
    CANCEL_TOURNAMENT_READ_TOURNAMENT {
        @Override
        public String getStateName() {
            return "CANCEL_TOURNAMENT_READ_TOURNAMENT";
        }
    },
    LEAVE_TOURNAMENT_READ_TOURNAMENT {
        @Override
        public String getStateName() {
            return "LEAVE_TOURNAMENT_READ_TOURNAMENT";
        }
    },
    REMOVE_TOURNAMENT_READ_TOURNAMENT {
        @Override
        public String getStateName() {
            return "REMOVE_TOURNAMENT_READ_TOURNAMENT";
        }
    },
    SOLVE_QUIZ_READ_TOURNAMENT {
        @Override
        public String getStateName() {
            return "SOLVE_QUIZ_READ_TOURNAMENT";
        }
    },
    UPDATE_TOURNAMENT_READ_TOURNAMENT {
        @Override
        public String getStateName() {
            return "UPDATE_TOURNAMENT_READ_TOURNAMENT";
        }
    },
    UPDATE_TOURNAMENT_READ_UPDATED_TOPICS {
        @Override
        public String getStateName() {
            return "UPDATE_TOURNAMENT_READ_UPDATED_TOPICS";
        }
    }
}
