package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto;

import java.time.LocalDateTime;
import java.util.List;

public class CreateTournamentCommand extends Command {
    private final Integer executionAggregateId;
    private final Long executionVersion;
    private final Integer executionCourseId;
    private final Integer creatorAggregateId;
    private final String creatorName;
    private final String creatorUsername;
    private final Long creatorVersion;
    private final List<TopicDto> topicDtos;
    private final Integer quizAggregateId;
    private final Long quizVersion;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final Integer numberOfQuestions;

    public CreateTournamentCommand(UnitOfWork unitOfWork, String serviceName,
                                   Integer executionAggregateId, Long executionVersion, Integer executionCourseId,
                                   Integer creatorAggregateId, String creatorName, String creatorUsername, Long creatorVersion,
                                   List<TopicDto> topicDtos,
                                   Integer quizAggregateId, Long quizVersion,
                                   LocalDateTime startTime, LocalDateTime endTime, Integer numberOfQuestions) {
        super(unitOfWork, serviceName, null);
        this.executionAggregateId = executionAggregateId;
        this.executionVersion = executionVersion;
        this.executionCourseId = executionCourseId;
        this.creatorAggregateId = creatorAggregateId;
        this.creatorName = creatorName;
        this.creatorUsername = creatorUsername;
        this.creatorVersion = creatorVersion;
        this.topicDtos = topicDtos;
        this.quizAggregateId = quizAggregateId;
        this.quizVersion = quizVersion;
        this.startTime = startTime;
        this.endTime = endTime;
        this.numberOfQuestions = numberOfQuestions;
    }

    public Integer getExecutionAggregateId() { return executionAggregateId; }
    public Long getExecutionVersion() { return executionVersion; }
    public Integer getExecutionCourseId() { return executionCourseId; }
    public Integer getCreatorAggregateId() { return creatorAggregateId; }
    public String getCreatorName() { return creatorName; }
    public String getCreatorUsername() { return creatorUsername; }
    public Long getCreatorVersion() { return creatorVersion; }
    public List<TopicDto> getTopicDtos() { return topicDtos; }
    public Integer getQuizAggregateId() { return quizAggregateId; }
    public Long getQuizVersion() { return quizVersion; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public Integer getNumberOfQuestions() { return numberOfQuestions; }
}
