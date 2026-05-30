package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.RemoveCourseExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities;

public class QuizzesTestFactory {

	public static final String ANONYMOUS = "ANONYMOUS";

	public static final LocalDateTime TIME_1 = DateHandler.now().plusMinutes(5);
	public static final LocalDateTime TIME_2 = DateHandler.now().plusMinutes(25);
	public static final LocalDateTime TIME_3 = DateHandler.now().plusHours(1).plusMinutes(5);
	public static final LocalDateTime TIME_4 = DateHandler.now().plusHours(1).plusMinutes(25);

	public static final Integer COURSE_EXECUTION_AGGREGATE_ID_1 = 1;
	public static final String COURSE_EXECUTION_NAME = "BLCM";
	public static final String COURSE_EXECUTION_TYPE = "TECNICO";
	public static final String COURSE_EXECUTION_ACRONYM = "TESTBLCM";
	public static final String COURSE_EXECUTION_ACADEMIC_TERM = "2022/2023";

	public static final Integer TOPIC_AGGREGATE_ID_1 = 4;
	public static final Integer TOPIC_AGGREGATE_ID_2 = 5;
	public static final Integer TOPIC_AGGREGATE_ID_3 = 6;
	public static final Integer USER_AGGREGATE_ID_1 = 7;
	public static final Integer USER_AGGREGATE_ID_2 = 8;
	public static final Integer USER_AGGREGATE_ID_3 = 9;
	public static final Integer TOURNAMENT_AGGREGATE_ID_1 = 10;
	public static final Integer QUIZ_AGGREGATE_ID_1 = 13;

	public static final String USER_NAME_1 = "USER_NAME_1";
	public static final String USER_NAME_2 = "USER_NAME_2";
	public static final String USER_NAME_3 = "USER_NAME_3";

	public static final String USER_USERNAME_1 = "USER_USERNAME_1";
	public static final String USER_USERNAME_2 = "USER_USERNAME_2";
	public static final String USER_USERNAME_3 = "USER_USERNAME_3";

	public static final String STUDENT_ROLE = "STUDENT";
	public static final String ACRONYM_1 = "ACRONYM_1";

	public static final String TOPIC_NAME_1 = "TOPIC_NAME_1";
	public static final String TOPIC_NAME_2 = "TOPIC_NAME_2";
	public static final String TOPIC_NAME_3 = "TOPIC_NAME_3";

	public static final String TITLE_1 = "Title One";
	public static final String TITLE_2 = "Title Two";
	public static final String TITLE_3 = "Title Three";
	public static final String CONTENT_1 = "Content One";
	public static final String CONTENT_2 = "Content Two";
	public static final String CONTENT_3 = "Content Three";
	public static final String OPTION_1 = "Option One";
	public static final String OPTION_2 = "Option Two";
	public static final String OPTION_3 = "Option Three";
	public static final String OPTION_4 = "Option Four";

	private final ExecutionFunctionalities courseExecutionFunctionalities;
	private final UserFunctionalities userFunctionalities;
	private final TopicFunctionalities topicFunctionalities;
	private final QuestionFunctionalities questionFunctionalities;
	private final TournamentFunctionalities tournamentFunctionalities;
	private final SagaUnitOfWorkService sagaUnitOfWorkService;

	public QuizzesTestFactory(
			SagaUnitOfWorkService sagaUnitOfWorkService,
			ExecutionFunctionalities courseExecutionFunctionalities,
			UserFunctionalities userFunctionalities,
			TopicFunctionalities topicFunctionalities,
			QuestionFunctionalities questionFunctionalities,
			TournamentFunctionalities tournamentFunctionalities) {

		this.sagaUnitOfWorkService = sagaUnitOfWorkService;
		this.courseExecutionFunctionalities = courseExecutionFunctionalities;
		this.userFunctionalities = userFunctionalities;
		this.topicFunctionalities = topicFunctionalities;
		this.questionFunctionalities = questionFunctionalities;
		this.tournamentFunctionalities = tournamentFunctionalities;
	}

	public CourseExecutionDto createCourseExecution(
			String name, String type, String acronym, String term, LocalDateTime endDate) {

		var courseExecutionDto = new CourseExecutionDto();
		courseExecutionDto.setName(name);
		courseExecutionDto.setType(type);
		courseExecutionDto.setAcronym(acronym);
		courseExecutionDto.setAcademicTerm(term);
		courseExecutionDto.setEndDate(DateHandler.toISOString(endDate));

		CourseExecutionDto createdCourseExecutionDto = courseExecutionFunctionalities
				.createCourseExecution(courseExecutionDto);
		return Objects.requireNonNull(createdCourseExecutionDto);
	}

