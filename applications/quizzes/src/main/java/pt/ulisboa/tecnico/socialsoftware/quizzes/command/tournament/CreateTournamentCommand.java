package pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import java.util.Set;

public class CreateTournamentCommand extends Command {
    private TournamentDto tournamentDto;
    private UserDto creatorDto;
    private CourseExecutionDto courseExecutionDto;
    private Set<TopicDto> topicDtos;
    private QuizDto quizDto;

    public CreateTournamentCommand(UnitOfWork unitOfWork, String serviceName, TournamentDto tournamentDto, UserDto creatorDto, CourseExecutionDto courseExecutionDto, Set<TopicDto> topicDtos, QuizDto quizDto) {
        super(unitOfWork, serviceName, null);
        this.tournamentDto = tournamentDto;
        this.creatorDto = creatorDto;
        this.courseExecutionDto = courseExecutionDto;
        this.topicDtos = topicDtos;
        this.quizDto = quizDto;
    }

    public TournamentDto getTournamentDto() { return tournamentDto; }
    public UserDto getCreatorDto() { return creatorDto; }
    public CourseExecutionDto getCourseExecutionDto() { return courseExecutionDto; }
    public Set<TopicDto> getTopicDtos() { return topicDtos; }
    public QuizDto getQuizDto() { return quizDto; }
}
