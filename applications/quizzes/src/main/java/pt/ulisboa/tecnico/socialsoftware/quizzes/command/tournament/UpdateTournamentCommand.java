package pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;

import java.util.Set;

public class UpdateTournamentCommand extends Command {
    private final TournamentDto tournamentDto;
    private final Set<TopicDto> topicDtos;

    public UpdateTournamentCommand(UnitOfWork unitOfWork, String serviceName, TournamentDto tournamentDto,
            Set<TopicDto> topicDtos) {
        super(unitOfWork, serviceName, tournamentDto.getAggregateId());
        this.tournamentDto = tournamentDto;
        this.topicDtos = topicDtos;
    }

    public TournamentDto getTournamentDto() {
        return tournamentDto;
    }

    public Set<TopicDto> getTopicDtos() {
        return topicDtos;
    }
}
