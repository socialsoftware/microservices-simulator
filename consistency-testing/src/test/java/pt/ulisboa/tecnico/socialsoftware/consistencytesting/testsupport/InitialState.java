package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testsupport;

import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

public record InitialState(
        UserDto userDto,
        CourseExecutionDto courseExecutionDto,
        TopicDto topicDto,
        QuestionDto questionDto,
        TournamentDto tournamentDto) {
}
