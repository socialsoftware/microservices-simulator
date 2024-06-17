package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates;

import jakarta.persistence.Entity;
import org.apache.commons.collections4.SetUtils;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentParticipantQuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.AGGREGATE_MERGE_FAILURE;

@Entity
public class CausalTournament extends Tournament implements CausalAggregate {
    public CausalTournament() {
        super();
    }

    public CausalTournament(Integer aggregateId, TournamentDto tournamentDto, UserDto creatorDto,
                            CourseExecutionDto courseExecutionDto, Set<TopicDto> topicDtos, QuizDto quizDto) {
        super(aggregateId, tournamentDto, creatorDto, courseExecutionDto, topicDtos, quizDto);
    }

    /* used to update the tournament by creating new versions */
    public CausalTournament(CausalTournament other) {
        super(other);
    }

    @Override
    public Set<String> getMutableFields()  {
        return Set.of("startTime", "endTime", "numberOfQuestions", "tournamentTopics", "tournamentParticipants", "cancelled", "tournamentCourseExecution", "tournamentCreator");
    }

    @Override
    public Set<String[]> getIntentions() {
        return Set.of(
                new String[]{"startTime", "endTime"},
                new String[]{"startTime", "numberOfQuestions"},
                new String[]{"endTime", "numberOfQuestions"},
                new String[]{"numberOfQuestions", "tournamentTopics"});
    }

    @Override
    public Aggregate mergeFields(Set<String> toCommitVersionChangedFields, Aggregate committedVersion, Set<String> committedVersionChangedFields){
        if (!(committedVersion instanceof CausalTournament)) {
            throw new TutorException(AGGREGATE_MERGE_FAILURE, getAggregateId());
        }

        CausalTournament committedTournament = (CausalTournament) committedVersion;
        CausalTournament mergedTournament = new CausalTournament(this);

        // merge of creator is built in the participants dont know yet
        mergeCreator(committedTournament, mergedTournament);
        //mergeCourseExecution(committedTournament, mergedTournament);
        mergeCourseExecution(committedTournament, mergedTournament);
        mergeQuiz(committedTournament, mergedTournament);
        mergeCancelled(toCommitVersionChangedFields, committedTournament, mergedTournament);
        mergeStartTime(toCommitVersionChangedFields, committedTournament, mergedTournament);
        mergeEndTime(toCommitVersionChangedFields, committedTournament, mergedTournament);
        mergeNumberOfQuestions(toCommitVersionChangedFields, committedTournament, mergedTournament);
        mergeParticipants((Tournament) getPrev(), this, committedTournament, mergedTournament);
        //mergeTopics((Tournament) getPrev(), this, committedTournament, mergedTournament);
        mergeTopics(toCommitVersionChangedFields, committedTournament, mergedTournament);

        return mergedTournament;
    }

    private void mergeCreator(Tournament committedTournament, Tournament mergedTournament) {
        if (getTournamentCourseExecution().getCourseExecutionVersion() >= committedTournament.getTournamentCourseExecution().getCourseExecutionVersion()) {
            mergedTournament.getTournamentCreator().setCreatorName(getTournamentCreator().getCreatorName());
            mergedTournament.getTournamentCreator().setCreatorUsername(getTournamentCreator().getCreatorUsername());
            mergedTournament.getTournamentCreator().setCreatorVersion(getTournamentCreator().getCreatorVersion());
        } else {
            mergedTournament.getTournamentCreator().setCreatorName(committedTournament.getTournamentCreator().getCreatorName());
            mergedTournament.getTournamentCreator().setCreatorUsername(committedTournament.getTournamentCreator().getCreatorUsername());
            mergedTournament.getTournamentCreator().setCreatorVersion(committedTournament.getTournamentCreator().getCreatorVersion());
        }
    }

    /*private void mergeCourseExecution(Tournament committedTournament, Tournament mergedTournament) {
        if(getCourseExecution().getVersion() >= committedTournament.getCourseExecution().getVersion()) {
            mergedTournament.getCourseExecution().setVersion(getCourseExecution().getVersion());
        } else {
            mergedTournament.getCourseExecution().setVersion(committedTournament.getCourseExecution().getVersion());
        }
    }*/

