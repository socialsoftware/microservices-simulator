package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate;

import java.util.Set;

import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

@Component
public interface TournamentFactory {
    Tournament createTournament(Integer aggregateId, TournamentDto tournamentDto, UserDto creatorDto, CourseExecutionDto courseExecutionDto, Set<TopicDto> topicDtos, QuizDto quizDto);
    Tournament createTournamentFromExisting(Tournament existingTournament);
    TournamentDto createTournamentDto(Tournament tournament);
}
