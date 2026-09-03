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
        def genericCompensationSignatures = genericCompensations.collect {
            "${it.stepKey()}|${it.aggregateName()}|${it.aggregateKeyText()}".toString()
        } as Set

        then: 'both map-returning helper inputs are recovered for every two-Saga feature in the ordinary test'
        targetTraces.count { it.sagaClassFqn == REMOVE } == 5
        targetTraces.count { it.sagaClassFqn == ADD } == 4
        targetTraces.every { it.sourceBindingName in ['remove', 'add'] }

        and: 'every Tournament command footprint maps its aggregate key to zero-based constructor argument 1'
        tournamentDispatches.size() == 3
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
            def ids = [binding.leftInputVariantId(), binding.rightInputVariantId()] as Set
            ids.any { it in targetInputIdsBySaga[REMOVE] } && ids.any { it in targetInputIdsBySaga[ADD] }
        }
        assert targetSetupBindings.size() == 20
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

        and: 'all setup and participant result references point backward to the exact producers'
        setup.actions().each { action ->
            referencedActionIds(action.arguments()*.value()).every { ref ->
                setup.actions().find { it.actionId() == ref }.orderIndex() < action.orderIndex()
            }
        }
        def tournamentBindings = setup.participantBindings().findAll { it.argumentIndex() == 1 }
        tournamentBindings*.value*.kind.toSet() == [SetupValueKind.ACTION_RESULT_PROPERTY] as Set
        tournamentBindings*.value*.actionId.toSet() == ['setup-action-12'] as Set
        tournamentBindings*.value*.propertyName.toSet() == ['aggregateId'] as Set
        setup.participantBindings().find { it.inputVariantId() in adapted.inputVariants().findAll { it.sagaFqn() == ADD }*.deterministicId && it.argumentIndex() == 2 }
                .value().actionId() == 'setup-action-1'
        setup.participantBindings().find { it.inputVariantId() in adapted.inputVariants().findAll { it.sagaFqn() == ADD }*.deterministicId && it.argumentIndex() == 3 }
                .value().actionId() == 'setup-action-4'

        when: 'ordinary deterministic enumeration is given all ten dependency-preserving orders'
        def selectedInputIds = [setupBinding.leftInputVariantId(), setupBinding.rightInputVariantId()] as Set
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
        generated.workloadPlans().every { it.setupPlan() == setup }
        serial != null
        serial.prerequisiteBaseline() == null
        serial.setupPlan() == setup
        new WorkloadPlanValidator().validate(serial).valid()
        eager.workloadMaterializability().find { it.workloadPlanId() == serial.deterministicId() }.materializable()
        immediate != null

        when: 'the current pair-keyed matcher sees the same pair inside one larger tuple'
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

        then: 'the incomplete pair setup is currently attached and leaves the third participant uncovered'
        threeParticipant != null
        threeParticipant.setupPlan() == setup
        threeParticipant.setupPlan().participantBindings()*.inputVariantId.toSet() == selectedInputIds
        !(thirdInput.deterministicId() in threeParticipant.setupPlan().participantBindings()*.inputVariantId)

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