	public UserDto createUser(String name, String username, String role) {
		var userDto = new UserDto();
		userDto.setName(name);
		userDto.setUsername(username);
		userDto.setRole(role);

		UserDto createdUserDto = userFunctionalities.createUser(userDto);
		Objects.requireNonNull(createdUserDto);

		userFunctionalities.activateUser(createdUserDto.getAggregateId());
		return createdUserDto;
	}

	public TopicDto createTopic(CourseExecutionDto courseExecutionDto, String name) {
		var topicDto = new TopicDto();
		topicDto.setName(name);

		TopicDto createdTopicDto = topicFunctionalities.createTopic(
				courseExecutionDto.getCourseAggregateId(), topicDto);
		return Objects.requireNonNull(createdTopicDto);
	}

	public QuestionDto createQuestion(
			CourseExecutionDto courseExecutionDto, List<TopicDto> topicDtos,
			String title, String content, String correctOption, String wrongOption) {

		var questionDto = new QuestionDto();
		questionDto.setTitle(title);
		questionDto.setContent(content);
		questionDto.setTopicDto(new HashSet<>(topicDtos));

		var optionDto1 = new OptionDto();
		optionDto1.setSequence(1);
		optionDto1.setCorrect(true);
		optionDto1.setContent(correctOption);

		var optionDto2 = new OptionDto();
		optionDto2.setSequence(2);
		optionDto2.setCorrect(false);
		optionDto2.setContent(wrongOption);

		questionDto.setOptionDtos(List.of(optionDto1, optionDto2));

		QuestionDto createdQuestionDto = questionFunctionalities.createQuestion(
				courseExecutionDto.getCourseAggregateId(), questionDto);
		return Objects.requireNonNull(createdQuestionDto);
	}

	public TournamentDto createTournament(LocalDateTime startTime, LocalDateTime endTime, Integer numberOfQuestions,
			Integer userCreatorId, Integer courseExecutionId, List<Integer> topicIds) {

		var tournamentDto = new TournamentDto();
		tournamentDto.setStartTime(DateHandler.toISOString(startTime));
		tournamentDto.setEndTime(DateHandler.toISOString(endTime));
		tournamentDto.setNumberOfQuestions(numberOfQuestions);

		TournamentDto createdTournamentDto = tournamentFunctionalities.createTournament(
				userCreatorId, courseExecutionId, topicIds, tournamentDto);
		return Objects.requireNonNull(createdTournamentDto);
	}

	public InitialState setupInitialState() {
		UserDto user = createUser(
				QuizzesTestFactory.USER_NAME_1,
				QuizzesTestFactory.USER_NAME_1,
				QuizzesTestFactory.STUDENT_ROLE);

		CourseExecutionDto courseExecution = createCourseExecution(
				QuizzesTestFactory.COURSE_EXECUTION_NAME,
				QuizzesTestFactory.COURSE_EXECUTION_TYPE,
				QuizzesTestFactory.COURSE_EXECUTION_ACRONYM,
				QuizzesTestFactory.COURSE_EXECUTION_ACADEMIC_TERM,
				QuizzesTestFactory.TIME_4);

		TopicDto topic = createTopic(courseExecution, QuizzesTestFactory.TOPIC_NAME_1);

		QuestionDto question = createQuestion(courseExecution, List.of(topic),
				QuizzesTestFactory.TITLE_1,
				QuizzesTestFactory.CONTENT_1,
				QuizzesTestFactory.OPTION_1,
				QuizzesTestFactory.OPTION_2);

		courseExecutionFunctionalities.addStudent(
				courseExecution.getAggregateId(), user.getAggregateId());

		TournamentDto tournament = createTournament(
				QuizzesTestFactory.TIME_1,
				QuizzesTestFactory.TIME_3,
				1,
				user.getAggregateId(),
				courseExecution.getAggregateId(),
				List.of(topic.getAggregateId()));

		return new InitialState(user, courseExecution, topic, question, tournament);
	}

