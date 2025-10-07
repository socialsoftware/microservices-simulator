package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.AnswerQuestion
import java.time.LocalDateTime

class AnswerQuestionBuilder extends SpockTest {
    private AnswerQuestion answerquestion

    static AnswerQuestionBuilder aAnswerQuestion() {
        return new AnswerQuestionBuilder()
    }

    AnswerQuestionBuilder() {
        this.answerquestion = new AnswerQuestion()
        // Set default values
        this.answerquestion.setId(1L)
        this.answerquestion.setVersion(1)
        this.answerquestion.setQuestionOptionSequenceChoice(1)
        this.answerquestion.setQuestionAggregateId(1)
        this.answerquestion.setQuestionVersion(1)
        this.answerquestion.setQuestionTimeTaken(1)
        this.answerquestion.setQuestionOptionKey(1)
        this.answerquestion.setQuestionCorrect(false)
    }

    AnswerQuestionBuilder withId(Long id) {
        this.answerquestion.setId(id)
        return this
    }

    AnswerQuestionBuilder withQuestionOptionSequenceChoice(Integer questionOptionSequenceChoice) {
        this.answerquestion.setQuestionOptionSequenceChoice(questionOptionSequenceChoice)
        return this
    }

    AnswerQuestionBuilder withQuestionAggregateId(Integer questionAggregateId) {
        this.answerquestion.setQuestionAggregateId(questionAggregateId)
        return this
    }

    AnswerQuestionBuilder withQuestionVersion(Integer questionVersion) {
        this.answerquestion.setQuestionVersion(questionVersion)
        return this
    }

    AnswerQuestionBuilder withQuestionTimeTaken(Integer questionTimeTaken) {
        this.answerquestion.setQuestionTimeTaken(questionTimeTaken)
        return this
    }

    AnswerQuestionBuilder withQuestionOptionKey(Integer questionOptionKey) {
        this.answerquestion.setQuestionOptionKey(questionOptionKey)
        return this
    }

    AnswerQuestionBuilder withQuestionCorrect(Boolean questionCorrect) {
        this.answerquestion.setQuestionCorrect(questionCorrect)
        return this
    }

    AnswerQuestionBuilder withQuestionState(AggregateState questionState) {
        this.answerquestion.setQuestionState(questionState)
        return this
    }

    AnswerQuestion build() {
        return this.answerquestion
    }
}
