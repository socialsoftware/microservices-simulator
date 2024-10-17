package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates

import org.springframework.boot.test.context.SpringBootTest
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.Tournament
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentParticipant
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentParticipantQuizAnswer
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentTopic
import pt.ulisboa.tecnico.socialsoftware.ms.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.utils.DateHandler
import spock.lang.Shared
import spock.lang.Unroll

@SpringBootTest
class TournamentMergeUnitTest extends QuizzesSpockTest {
    Tournament tournament1, tournament2, tournament3
    TournamentTopic topic1, topic2, topic3
    TournamentParticipant participant1, participant2, participant3

    @Shared
    Set<TournamentTopic> topicSetHasTopics1and2 = new HashSet<>()
    @Shared
    Set<TournamentTopic> topicSetHasTopics1 = new HashSet<>()
    @Shared
    Set<TournamentTopic> topicSetHasTopics2 = new HashSet<>()
    @Shared
    Set<TournamentTopic> topicSetHasTopics1and2and3 = new HashSet<>()
    @Shared
    Set<TournamentTopic> topicSetHasTopics1and3 = new HashSet<>()
    @Shared
    Set<TournamentTopic> topicSetHasTopics2and3 = new HashSet<>()
    @Shared
    Set<TournamentTopic> topicSetIsEmpty = new HashSet<>()

    @Shared
    Set<TournamentParticipant> participantSetHasParticipants1and2 = new HashSet<>()
    @Shared
    Set<TournamentParticipant> participantSetHasParticipants1 = new HashSet<>()
    @Shared
    Set<TournamentParticipant> participantSetHasParticipants2 = new HashSet<>()
    @Shared
    Set<TournamentParticipant> participantSetHasParticipants1and2and3 = new HashSet<>()
    @Shared
    Set<TournamentParticipant> participantSetHasParticipants1and3 = new HashSet<>()
    @Shared
    Set<TournamentParticipant> participantSetHasParticipants2and3 = new HashSet<>()
    @Shared
    Set<TournamentParticipant> participantSetIsEmpty = new HashSet<>()

