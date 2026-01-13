package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizOption;

public class QuizOptionDto implements Serializable {
    private Integer sequence;
    private Boolean correct;
    private String content;

    public QuizOptionDto() {
    }

    public QuizOptionDto(QuizOption quizOption) {
        this.sequence = quizOption.getOptionSequence();
        this.correct = quizOption.getOptionCorrect();
        this.content = quizOption.getOptionContent();
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}