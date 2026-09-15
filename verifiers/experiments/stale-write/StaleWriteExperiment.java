package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.experiments.stalewrite;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorBoundaryContext;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorFault;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorInjectedFaultException;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorProviderHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.InMemoryFaultVectorProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceNoopRecorder;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorder;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorderHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventReplayCoordinator;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.nio.file.Files;
import java.util.Objects;
import static pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.SagaReadExposureReport.*;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.*;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread.*;


import pt.ulisboa.tecnico.socialsoftware.quizzes.events.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.notification.handling.TournamentEventHandling;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.notification.handling.handlers.UpdateTopicEventHandler;

/** Controlled application experiment. No production detector or score is changed. */
public final class StaleWriteExperiment {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
    private final ConfigurableApplicationContext context;
    private final TransactionTemplate tx;
    private final EntityManager entityManager;
    private final EventService eventService;
    private final SagaUnitOfWorkService uowService;
    private final LocalCommandGateway gateway;
    private final RecordingRecorder recorder = new RecordingRecorder();
    private final List<Map<String,Object>> actions = new ArrayList<>(), checks = new ArrayList<>();
    private final Map<String,Object> report = new LinkedHashMap<>();
    private final Evidence evidence = new Evidence();
    private final String caseId;

    private StaleWriteExperiment(ConfigurableApplicationContext c,String id) {
        context=c;caseId=id;
        tx=new TransactionTemplate(c.getBean(PlatformTransactionManager.class));
        entityManager=c.getBean(EntityManager.class);eventService=c.getBean(EventService.class);
        uowService=c.getBean(SagaUnitOfWorkService.class);gateway=c.getBean(LocalCommandGateway.class);
        report.put("caseId",id);report.put("schemaVersion","stale-write-experiment.v1");
        report.put("actions",actions);report.put("checks",checks);report.put("observations",evidence.rows);
        report.put("serialized",Boolean.getBoolean("local.messaging.serialize"));
        report.put("scoreEvaluated",false);
    }