    def setup() {
        TournamentDto tournamentDto1 = new TournamentDto()
        tournamentDto1.setStartTime(DateHandler.toISOString(TIME_1))
        tournamentDto1.setEndTime(DateHandler.toISOString(TIME_2))
        tournamentDto1.setNumberOfQuestions(5)

        UserDto creatorDto = new UserDto()
        creatorDto.aggregateId = USER_AGGREGATE_ID_1
        creatorDto.name = USER_NAME_1
        creatorDto.username = USER_USERNAME_1

        CourseExecutionDto courseExecutionDto = new CourseExecutionDto()
        courseExecutionDto.setAggregateId(COURSE_EXECUTION_AGGREGATE_ID_1)
        courseExecutionDto.setVersion(6)
        courseExecutionDto.setAcronym(ACRONYM_1)

        TopicDto topicDto1 = new TopicDto()
        topicDto1.setAggregateId(TOPIC_AGGREGATE_ID_1)
        topicDto1.setName(TOPIC_NAME_1)
        topicDto1.setVersion(1)

        TopicDto topicDto2 = new TopicDto()
        topicDto2.setAggregateId(TOPIC_AGGREGATE_ID_2)
        topicDto2.setName(TOPIC_NAME_2)
        topicDto2.setVersion(2)

        TopicDto topicDto3 = new TopicDto()
        topicDto3.setAggregateId(TOPIC_AGGREGATE_ID_3)
        topicDto3.setName(TOPIC_NAME_3)
        topicDto3.setVersion(3)

        Set<TopicDto> topicDto1And2 = new HashSet<>(Arrays.asList(topicDto1,topicDto2))

        topic1 = new TournamentTopic(topicDto1)
        topic2 = new TournamentTopic(topicDto2)
        topic3 = new TournamentTopic(topicDto3)

        topicSetHasTopics1and2.add(topic1)
        topicSetHasTopics1and2.add(topic2)

        topicSetHasTopics1.add(topic1)

        topicSetHasTopics2.add(topic2)

        topicSetHasTopics1and2and3.add(topic1)
        topicSetHasTopics1and2and3.add(topic2)
        topicSetHasTopics1and2and3.add(topic3)

        topicSetHasTopics1and3.add(topic1)
        topicSetHasTopics1and3.add(topic3)

        topicSetHasTopics2and3.add(topic2)
        topicSetHasTopics2and3.add(topic3)

        topicSetIsEmpty.add(null)

        def ansId1 = 25
        def ansVer1 = 50
        def ansNoAnswered1 = 5
        def ansNoCorrect1 = 3

        def ansId2 = 30
        def ansVer2 = 65
        def ansNoAnswered2 = 4
        def ansNoCorrect2 = 4

        def answer1 = new TournamentParticipantQuizAnswer()
        answer1.setQuizAnswerAggregateId(ansId1)
        answer1.setQuizAnswerVersion(ansVer1)
        answer1.setNumberOfAnswered(ansNoAnswered1)
        answer1.setNumberOfCorrect(ansNoCorrect1)

        def answer1Committed = new TournamentParticipantQuizAnswer()
        answer1Committed.setQuizAnswerAggregateId(ansId1)
        answer1Committed.setQuizAnswerVersion(ansVer2)
        answer1Committed.setNumberOfAnswered(ansNoAnswered2)
        answer1Committed.setNumberOfCorrect(ansNoCorrect2)

        participant1 = new TournamentParticipant()
        participant1.setParticipantAggregateId(USER_AGGREGATE_ID_1)
        participant1.setParticipantName(USER_NAME_1)
        participant1.setParticipantUsername(USER_USERNAME_1)
        participant1.setParticipantVersion(1)
        participant1.setParticipantAnswer(answer1)

        participant2 = new TournamentParticipant()
        participant2.setParticipantAggregateId(USER_AGGREGATE_ID_2)
        participant2.setParticipantName(USER_NAME_2)
        participant2.setParticipantUsername(USER_USERNAME_2)
        participant2.setParticipantVersion(2)
        participant2.setParticipantAnswer(answer1)

        participant3 = new TournamentParticipant()
        participant3.setParticipantAggregateId(USER_AGGREGATE_ID_3)
        participant3.setParticipantName(USER_NAME_3)
        participant3.setParticipantUsername(USER_USERNAME_3)
        participant3.setParticipantVersion(3)
        participant3.setParticipantAnswer(answer1)

        participantSetIsEmpty.add(null)

        participantSetHasParticipants1.add(participant1)

        participantSetHasParticipants2.add(participant2)

        participantSetHasParticipants1and2.add(participant1)
        participantSetHasParticipants1and2.add(participant2)

        participantSetHasParticipants1and3.add(participant1)
        participantSetHasParticipants1and3.add(participant3)

        participantSetHasParticipants2and3.add(participant2)
        participantSetHasParticipants2and3.add(participant3)

        participantSetHasParticipants1and2and3.add(participant1)
        participantSetHasParticipants1and2and3.add(participant2)
        participantSetHasParticipants1and2and3.add(participant3)

        QuizDto quizDto = new QuizDto()
        quizDto.setAggregateId(QUIZ_AGGREGATE_ID_1)
        quizDto.setVersion(60)

        tournament1 = new CausalTournament(TOURNAMENT_AGGREGATE_ID_1, tournamentDto1, creatorDto, courseExecutionDto, topicDto1And2, quizDto)

        tournament2 = new CausalTournament(tournament1)
        tournament3 = new CausalTournament(tournament1)
    }

    def cleanup() {
    }

    // merge intentions

    @Unroll
    def 'intention fail combination' () {
        given:
        def prev = tournament1
        def toCommit = tournament2
        def committed = tournament3

        toCommit.setStartTime(toCommitStartTime)
        committed.setStartTime(committedStartTime)

        toCommit.setEndTime(toCommitEndTime)
        committed.setEndTime(committedEndTime)

        toCommit.setNumberOfQuestions(toCommitNoQuestions)
        committed.setNumberOfQuestions(committedNoQuestions)

        toCommit.setTournamentTopics(toCommitTopics)
        committed.setTournamentTopics(committedTopics)

        when:
        toCommit.merge(toCommit, committed)

        then:
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.AGGREGATE_MERGE_FAILURE_DUE_TO_INTENSIONS_CONFLICT

        where:
//      Prev values
//      TIME_1            | TIME_1             | TIME_2          | TIME_2           | 5                   | 5                    | Set.of(topic1, topic2) | Set.of(topic1, topic2)
        toCommitStartTime | committedStartTime | toCommitEndTime | committedEndTime | toCommitNoQuestions | committedNoQuestions | toCommitTopics         | committedTopics
        TIME_3            | TIME_1             | TIME_2          | TIME_4           | 5                   | 5                    | topicSetHasTopics1and2 | topicSetHasTopics1and2 // start/end
        TIME_3            | TIME_1             | TIME_2          | TIME_2           | 5                   | 4                    | topicSetHasTopics1and2 | topicSetHasTopics1and2 // start/numberofquestions
        TIME_1            | TIME_1             | TIME_3          | TIME_2           | 5                   | 6                    | topicSetHasTopics1and2 | topicSetHasTopics1and2 // end/numberofquestions
        TIME_1            | TIME_1             | TIME_2          | TIME_2           | 1                   | 5                    | topicSetHasTopics1and2 | topicSetHasTopics2 // numberofquestions/topics
    }


