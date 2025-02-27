package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;

@Entity
public class QuestionAnswer {
    @Id
    @GeneratedValue
    private Long id;
    private Integer optionSequenceChoice;
    private Integer questionAggregateId;
    private Integer questionVersion;
    private Integer timeTaken;
    private Integer optionKey;
    private boolean correct;
    private Aggregate.AggregateState state;
    @ManyToOne
    private QuizAnswer quizAnswer;

    public  QuestionAnswer() {
    }

    public QuestionAnswer(QuestionAnswerDto questionAnswerDto) {
        setOptionSequenceChoice(questionAnswerDto.getSequence());
        setQuestionAggregateId(questionAnswerDto.getQuestionAggregateId());
        setOptionKey(questionAnswerDto.getOptionKey());
        setState(Aggregate.AggregateState.ACTIVE);
    }

    public QuestionAnswer(QuestionAnswerDto questionAnswerDto, QuestionDto questionDto) {
        setQuestionAggregateId(questionAnswerDto.getQuestionAggregateId());
        setQuestionVersion(questionDto.getVersion());
        setOptionSequenceChoice(questionAnswerDto.getSequence());
        setOptionKey(questionAnswerDto.getOptionKey());

        if(getOptionKey() < 1 || getOptionKey() > questionDto.getOptionDtos().size()) {
            throw new TutorException(ErrorMessage.INVALID_OPTION_SELECTED, getOptionKey(), getQuestionAggregateId());
        }

        for(OptionDto o : questionDto.getOptionDtos()) {
            if(o.getKey().equals(questionAnswerDto.getOptionKey())) {
                setCorrect(o.isCorrect());
            }
        }
        setState(Aggregate.AggregateState.ACTIVE);
    }

    public QuestionAnswer(QuestionAnswer other) {
        setQuestionAggregateId(other.getQuestionAggregateId());
        setQuestionVersion(other.getQuestionVersion());
        setOptionSequenceChoice(other.getOptionSequenceChoice());
        setOptionKey(other.getOptionKey());
        setCorrect(other.isCorrect());
        setState(other.getState());
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Integer getOptionSequenceChoice() {
        return optionSequenceChoice;
    }

    public void setOptionSequenceChoice(Integer optionSequence) {
        this.optionSequenceChoice = optionSequence;
    }

    public Integer getQuestionAggregateId() {
        return questionAggregateId;
    }

    public void setQuestionAggregateId(Integer questionId) {
        this.questionAggregateId = questionId;
    }

    public Integer getQuestionVersion() {
        return questionVersion;
    }

    public void setQuestionVersion(Integer questionVersion) {
        this.questionVersion = questionVersion;
    }

    public Integer getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(Integer timeTaken) {
        this.timeTaken = timeTaken;
    }

    public Integer getOptionKey() {
        return optionKey;
    }

    public void setOptionKey(Integer optionKey) {
        this.optionKey = optionKey;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public Aggregate.AggregateState getState() {
        return state;
    }

    public void setState(Aggregate.AggregateState state) {
        this.state = state;
    }

    public QuizAnswer getQuizAnswer() {
        return quizAnswer;
    }

    public void setQuizAnswer(QuizAnswer quizAnswer) {
        this.quizAnswer = quizAnswer;
    }
}
