package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.ApplicationsFileTreeParser
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.EagerFaultScenarioGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.InputTupleJoiner
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.RecoveryScheduleCap
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioIdGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.SetupPlanValidator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ApplicationAnalysisScenarioModelAdapter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FootprintConfidence
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioActionKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupValueKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadGenerationResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.WorkloadPlanValidator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ExecutableArtifactWriter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ScenarioCatalogPackageReader
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceIndex
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchPhase

import java.nio.file.Files
import java.nio.file.Path
import spock.lang.TempDir

class SourceDerivedSharedSagaWorkloadAnalysisSpec extends VisitorTestSupport {

    @TempDir
    Path tempDir

    private static final String TARGET_TEST =
            'pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.behaviour.RemoveTournamentAddParticipantRecoveryWindowExploratoryTest'
    private static final String REMOVE =
            'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveTournamentFunctionalitySagas'
    private static final String ADD =
            'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantFunctionalitySagas'
    private static final String CREATE_USER =
            'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.sagas.CreateUserFunctionalitySagas'
    private static final String UPDATE_STUDENT_TEST =
            'pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.behaviour.execution.UpdateStudentNameFaultTest'
    private static final String ADD_STUDENT =
            'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.AddStudentFunctionalitySagas'
    private static final String GET_EXECUTION =
            'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.GetCourseExecutionByIdFunctionalitySagas'
    private static final String UPDATE_STUDENT_NAME =
            'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.UpdateStudentNameFunctionalitySagas'
    private static final String START_QUIZ_TEST =
            'pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.behaviour.CreateTournamentStartQuizRecoveryWindowExploratoryTest'
    private static final String START_QUIZ =
            'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.StartQuizFunctionalitySagas'
    private static final String CREATE_QUESTION =
            'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.CreateQuestionFunctionalitySagas'
    private static final String CREATE_QUIZ =
            'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.CreateQuizFunctionalitySagas'
    private static final String FIND_TOURNAMENT_TEST =
            'pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament.FindTournamentTest'
    private static final String FIND_TOURNAMENT =
            'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.FindTournamentFunctionalitySagas'
    private static final String ADD_PARTICIPANT_FEATURE_TEST =
            'pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament.AddParticipantAndCreateTournamentTest'
    private static final String REMOVE_STUDENT_TEST =
            'pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.RemoveStudentFromCourseExecutionTest'
    private static final String REMOVE_STUDENT =
            'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.RemoveStudentFromCourseExecutionFunctionalitySagas'

