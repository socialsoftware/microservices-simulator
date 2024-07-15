package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data;

import java.util.HashSet;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTopic;

public class UpdateTournamentData extends WorkflowData{
    private TournamentDto originalTournamentDto;
    private HashSet<SagaTopic> topics = new HashSet<SagaTopic>();
    private Tournament oldTournament;
    private TournamentDto newTournamentDto;
    private QuizDto quizDto;
    private Quiz oldQuiz;

    public TournamentDto getOriginalTournamentDto() {
        return originalTournamentDto;
    }

    public void setOriginalTournamentDto(TournamentDto originalTournamentDto) {
        this.originalTournamentDto = originalTournamentDto;
    }

    public HashSet<SagaTopic> getTopics() {
        return topics;
    }

    public HashSet<TopicDto> getTopicsDtos() {;
        HashSet<TopicDto> topicDtos = topics.stream()
                .map(TopicDto::new)
                .collect(Collectors.toCollection(HashSet::new));
        return topicDtos;
    }

    public void setTopics(HashSet<SagaTopic> topics) {
        this.topics = topics;
    }

    public void addTopic(SagaTopic topic) {
        this.topics.add(topic);
    }

    public Tournament getOldTournament() {
        return oldTournament;
    }

    public void setOldTournament(Tournament oldTournament) {
        this.oldTournament = oldTournament;
    }

    public TournamentDto getNewTournamentDto() {
        return newTournamentDto;
    }

    public void setNewTournamentDto(TournamentDto newTournamentDto) {
        this.newTournamentDto = newTournamentDto;
    }

    public QuizDto getQuizDto() {
        return quizDto;
    }

    public void setQuizDto(QuizDto quizDto) {
        this.quizDto = quizDto;
    }

    public Quiz getOldQuiz() {
        return oldQuiz;
    }

    public void setOldQuiz(Quiz oldQuiz) {
        this.oldQuiz = oldQuiz;
    }
}