    @Unroll
    def 'intention pass combination' () {
        given:
        def prev = tournament1
        def toCommit = tournament2
        def committed = tournament3

        toCommit.setStartTime(toCommitStartTime)
        committed.setStartTime(committedStartTime)

        toCommit.setEndTime(toCommitEndTime)
        committed.setEndTime(committedEndTime)

        toCommit.setNumberOfQuestions(toCommitNoQuestions)
        committed.setNumberOfQuestions(committedNoQuestions)

        toCommit.setTournamentTopics(toCommitTopics)
        committed.setTournamentTopics(committedTopics)

        when:
        def mergedTournament = (Tournament)toCommit.merge(toCommit, committed)

        then:
        mergedTournament != null

        where:
//      Prev values
//      TIME_1            | TIME_1             | TIME_2          | TIME_2           | 5                   | 5                    | Set.of(topic1, topic2) | Set.of(topic1, topic2)
        toCommitStartTime | committedStartTime | toCommitEndTime | committedEndTime | toCommitNoQuestions | committedNoQuestions | toCommitTopics         | committedTopics
        TIME_3            | TIME_1             | TIME_4          | TIME_4           | 5                   | 5                    | topicSetHasTopics1and2 | topicSetHasTopics1and2 // start && end/end
        TIME_3            | TIME_1             | TIME_2          | TIME_2           | 3                   | 4                    | topicSetHasTopics1and2 | topicSetHasTopics1and2 // start && numberofquestions/numberofquestions
        TIME_1            | TIME_1             | TIME_3          | TIME_2           | 3                   | 6                    | topicSetHasTopics1and2 | topicSetHasTopics1and2 // end && numberofquestions/numberofquestions
        TIME_1            | TIME_1             | TIME_2          | TIME_2           | 1                   | 5                    | topicSetHasTopics1     | topicSetHasTopics2 // numberofquestions && topics/topics
        TIME_3            | TIME_1             | TIME_2          | TIME_2           | 5                   | 5                    | topicSetHasTopics1and2 | topicSetHasTopics1 // start / topics
        TIME_1            | TIME_1             | TIME_3          | TIME_2           | 5                   | 5                    | topicSetHasTopics1and2 | topicSetHasTopics1 // end / topics
    }

    // merge operations

    def 'startTime merge' () {
        given:
        def prev = tournament1
        def toCommit = tournament2
        def committed = tournament3

        prev.setStartTime(TIME_1)
        committed.setStartTime(committedTime)
        toCommit.setStartTime(toCommitTime)

        when:
        def mergedTournament = (Tournament)toCommit.merge(toCommit, committed)

        then:
        mergedTournament.getStartTime() == result

        where:
        committedTime | toCommitTime || result
        TIME_4        | TIME_3       || TIME_3
        TIME_1        | TIME_3       || TIME_3
        TIME_4        | TIME_1       || TIME_4
    }

    def 'endTime merge' () {
        given:
        def prev = tournament1
        def toCommit = tournament2
        def committed = tournament3

        prev.setEndTime(TIME_2)
        committed.setEndTime(committedTime)
        toCommit.setEndTime(toCommitTime)

        when:
        def mergedTournament = (Tournament)toCommit.merge(toCommit, committed)

        then:
        mergedTournament.getEndTime() == result

        where:
        committedTime | toCommitTime || result
        TIME_4        | TIME_3       || TIME_3
        TIME_2        | TIME_3       || TIME_3
        TIME_4        | TIME_2       || TIME_4
    }

