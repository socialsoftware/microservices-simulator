package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.SetupPlanValidator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupValueKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.*
import spock.lang.Specification

class SetupPlanMapperNestedParticipantSpec extends Specification {
    private static final String QUIZ =
            'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto'
    private static final String QUESTION =
            'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto'
    private static final String TOPIC =
            'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto'

    def 'whole participant recipes retain literals assignments lists sets and exact earlier results'() {
        given:
        def questionOne = action('question-one', 'createQuestion', QUESTION)
        def questionTwo = action('question-two', 'createQuestion', QUESTION)
        def topic = action('topic-one', 'createTopic', TOPIC)
        def quizRecipe = constructor(QUIZ, [
                assignment(0, 'title', literal("'nested quiz'")),
                assignment(1, 'questionDtos', collection('list', [
                        reference('question-one', 'createQuestion'),
                        reference('question-two', 'createQuestion')
                ]))
        ])
        def questionRecipe = constructor(QUESTION, [
                assignment(0, 'title', literal("'nested question'")),
                assignment(1, 'topicDto', transform('toSet', collection('list', [
                        reference('topic-one', 'createTopic')
                ])))
        ])

        when:
        def plan = new SetupPlanMapper().map([questionOne, questionTwo, topic], [
                participant('quiz-input', QUIZ, quizRecipe),
                participant('question-input', QUESTION, questionRecipe)
        ])

        then:
        new SetupPlanValidator().validate(plan).valid()
        plan.participantBindings()*.inputVariantId() == ['quiz-input', 'question-input']
        def quiz = plan.participantBindings().find { it.inputVariantId() == 'quiz-input' }.value()
        quiz.kind() == SetupValueKind.CONSTRUCTOR
        quiz.assignments()*.propertyName() == ['title', 'questionDtos']
        quiz.assignments()[0].value().literalValue() == 'nested quiz'
        quiz.assignments()[1].value().kind() == SetupValueKind.LIST
        quiz.assignments()[1].value().elements()*.kind() ==
                [SetupValueKind.ACTION_RESULT, SetupValueKind.ACTION_RESULT]
        quiz.assignments()[1].value().elements()*.actionId() ==
                ['setup-action-1', 'setup-action-2']
        def question = plan.participantBindings().find { it.inputVariantId() == 'question-input' }.value()
        question.assignments()[0].value().literalValue() == 'nested question'
        question.assignments()[1].value().kind() == SetupValueKind.SET
        question.assignments()[1].value().elements()*.actionId() == ['setup-action-3']
    }

    def 'returned dto mutations and nested aggregate ids retain exact producer identity'() {
        given:
        def returnedRecipe = new GroovyValueRecipe(GroovyValueKind.HELPER_CALL_RESULT, 'created', [],
                new GroovyValueMetadata(GroovyValueResolutionCategory.RESOLVED, QUESTION, null, null, [
                        assignment(0, 'title', literal("'updated title'"))
                ]))
        def exact = new GroovySourceValueReference('question-one', 'createQuestion', [])
        def returnedArgument = new GroovyTraceArgument(0, 'mutated result', returnedRecipe, QUESTION, exact)
        def idsRecipe = transform('toSet', collection('list', [
                reference('topic-one', 'createTopic', ['aggregateId'])
        ]))

        when:
        def plan = new SetupPlanMapper().map([
                action('question-one', 'createQuestion', QUESTION),
                action('topic-one', 'createTopic', TOPIC)
        ], [
                new SetupPlanMapper.ParticipantSource('mutated-question', [returnedArgument]),
                participant('topic-ids', 'java.util.Set<java.lang.Integer>', idsRecipe)
        ])

        then:
        new SetupPlanValidator().validate(plan).valid()
        def mutated = plan.participantBindings().find { it.inputVariantId() == 'mutated-question' }.value()
        mutated.kind() == SetupValueKind.ACTION_RESULT
        mutated.actionId() == 'setup-action-1'
        mutated.assignments()*.propertyName() == ['title']
        mutated.assignments()*.value*.literalValue() == ['updated title']
        def ids = plan.participantBindings().find { it.inputVariantId() == 'topic-ids' }.value()
        ids.kind() == SetupValueKind.SET
        ids.elements()*.kind() == [SetupValueKind.ACTION_RESULT_PROPERTY]
        ids.elements()*.actionId() == ['setup-action-2']
        ids.elements()*.propertyName() == ['aggregateId']
    }

