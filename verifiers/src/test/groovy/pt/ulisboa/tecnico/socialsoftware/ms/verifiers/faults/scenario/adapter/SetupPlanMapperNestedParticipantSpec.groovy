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

    private static GroovyValueRecipe reference(String occurrence, String methodName) {
        new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_VARIABLE, methodName, [],
                GroovyValueMetadata.defaultMetadata(),
                new GroovySourceValueReference(occurrence, methodName, []))
    }

    private static GroovyValueRecipe literal(String text) {
        new GroovyValueRecipe(GroovyValueKind.LITERAL, text, [])
    }
}
