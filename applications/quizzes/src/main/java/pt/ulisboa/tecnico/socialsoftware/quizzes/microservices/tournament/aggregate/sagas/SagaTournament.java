package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.sagas;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.sagas.states.TournamentSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

import java.util.Set;
@Entity
public class SagaTournament extends Tournament implements SagaAggregate {
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private TournamentSagaState sagaState;
    
    public SagaTournament() {
        super();
        this.sagaState = TournamentSagaState.NOT_IN_SAGA;
    }

    public SagaTournament(Integer aggregateId, TournamentDto tournamentDto, UserDto creatorDto,
                            CourseExecutionDto courseExecutionDto, Set<TopicDto> topicDtos, QuizDto quizDto) {
        super(aggregateId, tournamentDto, creatorDto, courseExecutionDto, topicDtos, quizDto);
        this.sagaState = TournamentSagaState.NOT_IN_SAGA;
    }

    /* used to update the tournament by creating new versions */
    public SagaTournament(SagaTournament other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    @Override
    public void setSagaState(SagaState state) {
        this.sagaState = (TournamentSagaState) state;
    }

    @Override
    public TournamentSagaState getSagaState() {
        return this.sagaState;
    }

    @Override
    public TournamentSagaState getNeutralSagaState() {
        return TournamentSagaState.NOT_IN_SAGA;
    }

}