    public static void main(String[] args) throws Exception {
        if(args.length!=2 || !Set.of("forward-stale","forward-fresh","recovery-stale","recovery-delayed-event","recovery-no-event").contains(args[0]))
            throw new IllegalArgumentException("CASE OUTPUT.json required");
        Path out=Path.of(args[1]);if(Files.exists(out))throw new IllegalArgumentException("refusing overwrite");
        Files.createDirectories(out.getParent());
        System.setProperty("spring.profiles.active","test,sagas,local");
        System.setProperty("microservices.simulator.saga-read-exposure.enabled","true");
        System.setProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY,"true");
        StaleWriteExperiment e=null;
        try(var replay=EventReplayCoordinator.activate();var c=SpringApplication.run(QuizzesSimulator.class,
                "--verifiers.application.enabled=false","--server.port=0","--spring.main.banner-mode=off")) {
            e=new StaleWriteExperiment(c,args[0]);e.run();e.assertChecks();e.report.put("status","PASS");
        } catch(Throwable t) {
            if(e!=null){e.report.put("status","FAIL");e.report.put("failure",failure(t));}throw t;
        } finally {
            FaultVectorProviderHolder.clear();DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());
            if(e!=null)JSON.writerWithDefaultPrettyPrinter().writeValue(out.toFile(),e.report);
        }
        System.out.println("STALE_WRITE_PASS "+args[0]);
    }

    private void run() {
        Setup s=setup(true);eventService.clearEventsForReplay();
        report.put("identities",Map.of("tournament",s.tournamentId(),"quiz",s.quizId(),"topic",s.topic1Id()));
        report.put("initial",state(s));
        report.put("persistentInitial",context.getBean(PersistentStateObserver.class).snapshotAll());
        TournamentDto input=new TournamentDto();input.setAggregateId(s.tournamentId());
        input.setStartTime(DateHandler.toISOString(s.now().plusMinutes(65)));
        input.setEndTime(DateHandler.toISOString(s.now().plusMinutes(125)));input.setNumberOfQuestions(3);
        var a=uowService.createUnitOfWork(UpdateTournamentFunctionalitySagas.class.getSimpleName());
        var update=new UpdateTournamentFunctionalitySagas(uowService,input,
            Set.of(s.topic1Id(),s.topic2Id(),s.topic3Id()),a,gateway);
        boolean recovery=caseId.startsWith("recovery");
        EventReplayCoordinator.CapturedEvent event=null;
        try(var scope=ImpactEvidenceObserverHolder.install(evidence)) {
            step("A",update,a,"getOriginalTournamentStep",false,false);
            report.put("savedOriginalDto",JSON.convertValue(update.getOriginalTournamentDto(),Map.class));
            if(caseId.equals("forward-fresh")) {event=rename(s);deliver(s,event);}
            step("A",update,a,"getTopicsStep",false,false);
            report.put("cachedTopicDtos",JSON.convertValue(update.getTopicsDtos(),List.class));
            if(caseId.equals("forward-stale")) {event=rename(s);deliver(s,event);}
            report.put("beforeForwardWrite",state(s));
            step("A",update,a,"updateTournamentStep",false,false);
            report.put("afterForwardWrite",state(s));
            if(caseId.equals("recovery-stale") || caseId.equals("recovery-delayed-event")) {
                event=rename(s);
                if(caseId.equals("recovery-stale"))deliver(s,event);
            }
            step("A",update,a,"findQuestionsByTopicIds",false,false);
            step("A",update,a,"updateQuizStep",recovery,!recovery);
            report.put("beforeRecovery",state(s));
            if(recovery) {
                var cps=update.recoveryCheckpointsForExecutor(a);report.put("recoveryCheckpoints",cps);
                for(var cp:cps) {
                    recorder.clear();
                    try(var writer=ImpactWriterContext.enter(writer("A",update,cp.sourceStepName(),"RECOVERY"))) {
                        var r=update.recoverStepForExecutor(cp.sourceStepName(),a);
                        action("recover-"+cp.sourceStepName(),"SUCCESS",null,Map.of("recovery",r,"state",state(s)),recorder.drain());
                    }
                }
                check("all recovery checkpoints consumed",update.recoveryCheckpointsForExecutor(a).isEmpty(),Map.of());
            }
            report.put("afterRecovery",state(s));
            if(caseId.equals("recovery-delayed-event"))deliver(s,event);
            report.put("final",state(s));
            try(var excluded=ReadObservationContext.exclude("OBSERVER")) {
                report.put("persistentFinal",context.getBean(PersistentStateObserver.class).snapshotAll());
            }
            report.put("producerOutcome",recovery?"COMPENSATED":"COMMITTED");
            report.put("observerFailures",scope.drainFailures());
            report.put("readObserverFailures",scope.drainReadFailures());
        }
    }

    private EventReplayCoordinator.CapturedEvent rename(Setup s) {
        recorder.clear();
        TopicDto dto=new TopicDto();dto.setAggregateId(s.topic1Id());dto.setName("RENAMED TOPIC");
        try(var writer=ImpactWriterContext.enter(new ImpactEvidence.Writer("SAGA",caseId,"stale-write","B",
                    "B:rename","FORWARD","TopicFunctionalities","updateTopic",null));
            var capture=EventReplayCoordinator.beginTriggerCapture("B:rename")) {
            context.getBean(TopicFunctionalities.class).updateTopic(dto);
            var found=capture.capturedEvents().stream().filter(e->e.eventTypeFqn().equals(UpdateTopicEvent.class.getName())).toList();
            if(found.size()!=1)throw new IllegalStateException("Expected one exact UpdateTopicEvent");
            var event=found.getFirst();
            check("rename event published",event.published(),Map.of("id",event.eventId()));
            report.put("event",event);
            action("B:rename","COMMITTED",null,Map.of("state",state(s)),recorder.drain());
            return event;
        }
    }

    private void deliver(Setup s,EventReplayCoordinator.CapturedEvent event) {
        recorder.clear();
        try(var writer=ImpactWriterContext.enter(new ImpactEvidence.Writer("EVENT",caseId,"stale-write","C",
                    "C:deliver","EVENT",UpdateTopicEventHandler.class.getName(),"handleEvent",null));
            var selected=EventReplayCoordinator.beginSelectedEvent(event,UpdateTopicEvent.class.getName(),UpdateTopicEventHandler.class.getName())) {
            context.getBean(TournamentEventHandling.class).handleUpdateTopicEvents();
            selected.verifyCompleted();
            check("exact tournament receiver",Objects.equals(selected.subscriberAggregateId(),s.tournamentId()),Map.of());
            action("C:deliver","COMMITTED",null,Map.of("state",state(s)),recorder.drain());
            report.put("afterDelivery",state(s));
        }
    }

    private Map<String,Object> state(Setup s) {
        Map<String,Object> result=new LinkedHashMap<>(tournamentPair(s.tournamentId(),s.quizId()));
        result.put("sourceTopic",tx.execute(status->{entityManager.clear();Topic t=(Topic)latestInTx(s.topic1Id());
            return Map.of("name",t.getName(),"version",t.getVersion(),"aggregateId",t.getAggregateId());}));
        return result;
    }

    private static final class Evidence implements ImpactEvidenceObserver {
        final List<Map<String,Object>> rows=new ArrayList<>();
        void add(String kind,Object data){rows.add(Map.of("order",rows.size(),"kind",kind,"data",data));}
        public boolean isReadObservationEnabled(){return true;}
        public void committedWrite(ImpactEvidence.AggregateSnapshot a,ImpactEvidence.Writer w){add("WRITE",new ImpactEvidence.CommittedWrite(rows.size(),a,w));}
        public void readResponse(ReadResponseEvidence.Observation r){add("READ",r);}
        public void eventDelivery(ImpactEvidence.EventDelivery d){add("EVENT",d);}
        public void coverageGap(ImpactEvidence.CoverageGap g){add("GAP",g);}
    }

    private ImpactEvidence.Writer writer(String actor,WorkflowFunctionality fn,String step,String phase) {
        return new ImpactEvidence.Writer("SAGA",caseId,"stale-write",actor,actor+":"+phase+":"+step,
            phase,fn.getClass().getName(),step,null);
    }

    private void step(String actor,WorkflowFunctionality fn,SagaUnitOfWork uow,String name,boolean fault,boolean commit) {
        recorder.clear();
        var boundary=new FaultVectorBoundaryContext(caseId,"stale-write",actor,name+"#0",actions.size(),
            fn.getClass().getName(),fn.getClass().getSimpleName(),name,fault?1:0);
        try(var writer=ImpactWriterContext.enter(writer(actor,fn,name,"FORWARD"));
            var provider=FaultVectorProviderHolder.install(new InMemoryFaultVectorProvider(fault?Map.of(boundary.slotIndex(),FaultVectorFault.from(boundary)):Map.of()));
            var ignored=FaultVectorProviderHolder.enterBoundary(boundary)) {
            var result=fn.executeStepForExecutorControlled(name,uow);
            action(actor+":"+name,result.completed()?"SUCCESS":"FAULT",result.failure(),Map.of("boundary",boundary),recorder.drain());
            if(fault) {
                Throwable t=root(result.failure());
                check("exact injected pre-body fault",!result.completed() && t instanceof FaultVectorInjectedFaultException f
                    && f.getSlotIndex()==boundary.slotIndex() && f.getAssignedBit()==1
                    && boundary.scenarioExecutionId().equals(f.getScenarioExecutionId())
                    && boundary.scenarioPlanId().equals(f.getScenarioPlanId())
                    && boundary.sagaInstanceId().equals(f.getSagaInstanceId())
                    && boundary.scheduledStepId().equals(f.getScheduledStepId())
                    && boundary.runtimeStepName().equals(f.getRuntimeStepName())
                    && boundary.functionalityClassFqn().equals(f.getFunctionalityClassFqn())
                    && boundary.functionalityClassSimpleName().equals(f.getFunctionalityClassSimpleName()),failure(t));
            } else if(!result.completed())throw new IllegalStateException("Unexpected failure "+name,result.failure());
            if(commit) {
                var end=fn.finalizeForExecutor(uow);
                action(actor+":commit",end.committed()?"COMMITTED":"FAILED",end.failure(),Map.of(),recorder.drain());
                if(!end.committed())throw new IllegalStateException("Commit failed",end.failure());
            }
        }
    }

    private Setup setup(boolean withTournament) {
        DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());
        var executionFns = context.getBean(ExecutionFunctionalities.class);
        var userFns = context.getBean(UserFunctionalities.class);
        var topicFns = context.getBean(TopicFunctionalities.class);
        var questionFns = context.getBean(QuestionFunctionalities.class);
        var tournamentFns = context.getBean(TournamentFunctionalities.class);
        LocalDateTime now = LocalDateTime.parse(System.getProperty("experiment.fixtureNow",
                LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).toString()));
        CourseExecutionDto course = new CourseExecutionDto();
        course.setName("BLCM"); course.setType("TECNICO"); course.setAcronym("TESTBLCM");
        course.setAcademicTerm("2022/2023"); course.setEndDate(DateHandler.toISOString(now.plusDays(1)));
        course = executionFns.createCourseExecution(course);
        UserDto creator = new UserDto(); creator.setName("CREATOR"); creator.setUsername("creator"); creator.setRole("STUDENT");
        creator = userFns.createUser(creator); userFns.activateUser(creator.getAggregateId());
        executionFns.addStudent(course.getAggregateId(), creator.getAggregateId());
        TopicDto t1 = topic("TOPIC 1", course.getCourseAggregateId(), topicFns);
        TopicDto t2 = topic("TOPIC 2", course.getCourseAggregateId(), topicFns);
        TopicDto t3 = topic("TOPIC 3", course.getCourseAggregateId(), topicFns);
        question("Question 1", "Content 1", course.getCourseAggregateId(), t1, questionFns);
        question("Question 2", "Content 2", course.getCourseAggregateId(), t2, questionFns);
        question("Question 3", "Content 3", course.getCourseAggregateId(), t3, questionFns);
        Integer tournamentId = null, quizId = null;
        if (withTournament) {
            TournamentDto input = new TournamentDto();
            input.setStartTime(DateHandler.toISOString(now.plusMinutes(5)));
            input.setEndTime(DateHandler.toISOString(now.plusHours(1)));
            input.setNumberOfQuestions(2);
            TournamentDto made = tournamentFns.createTournament(creator.getAggregateId(), course.getAggregateId(),
                    List.of(t1.getAggregateId(), t2.getAggregateId()), input);
            tournamentId = made.getAggregateId(); quizId = made.getQuiz().getAggregateId();
        }
        DynamicEvidenceRecorderHolder.setRecorder(recorder);
        return new Setup(course.getAggregateId(), creator.getAggregateId(), t1.getAggregateId(), t2.getAggregateId(), t3.getAggregateId(), tournamentId, quizId, now);
    }

    private static TopicDto topic(String name, int courseId, TopicFunctionalities fns) {
        TopicDto dto = new TopicDto(); dto.setName(name); return fns.createTopic(courseId, dto);
    }

    private static void question(String title, String content, int courseId, TopicDto topic, QuestionFunctionalities fns) {
        QuestionDto dto = new QuestionDto(); dto.setTitle(title); dto.setContent(content); dto.setTopicDto(Set.of(topic));
        OptionDto a = new OptionDto(); a.setSequence(1); a.setCorrect(true); a.setContent("A");
        OptionDto b = new OptionDto(); b.setSequence(2); b.setCorrect(false); b.setContent("B");
        dto.setOptionDtos(List.of(a, b)); fns.createQuestion(courseId, dto);
    }

    private Map<String, Object> tournamentPair(int tournamentId, int quizId) {
        return tx.execute(status -> {
            entityManager.flush(); entityManager.clear();
            Tournament t = (Tournament) latestInTx(tournamentId);
            Quiz q = (Quiz) latestInTx(quizId);
            List<Integer> topicIds = t.getTournamentTopics().stream().map(v -> v.getTopicAggregateId()).sorted().toList();
            List<Map<String, Object>> topics = t.getTournamentTopics().stream()
                    .sorted(Comparator.comparing(v -> v.getTopicAggregateId()))
                    .map(v -> Map.<String, Object>of("topicId", v.getTopicAggregateId(), "topicVersion", v.getTopicVersion(),
                            "name", v.getTopicName(), "state", v.getState().name())).toList();
            List<Map<String, Object>> questions = q.getQuizQuestions().stream()
                    .sorted(Comparator.comparing(QuizQuestion::getQuestionAggregateId))
                    .map(v -> Map.<String, Object>of("questionId", v.getQuestionAggregateId(), "questionVersion", v.getQuestionVersion(),
                            "title", v.getTitle(), "content", v.getContent(), "state", v.getState().name())).toList();
            Map<String, Object> tournamentData = new LinkedHashMap<>();
            tournamentData.put("numberOfQuestions", t.getNumberOfQuestions());
            tournamentData.put("startTime", t.getStartTime().toString());
            tournamentData.put("endTime", t.getEndTime().toString());
            tournamentData.put("topicIds", topicIds);
            tournamentData.put("topics", topics);
            tournamentData.put("quizReference", Map.of("quizId", t.getTournamentQuiz().getQuizAggregateId(),
                    "quizVersion", t.getTournamentQuiz().getQuizVersion()));
            Map<String, Object> pair = new LinkedHashMap<>();
            pair.put("tournament", aggregateEnvelope(t, tournamentData));
            pair.put("quiz", aggregateEnvelope(q, Map.of("questionCount", q.getQuizQuestions().size(),
                    "availableDate", q.getAvailableDate().toString(), "conclusionDate", q.getConclusionDate().toString(),
                    "questions", questions)));
            return pair;
        });
    }

    private Map<String, Object> aggregateEnvelope(Aggregate aggregate, Map<String, Object> business) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("aggregateId", aggregate.getAggregateId()); out.put("version", aggregate.getVersion());
        out.put("previousVersion", aggregate.getPrev() == null ? null : aggregate.getPrev().getVersion());
        out.put("state", aggregate.getState().name());
        out.put("sagaState", aggregate instanceof SagaAggregate saga && saga.getSagaState() != null ? saga.getSagaState().getStateName() : null);
        out.putAll(business); return out;
    }

    private Aggregate latestInTx(int id) {
        List<Aggregate> rows = entityManager.createQuery("select a from Aggregate a where a.aggregateId=:id order by a.version desc", Aggregate.class)
                .setParameter("id", id).setMaxResults(1).getResultList();
        if (rows.isEmpty()) throw new IllegalStateException("aggregate not found " + id);
        return rows.get(0);
    }

    private void action(String name, String status, Throwable failure, Map<String, Object> details, List<Map<String, Object>> evidence) {
        Map<String, Object> row = new LinkedHashMap<>(); row.put("index", actions.size()); row.put("name", name); row.put("status", status);
        row.put("failure", failure(failure)); row.put("details", details); row.put("dynamicEvidence", evidence); actions.add(row);
    }

    private void check(String name, boolean passed, Object evidence) {
        checks.add(Map.of("name", name, "passed", passed, "evidence", evidence));
    }

    private void assertChecks() {
        List<String> failed = checks.stream().filter(c -> !Boolean.TRUE.equals(c.get("passed"))).map(c -> (String) c.get("name")).toList();
        if (!failed.isEmpty()) throw new IllegalStateException("failed checks: " + failed);
    }

    private static Map<String, Object> failure(Throwable failure) {
        if (failure == null) return Map.of();
        Throwable root = root(failure);
        return Map.of("class", root.getClass().getName(), "message", String.valueOf(root.getMessage()));
    }


    private static Throwable root(Throwable value) {
        if(value==null)return null;
        while(value.getCause()!=null && value.getCause()!=value)value=value.getCause();
        return value;
    }
    @SuppressWarnings("unchecked")
    private static Map<String,Object> castMap(Object v){return (Map<String,Object>)v;}
    private record Setup(int courseId,int creatorId,int topic1Id,int topic2Id,int topic3Id,
                         int tournamentId,int quizId,LocalDateTime now){}
    private static final class RecordingRecorder implements DynamicEvidenceRecorder {
        private final List<DynamicEvidenceEvent> events = new ArrayList<>();
        public boolean isEnabled() { return true; }
        public synchronized void record(DynamicEvidenceEvent event) { events.add(event); }
        public void close() {}
        synchronized void clear() { events.clear(); }
        private static Throwable root(Throwable value) {
        if(value==null)return null;
        while(value.getCause()!=null && value.getCause()!=value)value=value.getCause();
        return value;
    }
    @SuppressWarnings("unchecked")
        synchronized List<Map<String, Object>> drain() {
            List<Map<String, Object>> out = events.stream().map(e -> (Map<String, Object>) JSON.convertValue(e, Map.class)).toList();
            events.clear(); return out;
        }
    }
}
