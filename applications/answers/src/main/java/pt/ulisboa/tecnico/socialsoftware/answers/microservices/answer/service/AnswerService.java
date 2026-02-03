package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerUserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionAnsweredDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuizDto;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.AnswerDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.AnswerUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateAnswerRequestDto;


@Service
@Transactional
public class AnswerService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private AnswerFactory answerFactory;

    public AnswerService() {}

    public AnswerDto createAnswer(CreateAnswerRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            AnswerDto answerDto = new AnswerDto();
            answerDto.setCreationDate(createRequest.getCreationDate());
            answerDto.setAnswerDate(createRequest.getAnswerDate());
            answerDto.setCompleted(createRequest.getCompleted());
            if (createRequest.getExecution() != null) {
                AnswerExecutionDto executionDto = new AnswerExecutionDto();
                executionDto.setAggregateId(createRequest.getExecution().getAggregateId());
                executionDto.setVersion(createRequest.getExecution().getVersion());
                executionDto.setState(createRequest.getExecution().getState());
                answerDto.setExecution(executionDto);
            }
            if (createRequest.getUser() != null) {
                AnswerUserDto userDto = new AnswerUserDto();
                userDto.setAggregateId(createRequest.getUser().getAggregateId());
                userDto.setVersion(createRequest.getUser().getVersion());
                userDto.setState(createRequest.getUser().getState());
                answerDto.setUser(userDto);
            }
            if (createRequest.getQuiz() != null) {
                AnswerQuizDto quizDto = new AnswerQuizDto();
                quizDto.setAggregateId(createRequest.getQuiz().getAggregateId());
                quizDto.setVersion(createRequest.getQuiz().getVersion());
                quizDto.setState(createRequest.getQuiz().getState());
                answerDto.setQuiz(quizDto);
            }
            answerDto.setQuestions(createRequest.getQuestions());
            
            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Answer answer = answerFactory.createAnswer(aggregateId, answerDto);
            unitOfWorkService.registerChanged(answer, unitOfWork);
            return answerFactory.createAnswerDto(answer);
        } catch (Exception e) {
            throw new AnswersException("Error creating answer: " + e.getMessage());
        }
    }

    public AnswerDto getAnswerById(Integer id, UnitOfWork unitOfWork) {
        try {
            Answer answer = (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return answerFactory.createAnswerDto(answer);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving answer: " + e.getMessage());
        }
    }

    public List<AnswerDto> getAllAnswers(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = answerRepository.findAll().stream()
                .map(Answer::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(answerFactory::createAnswerDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AnswersException("Error retrieving all answers: " + e.getMessage());
        }
    }

    public AnswerDto updateAnswer(AnswerDto answerDto, UnitOfWork unitOfWork) {
        try {
            Integer id = answerDto.getAggregateId();
            Answer answer = (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            if (answerDto.getCreationDate() != null) {
                answer.setCreationDate(answerDto.getCreationDate());
            }
            if (answerDto.getAnswerDate() != null) {
                answer.setAnswerDate(answerDto.getAnswerDate());
            }
            answer.setCompleted(answerDto.getCompleted());

            unitOfWorkService.registerChanged(answer, unitOfWork);
            unitOfWorkService.registerEvent(new AnswerUpdatedEvent(answer.getAggregateId(), answer.getCreationDate(), answer.getAnswerDate(), answer.getCompleted()), unitOfWork);
            return answerFactory.createAnswerDto(answer);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating answer: " + e.getMessage());
        }
    }

    public void deleteAnswer(Integer id, UnitOfWork unitOfWork) {
        try {
            Answer answer = (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            answer.remove();
            unitOfWorkService.registerChanged(answer, unitOfWork);
            unitOfWorkService.registerEvent(new AnswerDeletedEvent(answer.getAggregateId()), unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting answer: " + e.getMessage());
        }
    }






}