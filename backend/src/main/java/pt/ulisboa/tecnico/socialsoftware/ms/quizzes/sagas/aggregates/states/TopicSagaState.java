package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum TopicSagaState implements SagaState {
    CREATE_TOURNAMENT_READ_TOPIC {
        @Override
        public String getStateName() {
            return "CREATE_TOURNAMENT_READ_TOPIC";
        }
    },
    DELETE_TOPIC_READ_TOPIC {
        @Override
        public String getStateName() {
            return "DELETE_TOPIC_READ_TOPIC";
        }
    },
    UPDATE_TOPIC_READ_TOPIC {
        @Override
        public String getStateName() {
            return "UPDATE_TOPIC_READ_TOPIC";
        }
    },
    UPDATE_TOURNAMENT_READ_TOPIC {
        @Override
        public String getStateName() {
            return "UPDATE_TOURNAMENT_READ_TOPIC";
        }
    }
}
