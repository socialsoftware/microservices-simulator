package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
@Entity
public class SagaTournament extends Tournament implements SagaAggregate {
    @Column
    private SagaState sagaState;
    
    public SagaTournament() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaTournament(Integer aggregateId, TournamentDto tournamentDto, UserDto creatorDto,
                            CourseExecutionDto courseExecutionDto, Set<TopicDto> topicDtos, QuizDto quizDto) {
        super(aggregateId, tournamentDto, creatorDto, courseExecutionDto, topicDtos, quizDto);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    /* used to update the tournament by creating new versions */
    public SagaTournament(SagaTournament other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    @Override
    public void setSagaState(SagaState state) {
        this.sagaState = state;
    }

    @Override
    public SagaState getSagaState() {
        return this.sagaState;
    }

}