	public AddParticipantFunctionalitySagas createAddParticipantFunctionality(
			SagaUnitOfWorkService sagaUnitOfWorkService,
			Integer tournamentAggrId,
			Integer courseExecutionAggrId,
			Integer userAggrId,
			CommandGateway gateway) {

		SagaUnitOfWork uow = sagaUnitOfWorkService.createUnitOfWork(
				AddParticipantFunctionalitySagas.class.getSimpleName());

		var addParticipantSaga = new AddParticipantFunctionalitySagas(
				sagaUnitOfWorkService,
				tournamentAggrId,
				courseExecutionAggrId,
				userAggrId,
				uow,
				gateway);

		return addParticipantSaga;
	}

	public AddParticipantFunctionalitySagas setupInitialStateAndCreateAddParticipantFunctionality(
			SagaUnitOfWorkService sagaUnitOfWorkService,
			CommandGateway gateway,
			ExecutionFunctionalities executionFuncs) {

		InitialState initialState = setupInitialState();

		AddParticipantFunctionalitySagas addParticipantSaga = createAddParticipantFunctionality(
				sagaUnitOfWorkService,
				initialState.tournamentDto().getAggregateId(),
				initialState.courseExecutionDto().getAggregateId(),
				initialState.userDto().getAggregateId(),
				gateway);

		return addParticipantSaga;
	}

	public UpdateTournamentFunctionalitySagas createUpdateTournamentFunctionality(
			SagaUnitOfWorkService sagaUnitOfWorkService,
			TournamentDto tournamentDto,
			Set<Integer> topicsAggregateIds,
			CommandGateway gateway) {

		SagaUnitOfWork uow = sagaUnitOfWorkService.createUnitOfWork(
				UpdateTournamentFunctionalitySagas.class.getSimpleName());

		return new UpdateTournamentFunctionalitySagas(
				sagaUnitOfWorkService,
				tournamentDto,
				topicsAggregateIds,
				uow,
				gateway);
	}

	public UpdateTournamentFunctionalitySagas setupInitialStateAndCreateUpdateTournamentFunctionality(
			SagaUnitOfWorkService sagaUnitOfWorkService,
			CommandGateway gateway) {

		InitialState initialState = setupInitialState();

		TournamentDto updateDto = new TournamentDto();
		updateDto.setAggregateId(initialState.tournamentDto().getAggregateId());
		updateDto.setStartTime(initialState.tournamentDto().getStartTime());
		updateDto.setEndTime(initialState.tournamentDto().getEndTime());
		updateDto.setNumberOfQuestions(initialState.tournamentDto().getNumberOfQuestions());

		Set<Integer> topicsAggregateIds = Set.of(initialState.topicDto().getAggregateId());

		return createUpdateTournamentFunctionality(
				sagaUnitOfWorkService,
				updateDto,
				topicsAggregateIds,
				gateway);
	}

	public RemoveCourseExecutionFunctionalitySagas createRemoveCourseExecutionFunctionality(
			SagaUnitOfWorkService sagaUnitOfWorkService,
			Integer executionAggregateId,
			CommandGateway gateway) {

		SagaUnitOfWork uow = sagaUnitOfWorkService.createUnitOfWork(
				RemoveCourseExecutionFunctionalitySagas.class.getSimpleName());

		return new RemoveCourseExecutionFunctionalitySagas(
				sagaUnitOfWorkService, executionAggregateId, uow, gateway);
	}

	public RemoveCourseExecutionFunctionalitySagas setupInitialStateAndCreateRemoveCourseExecutionFunctionality(
			SagaUnitOfWorkService sagaUnitOfWorkService,
			CommandGateway gateway) {

		InitialState initialState = setupInitialState();

		return createRemoveCourseExecutionFunctionality(
				sagaUnitOfWorkService,
				initialState.courseExecutionDto().getAggregateId(),
				gateway);
	}

	// TODO requires fixing SagaStateConverter to return SagaState
	public SagaState sagaStateOf(Integer sagaAggregateId) {
		SagaUnitOfWork uow = sagaUnitOfWorkService.createUnitOfWork("TEST");
		Aggregate agg = sagaUnitOfWorkService.aggregateLoadAndRegisterRead(sagaAggregateId, uow);
		var sagaAgg = (SagaAggregate) Objects.requireNonNull(agg);
		return Objects.requireNonNull(sagaAgg.getSagaState(), "WHAT HAPPENED");
	}
}