    def 'unmodified Remove Add test proves one shared Tournament producer with semantic key footprints'() {
        given:
        Path applicationRoot = resolveProjectPath('applications', 'quizzes')
        configureQuizzesParser(applicationRoot)
        def parser = new ApplicationsFileTreeParser()
        parser.parse(applicationRoot)
        def javaSources = parser.getJavaFilePathsForApplication(applicationRoot.parent, 'quizzes').values()
        def state = new ApplicationAnalysisState()

        and: 'the normal Java analysis phases run over Quizzes source'
        def indexVisitor = new CommandHandlerIndexVisitor()
        javaSources.each { path -> indexVisitor.visit(StaticJavaParser.parse(path), state) }
        def serviceVisitor = new ServiceVisitor()
        javaSources.each { path -> serviceVisitor.visit(StaticJavaParser.parse(path), state) }
        def commandVisitor = new CommandHandlerVisitor()
        javaSources.each { path -> commandVisitor.visit(StaticJavaParser.parse(path), state) }
        def workflowVisitor = new WorkflowFunctionalityVisitor()
        javaSources.each { path -> workflowVisitor.visit(StaticJavaParser.parse(path), state) }
        def creationSiteVisitor = new WorkflowFunctionalityCreationSiteVisitor()
        javaSources.each { path -> creationSiteVisitor.visit(StaticJavaParser.parse(path), state) }

        and: 'the checked-in ordinary test is parsed in place, without a verifier copy or annotation'
        Path targetSource = applicationRoot.resolve(
                'src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/behaviour/RemoveTournamentAddParticipantRecoveryWindowExploratoryTest.groovy')
        assert Files.readString(targetSource).contains('private Map newRemoveTournament()')
        def groovyIndex = new GroovySourceIndex()
        groovyIndex.parse(applicationRoot.resolve('src/test/groovy'))
        new GroovyConstructorInputTraceVisitor().visit(groovyIndex, state)

        when:
        def targetTraces = state.groovyFullTraceResults.findAll {
            it.sourceClassFqn == TARGET_TEST && it.sagaClassFqn in [REMOVE, ADD]
        }
        def removeSaga = state.sagas.find { it.fqn == REMOVE }
        def addSaga = state.sagas.find { it.fqn == ADD }
        def tournamentDispatches = (removeSaga.steps + addSaga.steps)
                *.dispatches.flatten().findAll { it.aggregateName() == 'Tournament' }
        def keyInputs = state.sourceAggregateKeyInputEvidence().findAll {
            it.sourceClassFqn() == TARGET_TEST && it.sagaFqn() in [REMOVE, ADD] &&
                    it.aggregateName() == 'Tournament' && it.constructorArgumentIndex() == 1
        }
        def supportedPairs = state.sourceSupportedSagaPairs().findAll {
            [it.left().sagaFqn(), it.right().sagaFqn()] as Set == [REMOVE, ADD] as Set &&
                    it.left().sourceClassFqn() == TARGET_TEST && it.right().sourceClassFqn() == TARGET_TEST
        }
        def adapted = new ApplicationAnalysisScenarioModelAdapter().adapt(state)
        def targetDefinitions = adapted.sagaDefinitions().findAll { it.sagaFqn() in [REMOVE, ADD] }
        def analysisLimitations = state.sagas.collectMany { saga ->
            saga.steps.collectMany { step -> step.analysisDiagnostics.collect { diagnostic ->
                [saga.fqn, step.name, diagnostic.phase(), diagnostic.code()]
            } }
        }.sort { left, right -> left.toString() <=> right.toString() }
        def genericCompensations = state.sagas*.steps.flatten()*.dispatches.flatten().findAll {
            it.phase() == DispatchPhase.COMPENSATION &&
                    it.commandTypeFqn() == 'pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command'
        }
        def semanticStateWrites = state.sagas*.steps.flatten()*.dispatches.flatten().findAll {
            it.commandTypeFqn() ==
                    'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand'
        }
        def genericCompensationSignatures = genericCompensations.collect {
            "${it.stepKey()}|${it.aggregateName()}|${it.aggregateKeyText()}".toString()
        } as Set

        and: 'the real RemoveStudent fixture exposes a setup AddStudent target and a later feature target'
        def setupAddStudentInput = adapted.inputVariants().find {
            it.sourceClassFqn() == REMOVE_STUDENT_TEST && it.callContextMethodName() == 'setup' &&
                    it.sagaFqn() == ADD_STUDENT
        }
        def removeStudentInput = adapted.inputVariants().find {
            it.sourceClassFqn() == REMOVE_STUDENT_TEST && it.callContextMethodName() == 'remove student successfully' &&
                    it.sagaFqn() == REMOVE_STUDENT
        }
        def setupAddStudentBinding = adapted.sourceSetupPlanBindings().find { binding ->
            binding.featureDerived() && binding.featureMethodName() == 'setup' &&
                    setupAddStudentInput?.deterministicId() in binding.inputVariantIds()
        }

        then: 'both map-returning helper inputs are recovered for every two-Saga feature in the ordinary test'
        targetTraces.count { it.sagaClassFqn == REMOVE } == 5
        targetTraces.count { it.sagaClassFqn == ADD } == 4
        targetTraces.every { it.sourceBindingName in ['remove', 'add'] }

        and: 'the strict prefix prepares both the setup target and the later feature without replaying the target'
        setupAddStudentInput != null
        removeStudentInput != null
        setupAddStudentBinding?.featureDerived()
        setupAddStudentBinding.featureMethodName() == 'setup'
        setupAddStudentBinding.setupPlan().actions()*.methodKey().every { !it.contains('#addStudent(') }
        def removeStudentTupleSetup = ScenarioGenerator.setupPlanFor(new InputTupleJoiner.InputTuple(
                [setupAddStudentInput, removeStudentInput], 'remove-student-fixture-tuple', []),
                adapted.sourceSetupPlanBindings())
        removeStudentTupleSetup != null
        removeStudentTupleSetup.actions()*.methodKey().every { !it.contains('#addStudent(') }
        removeStudentTupleSetup.participantBindings()*.inputVariantId.toSet().containsAll([
                setupAddStudentInput.deterministicId(), removeStudentInput.deterministicId()
        ])

        and: 'every Tournament command footprint maps its aggregate key to zero-based constructor argument 1'
        tournamentDispatches.size() == 4
        tournamentDispatches.every { it.aggregateKeyConstructorArgumentIndex() == 1 }

        and: 'both participant arg-1 records retain the exact same createTournament occurrence and aggregateId path'
        keyInputs*.producerReference*.producerMethodName.toSet() == ['createTournament'] as Set
        keyInputs*.producerReference*.propertyPath.toSet() == [['aggregateId']] as Set
        keyInputs*.producerReference*.occurrenceId.toSet().size() == 1
        keyInputs*.producerReference*.occurrenceId.first().contains("${TARGET_TEST}:73:")
        supportedPairs.size() == 20
        supportedPairs.every {
            it.aggregateName() == 'Tournament' &&
                    it.left().producerReference() == it.right().producerReference()
        }

        and: 'the semantic command path and source-supported join describe the same Tournament key'
        tournamentDispatches.every { it.aggregateKeyText() == 'tournamentAggregateId' }
        targetDefinitions*.steps.flatten()*.footprints.flatten()
                .findAll { it.aggregateKey()?.aggregateName() == 'Tournament' }
                .every { it.aggregateKey().confidence() == FootprintConfidence.SYMBOLIC }

        and: 'wrapper plumbing is transparent and only genuinely unsupported Quizzes shapes remain limited'
        genericCompensations.size() == 26
        genericCompensationSignatures == [
                'AnonymizeStudentFunctionalitySagas::getCourseExecutionStep|Execution|executionAggregateId',
                'AnswerQuestionFunctionalitySagas::getQuestionStep|Question|this.questionDto.getAggregateId()',
                'AnswerQuestionFunctionalitySagas::getQuizAnswerStep|Quiz|this.quizAnswer.getAggregateId()',
                'CancelTournamentFunctionalitySagas::getTournamentStep|Tournament|tournamentAggregateId',
                'ConcludeQuizFunctionalitySagas::getQuizAnswerStep|QuizAnswer|this.quizAnswer.getAggregateId()',
                'CreateQuestionFunctionalitySagas::getCourseStep|Course|courseDto.getAggregateId()',
                'CreateQuestionFunctionalitySagas::getTopicsStep|Topic|topicDto.getAggregateId()',
                'CreateQuizFunctionalitySagas::getCourseExecutionStep|Execution|courseExecutionId',
                'CreateQuizFunctionalitySagas::getQuestionsStep|Question|questionDto.getAggregateId()',
                'CreateTopicFunctionalitySagas::getCourseStep|Course|courseDto.getAggregateId()',
                'DeleteTopicFunctionalitySagas::getTopicStep|Topic|topicAggregateId',
                'DeleteUserFunctionalitySagas::getUserStep|User|userAggregateId',
                'FindParticipantFunctionalitySagas::getTournamentStep|Tournament|tournamentAggregateId',
                'LeaveTournamentFunctionalitySagas::getOldTournamentStep|Tournament|tournamentAggregateId',
                'RemoveCourseExecutionFunctionalitySagas::getCourseExecutionStep|Execution|executionAggregateId',
                'RemoveQuestionFunctionalitySagas::getQuestionStep|Question|questionAggregateId',
                'RemoveStudentFromCourseExecutionFunctionalitySagas::getOldCourseExecutionStep|Execution|courseExecutionAggregateId',
                'UpdateQuestionFunctionalitySagas::getQuestionStep|Question|question.getAggregateId()',
                'UpdateQuestionTopicsAsyncFunctionalitySagas::getQuestionAsyncStep|Question|this.question.getAggregateId()',
                'UpdateQuestionTopicsAsyncFunctionalitySagas::getTopicsAsyncStep|Topic|topicId',
                'UpdateQuestionTopicsFunctionalitySagas::getQuestionStep|Question|question.getAggregateId()',
                'UpdateQuestionTopicsFunctionalitySagas::getTopicsStep|Topic|topicId',
                'UpdateQuizFunctionalitySagas::getQuizStep|Quiz|quiz.getAggregateId()',
                'UpdateTopicFunctionalitySagas::getTopicStep|Topic|topic.getAggregateId()',
                'UpdateUserNameFunctionalitySagas::getParticipantStep|User|userAggregateId',
                'UpdateUserNameFunctionalitySagas::getTournamentStep|Tournament|tournamentAggregateId'
        ] as Set
        genericCompensations.every {
            it.aggregateKeyConfidence()?.name() == 'SYMBOLIC'
        }

        and: 'each exact semantic-lock setter contributes a same-target write regardless of state enum value'
        semanticStateWrites.size() == 64
        semanticStateWrites.count { it.phase() == DispatchPhase.FORWARD } == 38
        semanticStateWrites.count { it.phase() == DispatchPhase.COMPENSATION } == 26
        semanticStateWrites.every {
            it.accessPolicy().name() == 'WRITE' && it.aggregateName() && it.aggregateKeyText()
        }
        removeSaga.steps.find { it.name == 'getTournamentStep' }.dispatches*.accessPolicy()*.name() ==
                ['READ', 'WRITE']
        targetDefinitions.find { it.sagaFqn() == REMOVE }.steps()
                .find { it.name() == 'getTournamentStep' }.compensationEvidence().name() ==
                'IMPLICIT_SAGA_ROLLBACK'

        and: 'the non-identical ANSWER service token follows its handler to QuizAnswer rather than capitalization'
        Files.readString(applicationRoot.resolve(
                'src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/answer/coordination/sagas/ConcludeQuizFunctionalitySagas.java'))
                .contains('ServiceMapping.ANSWER.getServiceName(), this.quizAnswer.getAggregateId()')
        Files.readString(applicationRoot.resolve(
                'src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/answer/messaging/AnswerCommandHandler.java'))
                .contains('return "QuizAnswer";')
        genericCompensationSignatures.contains(
                'ConcludeQuizFunctionalitySagas::getQuizAnswerStep|QuizAnswer|this.quizAnswer.getAggregateId()')
        analysisLimitations == [
                ['pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantAsyncFunctionalitySagas',
                 'addParticipantStep', DispatchPhase.FORWARD, 'UNRESOLVED_COMMAND_PAYLOAD'],
                ['pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateTournamentFunctionalitySagas',
                 'updateQuizStep', DispatchPhase.FORWARD, 'UNRESOLVED_COMMAND_DISPATCH']
        ]

        and: 'the exact straight-line setup is persisted once in source order, including void effects'
        def targetInputIdsBySaga = [
                (REMOVE): adapted.inputVariants().findAll { it.sagaFqn() == REMOVE && it.sourceClassFqn() == TARGET_TEST }*.deterministicId as Set,
                (ADD): adapted.inputVariants().findAll { it.sagaFqn() == ADD && it.sourceClassFqn() == TARGET_TEST }*.deterministicId as Set
        ]
        def targetSetupBindings = adapted.sourceSetupPlanBindings().findAll { binding ->
            def ids = binding.inputVariantIds() as Set
            ids.any { it in targetInputIdsBySaga[REMOVE] } && ids.any { it in targetInputIdsBySaga[ADD] }
        }
        assert targetSetupBindings.size() == 1
        def setupBinding = targetSetupBindings.first()
        assert setupBinding != null: adapted.diagnostics().findAll { it.contains('source setup') }
        def setup = setupBinding.setupPlan()
        assert setup.actions().size() == 12: setup.actions().collect {
            [it.orderIndex(), it.methodKey(), it.sourceOccurrence(), it.blockers(), it.arguments()*.blockers()]
        }
        setup.actions()*.methodKey()*.split('#')*.last()*.split('\\(')*.first() == [
                'createCourseExecution', 'createUser', 'activateUser', 'createUser', 'activateUser',
                'addStudent', 'addStudent', 'createTopic', 'createTopic', 'createQuestion',
                'createQuestion', 'createTournament'
        ]
        setup.actions()*.methodKey().toSet() == [
                'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities#createCourseExecution(pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto):pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto',
                'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities#createUser(pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto):pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto',
                'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities#activateUser(java.lang.Integer):void',
                'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities#addStudent(java.lang.Integer,java.lang.Integer):void',
                'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities#createTopic(java.lang.Integer,pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto):pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto',
                'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities#createQuestion(java.lang.Integer,pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto):pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto',
                'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities#createTournament(java.lang.Integer,java.lang.Integer,java.util.List<java.lang.Integer>,pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto):pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto'
        ] as Set
        setup.actions().count { it.voidResult() } == 4
        setup.actions()*.sourceOccurrence().toSet().size() == 12
        setup.actions().every { action ->
            state.sagaCreationSites.any { site -> site.methodKey() == action.methodKey() }
        }
        new SetupPlanValidator().validate(setup).valid()

        and: 'an unrelated nested-property participant remains outside whole-result binding scope'
        def coveredSetupInputIds = adapted.sourceSetupPlanBindings()*.inputVariantIds().flatten() as Set
        def rejectedUnboundInput = adapted.inputVariants().find {
            it.sourceClassFqn() == START_QUIZ_TEST && it.sagaFqn() == START_QUIZ
        }
        rejectedUnboundInput != null
        !(rejectedUnboundInput.deterministicId() in coveredSetupInputIds)

        and: 'all accepted nested setup targets use exact pre-target prefixes'
        def inputsById = adapted.inputVariants().collectEntries { [(it.deterministicId()): it] }
        def nestedTargetBindings = adapted.sourceSetupPlanBindings().findAll {
            it.featureDerived() && it.featureMethodName() == 'setup' && it.inputVariantIds().size() == 1
        }
        def nestedSetupCohorts = [
                (CREATE_QUESTION): nestedTargetBindings.findAll { binding ->
                    binding.inputVariantIds().any { inputsById[it]?.sagaFqn() == CREATE_QUESTION }
                },
                (CREATE_QUIZ): nestedTargetBindings.findAll { binding ->
                    binding.inputVariantIds().any { inputsById[it]?.sagaFqn() == CREATE_QUIZ }
                }
        ]
        nestedSetupCohorts[CREATE_QUESTION]*.inputVariantIds().flatten().toSet().size() == 88
        nestedSetupCohorts[CREATE_QUIZ]*.inputVariantIds().flatten().toSet().size() == 4
        nestedSetupCohorts.every { saga, bindings ->
            bindings*.inputVariantIds().flatten().toSet().count { inputId ->
                inputsById[inputId].sagaFqn() == saga &&
                        inputsById[inputId].sourceClassFqn() ==
                        'pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.answer.RemoveCourseExecutionQuizAnswerReceiverTest'
            } == 1
        }
        nestedSetupCohorts.values().flatten().every { binding ->
            def inputId = binding.inputVariantIds().first()
            new SetupPlanValidator().validate(binding.setupPlan()).valid() &&
                    binding.targetOccurrencesByInputVariantId()[inputId]?.size() == 1 &&
                    !binding.setupPlan().actions()*.sourceOccurrence().contains(
                            binding.targetOccurrencesByInputVariantId()[inputId].first()) &&
                    binding.setupPlan().participantBindings().findAll {
                        it.inputVariantId() == inputId
                    }*.value().any { value -> referencedActionIds([value]) }
        }

        and: 'all setup and participant result references point backward to the exact producers'
        setup.actions().each { action ->
            referencedActionIds(action.arguments()*.value()).every { ref ->
                setup.actions().find { it.actionId() == ref }.orderIndex() < action.orderIndex()
            }
        }
        def tournamentBindings = setup.participantBindings().findAll {
            it.inputVariantId() in (targetInputIdsBySaga[REMOVE] + targetInputIdsBySaga[ADD]) &&
                    it.argumentIndex() == 1
        }
        tournamentBindings*.value*.kind.toSet() == [SetupValueKind.ACTION_RESULT_PROPERTY] as Set
        tournamentBindings*.value*.actionId.toSet() == ['setup-action-12'] as Set
        tournamentBindings*.value*.propertyName.toSet() == ['aggregateId'] as Set
        setup.participantBindings().find { it.inputVariantId() in adapted.inputVariants().findAll { it.sagaFqn() == ADD }*.deterministicId && it.argumentIndex() == 2 }
                .value().actionId() == 'setup-action-1'
        setup.participantBindings().find { it.inputVariantId() in adapted.inputVariants().findAll { it.sagaFqn() == ADD }*.deterministicId && it.argumentIndex() == 3 }
                .value().actionId() == 'setup-action-4'

        and: 'ordinary DTO collections and direct facade results make existing Quizzes setup reusable'
        def coveredSetupInputIdsNow = adapted.sourceSetupPlanBindings()*.inputVariantIds().flatten() as Set
        def findTournamentInput = adapted.inputVariants().find {
            it.sourceClassFqn() == FIND_TOURNAMENT_TEST && it.sagaFqn() == FIND_TOURNAMENT &&
                    it.sourceMethodName() == 'find tournament successfully'
        }
        def startQuizInput = adapted.inputVariants().find {
            it.sourceClassFqn() == 'pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.answer.StartQuizTest' &&
                    it.sagaFqn() == START_QUIZ &&
                    it.sourceMethodName() == 'student can start a quiz they have not started before'
        }
        findTournamentInput != null
        startQuizInput != null
        findTournamentInput.deterministicId() in coveredSetupInputIdsNow
        startQuizInput.deterministicId() in coveredSetupInputIdsNow
        def findBinding = adapted.sourceSetupPlanBindings()*.setupPlan()*.participantBindings().flatten().find {
            it.inputVariantId() == findTournamentInput.deterministicId() && it.argumentIndex() == 1
        }
        findBinding.value().kind() == SetupValueKind.ACTION_RESULT_PROPERTY
        findBinding.value().propertyName() == 'aggregateId'
        !adapted.diagnostics().any {
            it.contains('UNSUPPORTED_LOCAL_DATE_EXPRESSION:Arrays.asList')
        }

        and: 'an exact feature prefix supplies the later add-participant target without replaying it'
        def addParticipantFeature = 'create add participant successfully'
        def addParticipantInput = adapted.inputVariants().find {
            it.sourceClassFqn() == ADD_PARTICIPANT_FEATURE_TEST &&
                    it.callContextMethodName() == addParticipantFeature && it.sagaFqn() == ADD &&
                    it.stableSourceText().startsWith('tournamentFunctionalities.addParticipant(')
        }
        assert addParticipantInput != null
        def addParticipantTrace = state.groovyFullTraceResults.find {
            it.sourceClassFqn() == ADD_PARTICIPANT_FEATURE_TEST &&
                    it.callContextMethodName() == addParticipantFeature && it.sagaClassFqn() == ADD &&
                    it.sourceExpressionText().startsWith('tournamentFunctionalities.addParticipant(')
        }
        def featureBinding = adapted.sourceSetupPlanBindings().find {
            it.featureDerived() && it.sourceClassFqn() == ADD_PARTICIPANT_FEATURE_TEST &&
                    it.featureMethodName() == addParticipantFeature &&
                    it.frontierOccurrenceId() == addParticipantTrace.occurrence().occurrenceId() &&
                    addParticipantInput.deterministicId() in it.inputVariantIds()
        }
        assert featureBinding != null: adapted.diagnostics().findAll {
            it.contains(ADD_PARTICIPANT_FEATURE_TEST) && it.contains(addParticipantFeature)
        }
        featureBinding.setupPlan().actions()*.methodKey()*.split('#')*.last()*.split('\\(')*.first().takeRight(3) ==
                ['createUser', 'activateUser', 'addStudent']
        def featureCreateUser = featureBinding.setupPlan().actions().takeRight(3).first()
        featureCreateUser.arguments().first().value().assignments()*.propertyName() ==
                ['name', 'username', 'role']
        featureCreateUser.arguments().first().value().assignments()*.value()*.literalValue() ==
                ['NewUser', 'NewUsername', 'STUDENT']
        !featureBinding.setupPlan().actions()*.sourceOccurrence()
                .contains(addParticipantTrace.occurrence().occurrenceId())
        featureBinding.targetOccurrencesByInputVariantId()[addParticipantInput.deterministicId()] ==
                [addParticipantTrace.occurrence().occurrenceId()]
        def actionTraceByOccurrence = state.groovyFacadeSetupActionTraces.collectEntries {
            [(it.sourceOccurrence()): it]
        }
        featureBinding.setupPlan().actions().every {
            actionTraceByOccurrence[it.sourceOccurrence()].callContextMethodName() in ['setup', addParticipantFeature]
        }
        def foreignFeatureAction = state.groovyFacadeSetupActionTraces.find {
            it.sourceClassFqn() != ADD_PARTICIPANT_FEATURE_TEST &&
                    it.methodName() == 'createUser' && it.callContextMethodName() != 'setup'
        }
        foreignFeatureAction != null
        !featureBinding.setupPlan().actions()*.sourceOccurrence().contains(foreignFeatureAction.sourceOccurrence())
        def featureParticipantBinding = featureBinding.setupPlan().participantBindings().find {
            it.inputVariantId() == addParticipantInput.deterministicId() && it.argumentIndex() == 3
        }
        featureParticipantBinding.value().kind() == SetupValueKind.ACTION_RESULT_PROPERTY
        featureParticipantBinding.value().propertyName() == 'aggregateId'
        ScenarioGenerator.setupPlanFor(new InputTupleJoiner.InputTuple(
                [addParticipantInput], addParticipantInput.deterministicId(), []),
                adapted.sourceSetupPlanBindings()) == featureBinding.setupPlan()

        when: 'ordinary deterministic enumeration is given all ten dependency-preserving orders'
        def selectedInputIds = [
                setupBinding.inputVariantIds().find { it in targetInputIdsBySaga[REMOVE] },
                setupBinding.inputVariantIds().find { it in targetInputIdsBySaga[ADD] }
        ] as Set
        assert !selectedInputIds.contains(null)
        def targetInputs = adapted.inputVariants().findAll { it.deterministicId() in selectedInputIds }
        def config = new ScenarioGeneratorConfig(true,
                ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,
                false, 2, 100, 10, 10, false,
                ScenarioGeneratorConfig.InputPolicy.ALLOW_UNRESOLVED,
                ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING,
                1234L)
        def generated = ScenarioGenerator.generate(targetDefinitions, targetInputs, [], [setupBinding], config)
        def serial = generated.workloadPlans().find { plan ->
            plan.forwardSchedule()*.runtimeStepName() == [
                    'getTournamentStep', 'removeQuizStep', 'removeTournamentStep',
                    'getUserStep', 'addParticipantStep'
            ]
        }
        def eager = EagerFaultScenarioGenerator.generate(generated, new RecoveryScheduleCap(20))
        def immediate = eager.faultScenarios().find { scenario ->
            scenario.workloadPlanId() == serial?.deterministicId() && scenario.assignedVector() == '00100' &&
                    scenario.actions()*.kind().take(4) == [FaultScenarioActionKind.FORWARD,
                            FaultScenarioActionKind.FORWARD, FaultScenarioActionKind.FORWARD,
                            FaultScenarioActionKind.COMPENSATION]
        }

        then: 'the serial WorkloadPlan and immediate 00100 recovery are reached without reservation or reordering'
        generated.counts().schedulesEmitted == 10
        generated.workloadPlans().size() == 10
        generated.workloadPlans().every {
            it.setupPlan()?.actions() == setup.actions() &&
                    it.setupPlan().participantBindings()*.inputVariantId.toSet() == selectedInputIds
        }
        serial != null
        serial.prerequisiteBaseline() == null
        serial.setupPlan().actions() == setup.actions()
        new WorkloadPlanValidator().validate(serial).valid()
        eager.workloadMaterializability().find { it.workloadPlanId() == serial.deterministicId() }.materializable()
        immediate != null

        when: 'the complete-coverage matcher sees the same pair inside one cross-test larger tuple'
        def thirdDefinition = adapted.sagaDefinitions().find { it.sagaFqn() == CREATE_USER }
        def thirdInput = adapted.inputVariants().find {
            it.sagaFqn() == CREATE_USER && it.sourceClassFqn() != TARGET_TEST
        }
        assert thirdDefinition != null
        assert thirdInput != null
        def largerConfig = new ScenarioGeneratorConfig(true,
                ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,
                false, 3, 10, 1, 1, false,
                ScenarioGeneratorConfig.InputPolicy.ALLOW_UNRESOLVED,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL,
                1234L)
        def larger = ScenarioGenerator.generate(
                targetDefinitions + thirdDefinition,
                targetInputs + thirdInput,
                [], [setupBinding], largerConfig)
        def threeParticipant = larger.workloadPlans().find { it.participants().size() == 3 }

        then: 'the observed pair setup is not attached across test contexts'
        threeParticipant != null
        threeParticipant.setupPlan() == null

        when: 'a formerly accepted UpdateStudentName triple promotes one setup action to a participant'
        def naturalSagas = [ADD_STUDENT, GET_EXECUTION, UPDATE_STUDENT_NAME] as Set
        def naturalInputs = naturalSagas.collect { saga ->
            adapted.inputVariants().find {
                it.sagaFqn() == saga && it.sourceClassFqn() == UPDATE_STUDENT_TEST &&
                        (saga != ADD_STUDENT || it.callContextMethodName() == 'setup')
            }
        }
        assert naturalInputs.every { it != null }
        def naturalInputIds = naturalInputs*.deterministicId.toSet()
        def naturalBindings = adapted.sourceSetupPlanBindings().findAll { binding ->
            binding.inputVariantIds().any { it in naturalInputIds }
        }
        def addStudentInput = naturalInputs.find { it.sagaFqn() == ADD_STUDENT }
        def addStudentBinding = naturalBindings.find {
            it.featureDerived() && it.featureMethodName() == 'setup' &&
                    addStudentInput.deterministicId() in it.inputVariantIds()
        }

        then: 'the strict prefix prepares the complete tuple without replaying AddStudent'
        addStudentBinding?.featureDerived()
        addStudentBinding.featureMethodName() == 'setup'
        addStudentBinding.setupPlan().actions()*.methodKey().every { !it.contains('#addStudent(') }
        naturalBindings.any { it.inputVariantIds().containsAll(naturalInputIds) }
        def naturalSetup = ScenarioGenerator.setupPlanFor(new InputTupleJoiner.InputTuple(
                naturalInputs, 'natural-incoherent-tuple', []),
                adapted.sourceSetupPlanBindings())
        naturalSetup != null
        naturalSetup.actions()*.methodKey().every { !it.contains('#addStudent(') }

        and: 'an opt-in evidence path can publish only the selected focused workload'
        def focusedOutput = System.getProperty('checkpointC.packageOutput')
        if (focusedOutput) {
            Path focusedDirectory = Path.of(focusedOutput).toAbsolutePath().normalize()
            Files.createDirectories(focusedDirectory)
            def focusedWorkloads = new WorkloadGenerationResult(
                    generated.schemaVersion(), generated.effectiveConfig(), [serial], [], generated.counts(),
                    generated.warnings())
            def focusedEager = EagerFaultScenarioGenerator.generate(focusedWorkloads, new RecoveryScheduleCap(20))
            new ExecutableArtifactWriter().write(adapted, 'quizzes', focusedEager, focusedDirectory)
        }

        when: 'the current package is written and read through its checksum boundary'
        Path workloadPath = tempDir.resolve('workloads.jsonl')
        Path manifestPath = tempDir.resolve('scenario-catalog-manifest.json')
        def manifest = new ExecutableArtifactWriter().write(adapted, 'quizzes', eager, tempDir)
        def roundTrip = new ScenarioCatalogPackageReader().read(manifestPath)
        def packageText = Files.readString(workloadPath)

        then:
        manifest.formatVersion() == 1
        manifest.files().keySet().containsAll(['setups', 'workloads', 'faultScenarios', 'requests'])
        def roundTripPlan = roundTrip.workloadPlans().find { it.deterministicId() == serial.deterministicId() }
        roundTripPlan.setupPlan().actions().size() == 12
        def roundTripMaterializability = EagerFaultScenarioGenerator.evaluateMaterializability(roundTripPlan)
        assert roundTripMaterializability.materializable(): roundTripMaterializability.diagnostics()
        roundTrip.workloadPlans().size() == eager.workloadPlans().size()
        roundTrip.workloadPlans().every { it.setupPlan() != null }
        !packageText.contains('runtimeResult')
        !packageText.contains('databaseId')
    }

    private static Set<String> referencedActionIds(Collection values) {
        def ids = [] as Set
        def visit
        visit = { value ->
            if (value == null) return
            if (value.actionId() != null) ids << value.actionId()
            value.constructorArguments().each(visit)
            value.assignments().each { visit(it.value()) }
            value.elements().each(visit)
            visit(value.receiver())
        }
        values.each(visit)
        ids
    }

    private static void configureQuizzesParser(Path applicationRoot) {
        def solver = new CombinedTypeSolver()
        solver.add(new ReflectionTypeSolver(false))
        def simulatorSource = resolveProjectPath('simulator', 'src', 'main', 'java')
        if (Files.isDirectory(simulatorSource)) {
            solver.add(new JavaParserTypeSolver(simulatorSource.toFile()))
        }
        solver.add(new JavaParserTypeSolver(applicationRoot.resolve('src/main/java').toFile()))
        StaticJavaParser.getParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                .setSymbolResolver(new JavaSymbolSolver(solver))
    }
}
