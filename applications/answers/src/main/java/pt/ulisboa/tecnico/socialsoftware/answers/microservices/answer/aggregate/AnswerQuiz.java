package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;

@Entity
public class AnswerQuiz {
    @Id
    @GeneratedValue
    private Long id;
    private Integer quizAggregateId;
    private Integer quizVersion;
    private List<Integer> quizQuestionsAggregateIds = new ArrayList<>();
    @OneToOne
    private Answer answer;

    public AnswerQuiz() {
    }

    public AnswerQuiz(QuizDto quizDto) {
        setQuizAggregateId(quizDto.getAggregateId());
        setQuizVersion(quizDto.getVersion());
        setQuizQuestionsAggregateIds(quizDto.getQuestions().stream()
            .map(QuestionDto::getAggregateId)
            .collect(Collectors.toList()));
    }

    public AnswerQuiz(AnswerQuiz other) {
        setQuizAggregateId(other.getQuizAggregateId());
        setQuizVersion(other.getQuizVersion());
        setQuizQuestionsAggregateIds(new ArrayList<>(other.getQuizQuestionsAggregateIds()));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public void setQuizAggregateId(Integer quizAggregateId) {
        this.quizAggregateId = quizAggregateId;
    }

    public Integer getQuizVersion() {
        return quizVersion;
    }

    public void setQuizVersion(Integer quizVersion) {
        this.quizVersion = quizVersion;
    }

    public List<Integer> getQuizQuestionsAggregateIds() {
        return quizQuestionsAggregateIds;
    }

    public void setQuizQuestionsAggregateIds(List<Integer> quizQuestionsAggregateIds) {
        this.quizQuestionsAggregateIds = quizQuestionsAggregateIds;
    }

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }

}