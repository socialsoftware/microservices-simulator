package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.AnswerUser
import java.time.LocalDateTime

class AnswerUserBuilder extends SpockTest {
    private AnswerUser answeruser

    static AnswerUserBuilder aAnswerUser() {
        return new AnswerUserBuilder()
    }

    AnswerUserBuilder() {
        this.answeruser = new AnswerUser()
        // Set default values
        this.answeruser.setId(1L)
        this.answeruser.setVersion(1)
        this.answeruser.setUserAggregateId(1)
        this.answeruser.setUserName("Default userName")
    }

    AnswerUserBuilder withId(Long id) {
        this.answeruser.setId(id)
        return this
    }

    AnswerUserBuilder withUserAggregateId(Integer userAggregateId) {
        this.answeruser.setUserAggregateId(userAggregateId)
        return this
    }

    AnswerUserBuilder withUserName(String userName) {
        this.answeruser.setUserName(userName)
        return this
    }

    AnswerUserBuilder withUserState(AggregateState userState) {
        this.answeruser.setUserState(userState)
        return this
    }

    AnswerUser build() {
        return this.answeruser
    }
}
