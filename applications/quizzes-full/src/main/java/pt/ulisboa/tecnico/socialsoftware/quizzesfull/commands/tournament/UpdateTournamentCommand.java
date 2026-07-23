package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto;

import java.time.LocalDateTime;
import java.util.List;

public class UpdateTournamentCommand extends Command {
    private final Integer tournamentAggregateId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final List<TopicDto> topicDtos;

    public UpdateTournamentCommand(UnitOfWork unitOfWork, String serviceName,
                                   Integer tournamentAggregateId,
                                   LocalDateTime startTime, LocalDateTime endTime,
                                   List<TopicDto> topicDtos) {
        super(unitOfWork, serviceName, tournamentAggregateId);
        this.tournamentAggregateId = tournamentAggregateId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.topicDtos = topicDtos;
    }

    public Integer getTournamentAggregateId() { return tournamentAggregateId; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public List<TopicDto> getTopicDtos() { return topicDtos; }
}
