package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizOptionDto;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.QuizType;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateQuizRequestDto;


@Service
@Transactional
public class QuizService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizFactory quizFactory;

    public QuizService() {}

    public QuizDto createQuiz(CreateQuizRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            QuizDto quizDto = new QuizDto();
            quizDto.setTitle(createRequest.getTitle());
            quizDto.setQuizType(createRequest.getQuizType() != null ? createRequest.getQuizType().name() : null);
            quizDto.setCreationDate(createRequest.getCreationDate());
            quizDto.setAvailableDate(createRequest.getAvailableDate());
            quizDto.setConclusionDate(createRequest.getConclusionDate());
            quizDto.setResultsDate(createRequest.getResultsDate());
            if (createRequest.getExecution() != null) {
                QuizExecutionDto executionDto = new QuizExecutionDto();
                executionDto.setAggregateId(createRequest.getExecution().getAggregateId());
                executionDto.setVersion(createRequest.getExecution().getVersion());
                executionDto.setState(createRequest.getExecution().getState());
                quizDto.setExecution(executionDto);
            }
            if (createRequest.getQuestions() != null) {
                quizDto.setQuestions(createRequest.getQuestions().stream().map(srcDto -> {
                    QuizQuestionDto projDto = new QuizQuestionDto();
                    projDto.setAggregateId(srcDto.getAggregateId());
                    projDto.setVersion(srcDto.getVersion());
                    projDto.setState(srcDto.getState());
                    return projDto;
                }).collect(Collectors.toSet()));
            }
            
            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Quiz quiz = quizFactory.createQuiz(aggregateId, quizDto);
            unitOfWorkService.registerChanged(quiz, unitOfWork);
            return quizFactory.createQuizDto(quiz);
        } catch (Exception e) {
            throw new AnswersException("Error creating quiz: " + e.getMessage());
        }
    }

    public QuizDto getQuizById(Integer id, UnitOfWork unitOfWork) {
        try {
            Quiz quiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return quizFactory.createQuizDto(quiz);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving quiz: " + e.getMessage());
        }
    }

    public List<QuizDto> getAllQuizs(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = quizRepository.findAll().stream()
                .map(Quiz::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(quizFactory::createQuizDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AnswersException("Error retrieving all quizs: " + e.getMessage());
        }
    }

    public QuizDto updateQuiz(QuizDto quizDto, UnitOfWork unitOfWork) {
        try {
            Integer id = quizDto.getAggregateId();
            Quiz quiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            if (quizDto.getTitle() != null) {
                quiz.setTitle(quizDto.getTitle());
            }
            if (quizDto.getQuizType() != null) {
                quiz.setQuizType(QuizType.valueOf(quizDto.getQuizType()));
            }
            if (quizDto.getCreationDate() != null) {
                quiz.setCreationDate(quizDto.getCreationDate());
            }
            if (quizDto.getAvailableDate() != null) {
                quiz.setAvailableDate(quizDto.getAvailableDate());
            }
            if (quizDto.getConclusionDate() != null) {
                quiz.setConclusionDate(quizDto.getConclusionDate());
            }
            if (quizDto.getResultsDate() != null) {
                quiz.setResultsDate(quizDto.getResultsDate());
            }

            unitOfWorkService.registerChanged(quiz, unitOfWork);
            return quizFactory.createQuizDto(quiz);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating quiz: " + e.getMessage());
        }
    }

    public void deleteQuiz(Integer id, UnitOfWork unitOfWork) {
        try {
            Quiz quiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            quiz.remove();
            unitOfWorkService.registerChanged(quiz, unitOfWork);
            unitOfWorkService.registerEvent(new QuizDeletedEvent(quiz.getAggregateId()), unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting quiz: " + e.getMessage());
        }
    }




}