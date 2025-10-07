package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;

@Entity
public class AnswerQuiz {
    @Id
    @GeneratedValue
    private Long id;
    private Integer quizAggregateId;
    private Integer quizVersion;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "answerquiz")
    private Set<Object> quizQuestionsAggregateIds = new HashSet<>();
    @OneToOne
    private Answer answer; 

    public AnswerQuiz() {
    }

    public AnswerQuiz(AnswerDto answerDto) {
        setQuizAggregateId(answerDto.getQuizAggregateId());
        setQuizVersion(answerDto.getQuizVersion());
    }

    public AnswerQuiz(AnswerQuiz other) {
        setQuizAggregateId(other.getQuizAggregateId());
        setQuizVersion(other.getQuizVersion());
        setQuizQuestionsAggregateIds(other.getQuizQuestionsAggregateIds().stream().map(Object::new).collect(Collectors.toSet()));
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

    public Set<Object> getQuizQuestionsAggregateIds() {
        return quizQuestionsAggregateIds;
    }

    public void setQuizQuestionsAggregateIds(Set<Object> quizQuestionsAggregateIds) {
        this.quizQuestionsAggregateIds = quizQuestionsAggregateIds;
        if (this.quizQuestionsAggregateIds != null) {
            this.quizQuestionsAggregateIds.forEach(object -> object.setAnswerQuiz(this));
        }
    }

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }


}