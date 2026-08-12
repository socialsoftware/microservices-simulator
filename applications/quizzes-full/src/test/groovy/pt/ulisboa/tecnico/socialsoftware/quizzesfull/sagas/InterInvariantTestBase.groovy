package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas

import java.time.LocalDateTime
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest

abstract class InterInvariantTestBase extends QuizzesFullSpockTest {

    enum Stage { ENROLLMENT, QUESTION, QUIZ, TOURNAMENT }

    Integer courseId
    Integer userId
    Integer executionId
    Integer topicId
    Integer questionId
    Integer quizId
    Integer tournamentId
    LocalDateTime startTime
    LocalDateTime endTime

    protected void buildFixture(Set<Stage> stages = [Stage.QUESTION] as Set) {
        courseId = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO).aggregateId
        executionId = createExecution(courseId, ACRONYM_1, ACADEMIC_TERM_1).aggregateId

        boolean needsUser = Stage.ENROLLMENT in stages || Stage.QUIZ in stages || Stage.TOURNAMENT in stages
        if (needsUser) {
            userId = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE).aggregateId
            executionFunctionalities.enrollStudentInExecution(executionId, userId)
        }

        boolean needsQuestion = Stage.QUESTION in stages || Stage.QUIZ in stages || Stage.TOURNAMENT in stages
        if (needsQuestion) {
            topicId = createTopic(courseId, "Topic A").aggregateId
            questionId = createQuestion(courseId, [topicId], "Q1 Title", "Q1 Content").aggregateId
        }

        if (Stage.QUIZ in stages) {
            quizId = createQuiz(executionId, [questionId]).aggregateId
        }

        if (Stage.TOURNAMENT in stages) {
            startTime = LocalDateTime.now().plusDays(1)
            endTime = LocalDateTime.now().plusDays(2)
            tournamentId = createTournament(executionId, userId, [topicId], 1, startTime, endTime).aggregateId
        }
    }
}
