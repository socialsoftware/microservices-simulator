package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
public class AnsweredQuiz {
    @Id
    @GeneratedValue
    private Long id;
    private Integer quizAggregateId;
    private Integer quizVersion;
    @OneToOne
    private QuizAnswer quizAnswer;
    @ElementCollection
    private List<Integer> quizQuestionsAggregateIds;

    public AnsweredQuiz() {
        this.quizAggregateId = 0;
    }

    public AnsweredQuiz(QuizDto quizDto) {
        this.quizAggregateId = quizDto.getAggregateId();
        setQuizVersion(quizDto.getVersion());
        setQuizQuestionsAggregateIds(quizDto.getQuestionDtos().stream()
                .map(QuestionDto::getAggregateId)
                .collect(Collectors.toList()));
    }

    public AnsweredQuiz(AnsweredQuiz other) {
        this.quizAggregateId = other.getQuizAggregateId();
        setQuizVersion(other.getQuizVersion());
        setQuizQuestionsAggregateIds(new ArrayList<>(other.getQuizQuestionsAggregateIds()));
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
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

    public QuizAnswer getQuizAnswer() {
        return quizAnswer;
    }

    public void setQuizAnswer(QuizAnswer quizAnswer) {
        this.quizAnswer = quizAnswer;
    }
}
