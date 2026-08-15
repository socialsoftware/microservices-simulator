package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.question

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionTopic
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.sagas.SagaQuestion

@DataJpaTest
@Transactional
@Import(QuestionIntraInvariantTest.LocalBeanConfiguration)
class QuestionIntraInvariantTest extends QuizzesFull2SpockTest {

    private static SagaQuestion aQuestion() {
        return new SagaQuestion(QUESTION_AGGREGATE_ID, COURSE_AGGREGATE_ID, QUESTION_TITLE, QUESTION_CONTENT,
                QUESTION_CREATION_DATE)
    }

    private static QuestionTopic aTopic(Integer courseAggregateId) {
        return new QuestionTopic(QUESTION_TOPIC_AGGREGATE_ID, QUESTION_TOPIC_NAME, QUESTION_TOPIC_VERSION,
                courseAggregateId)
    }

    def "create question"() {
        // Spec: plan.md §5 Question — CreateQuestion(courseAggregateId, title, content, options, topicAggregateIds);
        // snapshot field courseAggregateId (plan.md § snapshot table)
        when:
        def question = aQuestion()
        question.verifyInvariants()

        then:
        question.aggregateId == QUESTION_AGGREGATE_ID
        question.title == QUESTION_TITLE
        question.content == QUESTION_CONTENT
        question.creationDate == QUESTION_CREATION_DATE
        question.courseAggregateId == COURSE_AGGREGATE_ID
        question.options.isEmpty()
        question.topics.isEmpty()
        question.sagaState == GenericSagaState.NOT_IN_SAGA
    }

    def "question: TOPIC_BELONGS_TO_QUESTION_COURSE violation"() {
        // Spec: plan.md §3.2 rule TOPIC_BELONGS_TO_QUESTION_COURSE —
        // forall t in topics: t.courseAggregateId == question.courseAggregateId
        given:
        def question = aQuestion()
        question.addTopic(aTopic(COURSE_AGGREGATE_ID_2))

        when:
        question.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.TOPIC_BELONGS_TO_QUESTION_COURSE
    }

    def "question: TOPIC_BELONGS_TO_QUESTION_COURSE holds for a topic of the question's course"() {
        // Spec: plan.md §3.2 rule TOPIC_BELONGS_TO_QUESTION_COURSE — satisfying representative
        given:
        def question = aQuestion()
        question.addTopic(aTopic(COURSE_AGGREGATE_ID))

        when:
        question.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    // TOPIC_BELONGS_TO_QUESTION_COURSE is a categorical equality over course ids, so testing.md
    // § Choosing Input Values requires no boundary-straddling pair. The Course reference
    // (courseAggregateId) is immutable per domain-model §2 and is held in a Java `final` field,
    // which § T1 excludes from coverage.

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