    def 'returned dto mutation values fail closed on unavailable or mismatched producers'() {
        given:
        def returnedRecipe = new GroovyValueRecipe(GroovyValueKind.HELPER_CALL_RESULT, 'created', [],
                new GroovyValueMetadata(GroovyValueResolutionCategory.RESOLVED, QUESTION, null, null, [
                        assignment(0, 'title', reference(referencedOccurrence, referencedMethod, ['aggregateId']))
                ]))
        def exact = new GroovySourceValueReference('question-one', 'createQuestion', [])
        def argument = new GroovyTraceArgument(0, 'mutated result', returnedRecipe, QUESTION, exact)

        when:
        def plan = new SetupPlanMapper().map([
                action('question-one', 'createQuestion', QUESTION),
                action('topic-one', 'createTopic', TOPIC)
        ], [new SetupPlanMapper.ParticipantSource('mutated-question', [argument])])

        then:
        plan.participantBindings()[0].value().blockers() == [expectedBlocker]
        !new SetupPlanValidator().validate(plan).valid()

        where:
        referencedOccurrence | referencedMethod  || expectedBlocker
        'missing-topic'       | 'createTopic'     || 'UNRESOLVED_SETUP_SOURCE_REFERENCE:missing-topic'
        'topic-one'           | 'createQuestion'  || 'MISMATCHED_SETUP_SOURCE_REFERENCE:topic-one'
    }

    def 'property projection ignores unrelated dto setters but rejects mutation of the projected property'() {
        given:
        def baseResult = new GroovyValueRecipe(GroovyValueKind.HELPER_CALL_RESULT, 'created', [],
                new GroovyValueMetadata(GroovyValueResolutionCategory.RESOLVED, QUESTION, null, null, [
                        assignment(0, mutatedProperty, literal(mutatedValue))
                ]))
        def propertyReference = new GroovySourceValueReference('question-one', 'createQuestion', ['aggregateId'])
        def propertyRecipe = new GroovyValueRecipe(GroovyValueKind.PROPERTY_ACCESS, 'aggregateId', [baseResult],
                GroovyValueMetadata.defaultMetadata(), propertyReference)
        def argument = new GroovyTraceArgument(0, 'projected result property', propertyRecipe,
                Integer.name, propertyReference)

        when:
        def plan = new SetupPlanMapper().map([action('question-one', 'createQuestion', QUESTION)], [
                new SetupPlanMapper.ParticipantSource('projected-id', [argument])
        ])
        def value = plan.participantBindings()[0].value()

        then:
        value.kind() == expectedKind
        value.blockers() == expectedBlockers

        where:
        mutatedProperty | mutatedValue      || expectedKind                          | expectedBlockers
        'title'         | "'updated title'" || SetupValueKind.ACTION_RESULT_PROPERTY | []
        'aggregateId'   | '999'             || SetupValueKind.LITERAL                | ['UNSUPPORTED_SETUP_RESULT_PROPERTY_MUTATION:[aggregateId]']
    }

    def 'property projection does not reinterpret authoritative constructor history as a returned dto mutation'() {
        given:
        def historical = constructor(QUESTION, [assignment(0, 'aggregateId', literal('41'))])
        def propertyReference = new GroovySourceValueReference('question-one', 'createQuestion', ['aggregateId'])
        def propertyRecipe = new GroovyValueRecipe(GroovyValueKind.PROPERTY_ACCESS, 'aggregateId', [historical],
                GroovyValueMetadata.defaultMetadata(), propertyReference)
        def argument = new GroovyTraceArgument(0, 'authoritative projected result', propertyRecipe,
                Integer.name, propertyReference)

        when:
        def plan = new SetupPlanMapper().map([action('question-one', 'createQuestion', QUESTION)], [
                new SetupPlanMapper.ParticipantSource('projected-id', [argument])
        ])

        then:
        def value = plan.participantBindings()[0].value()
        value.kind() == SetupValueKind.ACTION_RESULT_PROPERTY
        value.actionId() == 'setup-action-1'
        value.propertyName() == 'aggregateId'
        value.blockers().isEmpty()
    }

    def 'unresolved later and selected-target references remain blocked without value fallback'() {
        given:
        def recipe = constructor(QUIZ, [
                assignment(0, 'title', literal("'must survive'")),
                assignment(1, 'questionDtos', collection('list', [
                        reference(unavailableOccurrence, 'createQuestion')
                ]))
        ])

        when:
        def plan = new SetupPlanMapper().map([action('earlier', 'createQuestion', QUESTION)], [
                participant('quiz-input', QUIZ, recipe)
        ])

        then:
        plan.participantBindings().size() == 1
        plan.participantBindings()[0].value().blockers() ==
                ["UNRESOLVED_SETUP_SOURCE_REFERENCE:${unavailableOccurrence}".toString()]
        !new SetupPlanValidator().validate(plan).valid()

        where:
        unavailableOccurrence << ['missing-producer', 'later-producer', 'selected-target']
    }

