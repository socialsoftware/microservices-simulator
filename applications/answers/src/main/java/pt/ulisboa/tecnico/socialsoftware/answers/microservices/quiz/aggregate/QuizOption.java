package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.OptionDto;

@Entity
public class QuizOption {
    @Id
    @GeneratedValue
    private Integer optionNumber;
    private String content;
    private Boolean isCorrect;
    @OneToOne
    private Quiz quiz; 

    public QuizOption() {
    }

    public QuizOption(OptionDto optionDto) {

    }

    public QuizOption(QuizOption other) {
        setContent(other.getContent());
        setIsCorrect(other.getIsCorrect());
    }


    public Integer getOptionNumber() {
        return optionNumber;
    }

    public void setOptionNumber(Integer optionNumber) {
        this.optionNumber = optionNumber;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }


}