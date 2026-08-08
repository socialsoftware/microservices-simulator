package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentTopicDto;

import java.time.LocalDateTime;
import java.util.List;

public class UpdateTournamentCommand extends Command {
    private Integer tournamentAggregateId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer numberOfQuestions;
    private List<TournamentTopicDto> topics;
    private List<QuizQuestionDto> selectedQuestions;

    protected UpdateTournamentCommand() {}

    public UpdateTournamentCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentAggregateId,
                                   LocalDateTime startTime, LocalDateTime endTime, Integer numberOfQuestions,
                                   List<TournamentTopicDto> topics, List<QuizQuestionDto> selectedQuestions) {
        super(unitOfWork, serviceName, tournamentAggregateId);
        this.tournamentAggregateId = tournamentAggregateId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.numberOfQuestions = numberOfQuestions;
        this.topics = topics;
        this.selectedQuestions = selectedQuestions;
    }

    public Integer getTournamentAggregateId() {
        return tournamentAggregateId;
    }

    public void setTournamentAggregateId(Integer tournamentAggregateId) {
        this.tournamentAggregateId = tournamentAggregateId;
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
}