    def 'ambiguous exact occurrences invalidate the plan and binding deterministically'() {
        given:
        def first = action('duplicate', 'createQuestion', QUESTION)
        def duplicate = action('duplicate', 'createTopic', TOPIC)
        def recipe = constructor(QUIZ, [assignment(0, 'questionDtos',
                collection('list', [reference('duplicate', 'createQuestion')]))])

        when:
        def plan = new SetupPlanMapper().map([first, duplicate], [participant('quiz-input', QUIZ, recipe)])

        then:
        plan.actions().size() == 1
        plan.blockers() == ['AMBIGUOUS_SETUP_SOURCE_OCCURRENCE:duplicate']
        plan.participantBindings()[0].value().blockers() ==
                ['AMBIGUOUS_SETUP_SOURCE_REFERENCE:duplicate']
        !new SetupPlanValidator().validate(plan).valid()
    }

    def 'an authoritative whole-result reference does not evaluate historical nested provenance'() {
        given:
        def historicalRecipe = constructor(QUESTION, [assignment(0, 'topicDto',
                collection('set', [reference('historical-missing', 'createTopic')]))])
        def exact = new GroovySourceValueReference('question-one', 'createQuestion', [])
        def argument = new GroovyTraceArgument(0, 'whole prior result', historicalRecipe, QUESTION, exact)

        when:
        def plan = new SetupPlanMapper().map([action('question-one', 'createQuestion', QUESTION)], [
                new SetupPlanMapper.ParticipantSource('question-input', [argument])
        ])

        then:
        plan.participantBindings()[0].value().kind() == SetupValueKind.ACTION_RESULT
        plan.participantBindings()[0].value().actionId() == 'setup-action-1'
        new SetupPlanValidator().validate(plan).valid()
    }

    def 'nested references keep validator type checks and reference-free arguments stay unbound'() {
        given:
        def wrongTypeRecipe = constructor(QUIZ, [assignment(0, 'questionDtos',
                collection('list', [reference('topic-one', 'createTopic')]))])
        def literalOnlyRecipe = constructor(QUIZ, [assignment(0, 'title', literal("'plain quiz'"))])

        when:
        def plan = new SetupPlanMapper().map([action('topic-one', 'createTopic', TOPIC)], [
                participant('wrong-type', QUIZ, wrongTypeRecipe),
                participant('literal-only', QUIZ, literalOnlyRecipe)
        ])
        def validation = new SetupPlanValidator().validate(plan)

        then:
        plan.participantBindings()*.inputVariantId() == ['wrong-type']
        !validation.valid()
        validation.diagnostics()*.code().contains('INCOMPATIBLE_SETUP_RESULT_REFERENCE')
    }

    private static SetupPlanMapper.ParticipantSource participant(String id,
                                                                 String expectedType,
                                                                 GroovyValueRecipe recipe) {
        new SetupPlanMapper.ParticipantSource(id,
                [new GroovyTraceArgument(0, 'dummyapp nested participant fixture', recipe, expectedType)])
    }

    private static GroovyFacadeSetupActionTrace action(String occurrence,
                                                       String methodName,
                                                       String resultType) {
        new GroovyFacadeSetupActionTrace('com.example.dummyapp.NestedParticipantSpec', 'setup', occurrence,
                'com.example.dummyapp.FixtureFacade',
                "com.example.dummyapp.FixtureFacade#${methodName}():${resultType}".toString(),
                methodName, [], resultType, false, [])
    }

    private static GroovyValueRecipe constructor(String type, List<GroovyAssignmentRecipe> assignments) {
        new GroovyValueRecipe(GroovyValueKind.CONSTRUCTOR, "new ${type}()".toString(), [],
                new GroovyValueMetadata(GroovyValueResolutionCategory.RESOLVED, type, null, null, assignments))
    }

    private static GroovyAssignmentRecipe assignment(int order,
                                                     String property,
                                                     GroovyValueRecipe value) {
        new GroovyAssignmentRecipe('property', property, property, order,
                "${property} = value".toString(), value, null)
    }

    private static GroovyValueRecipe collection(String kind, List<GroovyValueRecipe> elements) {
        new GroovyValueRecipe(GroovyValueKind.COLLECTION_LITERAL, kind, elements)
    }

    private static GroovyValueRecipe transform(String transform, GroovyValueRecipe receiver) {
        new GroovyValueRecipe(GroovyValueKind.LOCAL_TRANSFORM, transform, [receiver])
    }

    private static GroovyValueRecipe reference(String occurrence, String methodName,
                                                List<String> propertyPath = []) {
        new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_VARIABLE, methodName, [],
                GroovyValueMetadata.defaultMetadata(),
                new GroovySourceValueReference(occurrence, methodName, propertyPath))
    }

    private static GroovyValueRecipe literal(String text) {
        new GroovyValueRecipe(GroovyValueKind.LITERAL, text, [])
    }
}