    def 'numberOfQuestions merge' () {
        given:
        def prev = tournament1
        def committed = tournament3
        def toCommit = tournament2

        prev.setNumberOfQuestions(5)
        committed.setNumberOfQuestions(committedValue)
        toCommit.setNumberOfQuestions(toCommitValue)

        when:
        def mergedTournament = (Tournament)toCommit.merge(toCommit, committed)

        then:
        mergedTournament.getNumberOfQuestions() == result

        where:
        committedValue | toCommitValue || result
        4              | 3             || 3
        5              | 3             || 3
        4              | 5             || 4
    }

    @Unroll
    def 'topics merge' () {
        given:
        def prev = tournament1
        def committed = tournament3
        def toCommit = tournament2

        prev.setTournamentTopics(topicSetHasTopics1and2)
        committed.setTournamentTopics(committedTopics)
        toCommit.setTournamentTopics(toCommitTopics)

        when:
        def mergedTournament = (Tournament)toCommit.merge(toCommit, committed)

        then:
        mergedTournament.getTournamentTopics() == result

        where:
        committedTopics        | toCommitTopics             || result
        topicSetHasTopics1and2 | topicSetHasTopics1and2     || topicSetHasTopics1and2
        topicSetHasTopics2     | topicSetHasTopics1         || topicSetHasTopics1
        topicSetHasTopics1     | topicSetHasTopics1and2     || topicSetHasTopics1
        topicSetHasTopics1and2 | topicSetHasTopics1and2and3 || topicSetHasTopics1and2and3
    }

    @Unroll
    def 'participants merge' () {
        given:
        def prev = tournament1
        def committed = tournament3
        def toCommit = tournament2

        prev.setTournamentParticipants(prevPartipants)
        committed.setTournamentParticipants(committedParticipants)
        toCommit.setTournamentParticipants(toCommitParticipants)

        when:
        def mergedTournament = (Tournament)toCommit.merge(toCommit, committed)

        then:
        mergedTournament.getTournamentParticipants() == result

        where:
        prevPartipants                     | committedParticipants                  | toCommitParticipants                   || result
        participantSetHasParticipants1and2 | participantSetHasParticipants1and2     | participantSetHasParticipants1and2     || participantSetHasParticipants1and2 // no changes
        participantSetHasParticipants1and2 | participantSetHasParticipants1         | participantSetHasParticipants1and2     || participantSetHasParticipants1 // only committed deletes
        participantSetHasParticipants1and2 | participantSetHasParticipants1and2     | participantSetHasParticipants1         || participantSetHasParticipants1 // only to commit deletes
        participantSetHasParticipants1and2 | participantSetHasParticipants2         | participantSetHasParticipants1         || new HashSet<TournamentParticipant>() // two different deletes
        participantSetHasParticipants1and2 | participantSetHasParticipants1         | participantSetHasParticipants1         || participantSetHasParticipants1 // same delete
        participantSetHasParticipants1and2 | participantSetHasParticipants1and2and3 | participantSetHasParticipants1and2     || participantSetHasParticipants1and2and3 // only committed inserts
        participantSetHasParticipants1and2 | participantSetHasParticipants1and2     | participantSetHasParticipants1and2and3 || participantSetHasParticipants1and2and3 // only to commit inserts
        participantSetHasParticipants1     | participantSetHasParticipants1and2     | participantSetHasParticipants1and3     || participantSetHasParticipants1and2and3 // two different inserts
        participantSetHasParticipants1     | participantSetHasParticipants1and2     | participantSetHasParticipants1and2     || participantSetHasParticipants1and2 // same insert
        participantSetHasParticipants1and2 | participantSetHasParticipants1         | participantSetHasParticipants1and2and3 || participantSetHasParticipants1and3 // different committed delete and to commit insert
        participantSetHasParticipants1and2 | participantSetHasParticipants1and2and3 | participantSetHasParticipants1         || participantSetHasParticipants1and3 // different committed insert and to commit delete
    }