    private void mergeCourseExecution(Tournament committedTournament, Tournament mergedTournament) {
        if (getTournamentCourseExecution().getCourseExecutionVersion() >= committedTournament.getTournamentCourseExecution().getCourseExecutionVersion()) {
            mergedTournament.getTournamentCourseExecution().setCourseExecutionVersion(getTournamentCourseExecution().getCourseExecutionVersion());
        } else {
            mergedTournament.getTournamentCourseExecution().setCourseExecutionVersion(committedTournament.getTournamentCourseExecution().getCourseExecutionVersion());
        }
    }

    private void mergeQuiz(Tournament committedTournament, Tournament mergedTournament) {
        // The quiz aggregate id must be set in case the quiz has been regenerated due to the previous having been invalidated
        if (getTournamentQuiz().getQuizVersion() >= committedTournament.getTournamentQuiz().getQuizVersion()) {
            mergedTournament.getTournamentQuiz().setQuizAggregateId(getTournamentQuiz().getQuizAggregateId());
            mergedTournament.getTournamentQuiz().setQuizVersion(getTournamentQuiz().getQuizVersion());
        } else {
            mergedTournament.getTournamentQuiz().setQuizVersion(committedTournament.getTournamentQuiz().getQuizAggregateId());
            mergedTournament.getTournamentQuiz().setQuizVersion(committedTournament.getTournamentQuiz().getQuizVersion());
        }

    }

    private void mergeCancelled(Set<String> toCommitVersionChangedFields, Tournament committedTournament, Tournament mergedTournament) {
        if (toCommitVersionChangedFields.contains("cancelled")) {
            mergedTournament.setCancelled(isCancelled());
        } else {
            mergedTournament.setCancelled(committedTournament.isCancelled());
        }
    }

    private void mergeNumberOfQuestions(Set<String> toCommitVersionChangedFields, Tournament committedTournament, Tournament mergedTournament) {
        if (toCommitVersionChangedFields.contains("numberOfQuestions")) {
            mergedTournament.setNumberOfQuestions(getNumberOfQuestions());
        } else {
            mergedTournament.setNumberOfQuestions(committedTournament.getNumberOfQuestions());
        }
    }

    private void mergeEndTime(Set<String> toCommitVersionChangedFields, Tournament committedTournament, Tournament mergedTournament) {
        if (toCommitVersionChangedFields.contains("endTime")) {
            mergedTournament.setEndTime(getEndTime());
        } else {
            mergedTournament.setEndTime(committedTournament.getEndTime());
        }
    }

    private void mergeStartTime(Set<String> toCommitVersionChangedFields, Tournament committedTournament, Tournament mergedTournament) {
        if (toCommitVersionChangedFields.contains("startTime")) {
            mergedTournament.setStartTime(getStartTime());
        } else {
            mergedTournament.setStartTime(committedTournament.getStartTime());
        }
    }

    private void mergeParticipants(Tournament prev, Tournament v1, Tournament v2, Tournament mergedTournament) {
        // Here we "calculate" the result of the incremental fields. This fields will always be the same regardless
        // of the base we choose.

        Set<TournamentParticipant> prevParticipantsPre = new HashSet<>(prev.getTournamentParticipants());
        Set<TournamentParticipant> v1ParticipantsPre = new HashSet<>(v1.getTournamentParticipants());
        Set<TournamentParticipant> v2ParticipantsPre = new HashSet<>(v2.getTournamentParticipants());

        CausalTournament.syncParticipantsVersions(prevParticipantsPre, v1ParticipantsPre, v2ParticipantsPre, prev.getTournamentCourseExecution().getCourseExecutionVersion(), v1.getTournamentCourseExecution().getCourseExecutionVersion(), v2.getTournamentCourseExecution().getCourseExecutionAggregateId());

        Set<TournamentParticipant> prevParticipants = new HashSet<>(prevParticipantsPre);
        Set<TournamentParticipant> v1Participants = new HashSet<>(v1ParticipantsPre);
        Set<TournamentParticipant> v2Participants = new HashSet<>(v2ParticipantsPre);


        Set<TournamentParticipant> addedParticipants =  SetUtils.union(
                SetUtils.difference(v1Participants, prevParticipants),
                SetUtils.difference(v2Participants, prevParticipants)
        );

        Set<TournamentParticipant> removedParticipants = SetUtils.union(
                SetUtils.difference(prevParticipants, v1Participants),
                SetUtils.difference(prevParticipants, v2Participants)
        );

        Set<TournamentParticipant> mergedParticipants = SetUtils.union(SetUtils.difference(prevParticipants, removedParticipants), addedParticipants);
        mergedTournament.setTournamentParticipants(mergedParticipants.stream().map(TournamentParticipant::new).collect(Collectors.toSet()));

    }

