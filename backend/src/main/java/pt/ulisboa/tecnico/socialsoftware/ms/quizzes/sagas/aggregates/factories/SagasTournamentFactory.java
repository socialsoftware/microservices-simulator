package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.factories;

import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaTournamentDto;

@Service
@Profile("sagas")
public class SagasTournamentFactory implements TournamentFactory {

    @Override
    public Tournament createTournament(Integer aggregateId, TournamentDto tournamentDto, UserDto creatorDto,
            CourseExecutionDto courseExecutionDto, Set<TopicDto> topicDtos, QuizDto quizDto) {
        return new SagaTournament(aggregateId, tournamentDto, creatorDto, courseExecutionDto, topicDtos, quizDto);
    }

    @Override
    public Tournament createTournamentFromExisting(Tournament existingTournament) {
        return new SagaTournament((SagaTournament) existingTournament);
    }

    @Override
    public TournamentDto createTournamentDto(Tournament tournament) {
        return new SagaTournamentDto(tournament);
    }
}