    def 'participants merge with different versions of the same participant _one is anonymous_' () {
        given:
        def prev = tournament1
        def committed = tournament3
        def toCommit = tournament2

        def ansId1 = 25
        def ansVer1 = 50
        def ansNoAnswered1 = 5
        def ansNoCorrect1 = 3

        def ansId2 = 30
        def ansVer2 = 65
        def ansNoAnswered2 = 4
        def ansNoCorrect2 = 4

        def answer1 = new TournamentParticipantQuizAnswer()
        answer1.setQuizAnswerAggregateId(ansId1)
        answer1.setQuizAnswerVersion(ansVer1)
        answer1.setNumberOfAnswered(ansNoAnswered1)
        answer1.setNumberOfCorrect(ansNoCorrect1)

        def answer1Committed = new TournamentParticipantQuizAnswer()
        answer1Committed.setQuizAnswerAggregateId(ansId1)
        answer1Committed.setQuizAnswerVersion(ansVer2)
        answer1Committed.setNumberOfAnswered(ansNoAnswered2)
        answer1Committed.setNumberOfCorrect(ansNoCorrect2)

        def participant1Committed = new TournamentParticipant()
        participant1Committed.setParticipantAggregateId(participant1.getParticipantAggregateId())
        participant1Committed.setParticipantName(ANONYMOUS)
        participant1Committed.setParticipantUsername(ANONYMOUS)
        participant1Committed.setParticipantVersion(participant1.getParticipantVersion() + 1)

        HashSet<TournamentParticipant> prevParticipants = new HashSet<>()
        prevParticipants.add(participant1)

        HashSet<TournamentParticipant> toCommitParticipants = new HashSet<>()
        toCommitParticipants.add(participant1)

        HashSet<TournamentParticipant> committedParticipants = new HashSet<>()
        committedParticipants.add(participant1Committed)


        prev.setTournamentParticipants(prevParticipants)
        toCommit.setTournamentParticipants(toCommitParticipants)
        committed.setTournamentParticipants(committedParticipants)

        when:
        def mergedTournament = (Tournament)(toCommit.merge(toCommit, committed))

        then:
        mergedTournament.getTournamentParticipants() == new HashSet(committedParticipants)
    }

    def 'participants merge with different versions of the same participant _one has answers_' () {
        given:
        def prev = tournament1
        def toCommit = tournament2
        def committed = tournament3

        def ansId1 = 25
        def ansVer1 = 50
        def ansNoAnswered1 = 5
        def ansNoCorrect1 = 3

        def ansId2 = 30
        def ansVer2 = 65
        def ansNoAnswered2 = 4
        def ansNoCorrect2 = 4

        def answer1 = new TournamentParticipantQuizAnswer()
        answer1.setQuizAnswerAggregateId(ansId1)
        answer1.setQuizAnswerVersion(ansVer1)
        answer1.setNumberOfAnswered(ansNoAnswered1)
        answer1.setNumberOfCorrect(ansNoCorrect1)

        def answer1Committed = new TournamentParticipantQuizAnswer()
        answer1Committed.setQuizAnswerAggregateId(ansId1)
        answer1Committed.setQuizAnswerVersion(ansVer2)
        answer1Committed.setNumberOfAnswered(ansNoAnswered2)
        answer1Committed.setNumberOfCorrect(ansNoCorrect2)

        def participant1Committed = new TournamentParticipant()
        participant1Committed.setParticipantAggregateId(participant1.getParticipantAggregateId())
        participant1Committed.setParticipantName(participant1.getParticipantName())
        participant1Committed.setParticipantUsername(participant1.getParticipantUsername())
        participant1Committed.setParticipantVersion(participant1.getParticipantVersion() + 1)

        participant1.setParticipantAnswer(answer1)
        participant1Committed.setParticipantAnswer(answer1Committed)

        HashSet<TournamentParticipant> prevParticipants = new HashSet<TournamentParticipant>()
        prevParticipants.add(participant1)

        HashSet<TournamentParticipant> toCommitParticipants = new HashSet<TournamentParticipant>()
        toCommitParticipants.add(participant1)

        HashSet<TournamentParticipant> committedParticipants = new HashSet<>()
        committedParticipants.add(participant1Committed)

        prev.setTournamentParticipants(prevParticipants)
        toCommit.setTournamentParticipants(toCommitParticipants)
        committed.setTournamentParticipants(committedParticipants)

        when:
        def mergedTournament = (Tournament)(toCommit.merge(toCommit, committed))

        then:
        mergedTournament.getTournamentParticipants() == new HashSet(committedParticipants)
    }

}