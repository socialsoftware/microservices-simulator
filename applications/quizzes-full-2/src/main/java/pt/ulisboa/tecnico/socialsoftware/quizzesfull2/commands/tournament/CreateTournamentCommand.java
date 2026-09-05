package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentTopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto;

import java.time.LocalDateTime;
import java.util.List;

public class CreateTournamentCommand extends Command {
    private ExecutionDto executionDto;
    private UserDto creatorDto;
    private List<TournamentTopicDto> topics;
    private List<QuizQuestionDto> selectedQuestions;
    private Integer quizAggregateId;
    private Long quizVersion;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer numberOfQuestions;

    protected CreateTournamentCommand() {}

    public CreateTournamentCommand(UnitOfWork unitOfWork, String serviceName, ExecutionDto executionDto,
                                   UserDto creatorDto, List<TournamentTopicDto> topics,
                                   List<QuizQuestionDto> selectedQuestions, Integer quizAggregateId,
                                   Long quizVersion, LocalDateTime startTime, LocalDateTime endTime,
                                   Integer numberOfQuestions) {
        super(unitOfWork, serviceName, null);
        this.executionDto = executionDto;
        this.creatorDto = creatorDto;
        this.topics = topics;
        this.selectedQuestions = selectedQuestions;
        this.quizAggregateId = quizAggregateId;
        this.quizVersion = quizVersion;
        this.startTime = startTime;
        this.endTime = endTime;
        this.numberOfQuestions = numberOfQuestions;
    }

    public ExecutionDto getExecutionDto() {
        return executionDto;
    }

    public void setExecutionDto(ExecutionDto executionDto) {
        this.executionDto = executionDto;
    }

    public UserDto getCreatorDto() {
        return creatorDto;
    }

    public void setCreatorDto(UserDto creatorDto) {
        this.creatorDto = creatorDto;
    }

    public List<TournamentTopicDto> getTopics() {
        return topics;
    }

    public void setTopics(List<TournamentTopicDto> topics) {
        this.topics = topics;
    }

    public List<QuizQuestionDto> getSelectedQuestions() {
        return selectedQuestions;
    }

    public void setSelectedQuestions(List<QuizQuestionDto> selectedQuestions) {
        this.selectedQuestions = selectedQuestions;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public void setQuizAggregateId(Integer quizAggregateId) {
        this.quizAggregateId = quizAggregateId;
    }

    public Long getQuizVersion() {
        return quizVersion;
    }

    public void setQuizVersion(Long quizVersion) {
        this.quizVersion = quizVersion;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(Integer numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }
}