    private static void syncParticipantsVersions(Set<TournamentParticipant> prevParticipants,
                                                Set<TournamentParticipant> v1Participants,
                                                Set<TournamentParticipant> v2Participants,
                                                Integer prevCourseExecutionVersion,
                                                Integer v1CourseExecutionVersion,
                                                Integer v2CourseExecutionVersion) {

        for (TournamentParticipant tp1 : v1Participants) {
            for (TournamentParticipant tp2 : v2Participants) {
                if (tp1.getParticipantAggregateId().equals(tp2.getParticipantAggregateId())) {
                    if (v1CourseExecutionVersion > v2CourseExecutionVersion) {
                        tp2.setParticipantVersion(tp1.getParticipantVersion());
                        tp2.setParticipantName(tp1.getParticipantName());
                        tp2.setParticipantUsername(tp1.getParticipantUsername());
                        if (tp1.getParticipantAnswer() != null) {
                            tp2.setParticipantAnswer(new TournamentParticipantQuizAnswer(tp1.getParticipantAnswer()));
                        }
                    }

                    if (v2CourseExecutionVersion > v1CourseExecutionVersion) {
                        tp1.setParticipantVersion(tp2.getParticipantVersion());
                        tp1.setParticipantName(tp2.getParticipantName());
                        tp1.setParticipantUsername(tp2.getParticipantUsername());
                        if (tp2.getParticipantAnswer() != null) {
                            tp1.setParticipantAnswer(new TournamentParticipantQuizAnswer(tp2.getParticipantAnswer()));
                        }
                    }
                }
            }

            // no need to check again because the prev does not contain any newer version than v1 an v2
            for (TournamentParticipant prevParticipant : prevParticipants) {
                if (tp1.getParticipantAggregateId().equals(prevParticipant.getParticipantAggregateId())) {
                    if (v1CourseExecutionVersion > prevCourseExecutionVersion) {
                        prevParticipant.setParticipantVersion(tp1.getParticipantVersion());
                        prevParticipant.setParticipantName(tp1.getParticipantName());
                        prevParticipant.setParticipantUsername(tp1.getParticipantUsername());
                        if (tp1.getParticipantAnswer() != null) {
                            prevParticipant.setParticipantAnswer(new TournamentParticipantQuizAnswer(tp1.getParticipantAnswer()));
                        }
                    }

                    if (prevCourseExecutionVersion > v1CourseExecutionVersion) {
                        tp1.setParticipantVersion(prevParticipant.getParticipantVersion());
                        tp1.setParticipantName(prevParticipant.getParticipantName());
                        tp1.setParticipantUsername(prevParticipant.getParticipantUsername());
                        if (prevParticipant.getParticipantAnswer() != null) {
                            tp1.setParticipantAnswer(new TournamentParticipantQuizAnswer(prevParticipant.getParticipantAnswer()));
                        }
                    }
                }
            }
        }
    }

    private void mergeTopics(Set<String> toCommitVersionChangedFields, Tournament committedTournament, Tournament mergedTournament) {
        if (toCommitVersionChangedFields.contains("tournamentTopics")) {
            mergedTournament.setTournamentTopics(getTournamentTopics().stream().map(TournamentTopic::new).collect(Collectors.toSet()));
        } else {
            mergedTournament.setTournamentTopics(committedTournament.getTournamentTopics().stream().map(TournamentTopic::new).collect(Collectors.toSet()));
        }
    }

}
