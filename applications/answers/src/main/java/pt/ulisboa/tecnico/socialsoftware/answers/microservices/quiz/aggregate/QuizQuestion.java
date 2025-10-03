package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.OneToOne;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;

@Entity
public class QuizQuestion {
    @Id
    @GeneratedValue
    private Integer questionId;
    private String questionTitle;
    private String questionContent;
    private Integer order;
    @OneToOne
    private Quiz quiz; 

    public QuizQuestion() {
    }

    public QuizQuestion(QuizDto quizDto) {
        setQuestionTitle(quizDto.getQuestionTitle());
        setQuestionContent(quizDto.getQuestionContent());
        setOrder(quizDto.getOrder());
    }

    public QuizQuestion(QuizQuestion other) {
        setQuestionTitle(other.getQuestionTitle());
        setQuestionContent(other.getQuestionContent());
        setOrder(other.getOrder());
    }


    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public String getQuestionTitle() {
        return questionTitle;
    }

    public void setQuestionTitle(String questionTitle) {
        this.questionTitle = questionTitle;
    }

    public String getQuestionContent() {
        return questionContent;
    }

    public void setQuestionContent(String questionContent) {
        this.questionContent = questionContent;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }


}