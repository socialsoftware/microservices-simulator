package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate;

import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;

public interface TournamentFactory {
    Tournament createTournament(Integer aggregateId, TournamentDto tournamentDto, UserDto creatorDto, CourseExecutionDto courseExecutionDto, Set<TopicDto> topicDtos, QuizDto quizDto);
    Tournament createTournamentFromExisting(Tournament existingTournament);
}
