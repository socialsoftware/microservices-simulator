package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;

import java.util.Set;

@Entity
public class SagaTournament extends Tournament implements SagaAggregate {
    @OneToOne
    private SagaState sagaState;
    
    public SagaTournament() {
        super();
    }

    public SagaTournament(Integer aggregateId, TournamentDto tournamentDto, UserDto creatorDto,
                            CourseExecutionDto courseExecutionDto, Set<TopicDto> topicDtos, QuizDto quizDto) {
        super(aggregateId, tournamentDto, creatorDto, courseExecutionDto, topicDtos, quizDto);
    }

    /* used to update the tournament by creating new versions */
    public SagaTournament(SagaTournament other) {
        super(other);
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
