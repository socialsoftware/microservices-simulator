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
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizExecutionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizExecutionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizQuestionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizQuestionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizOptionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizOptionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizQuestionRemovedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizQuestionUpdatedEvent;
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
            QuizUpdatedEvent event = new QuizUpdatedEvent(quiz.getAggregateId(), quiz.getTitle(), quiz.getCreationDate(), quiz.getAvailableDate(), quiz.getConclusionDate(), quiz.getResultsDate());
            event.setPublisherAggregateVersion(quiz.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
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

    public QuizQuestionDto addQuizQuestion(Integer quizId, Integer questionAggregateId, QuizQuestionDto QuizQuestionDto, UnitOfWork unitOfWork) {
        try {
            Quiz quiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizId, unitOfWork);
            QuizQuestion element = new QuizQuestion(QuizQuestionDto);
            quiz.getQuestions().add(element);
            unitOfWorkService.registerChanged(quiz, unitOfWork);
            return QuizQuestionDto;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error adding QuizQuestion: " + e.getMessage());
        }
    }

    public List<QuizQuestionDto> addQuizQuestions(Integer quizId, List<QuizQuestionDto> QuizQuestionDtos, UnitOfWork unitOfWork) {
        try {
            Quiz quiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizId, unitOfWork);
            QuizQuestionDtos.forEach(dto -> {
                QuizQuestion element = new QuizQuestion(dto);
                quiz.getQuestions().add(element);
            });
            unitOfWorkService.registerChanged(quiz, unitOfWork);
            return QuizQuestionDtos;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error adding QuizQuestions: " + e.getMessage());
        }
    }

    public QuizQuestionDto getQuizQuestion(Integer quizId, Integer questionAggregateId, UnitOfWork unitOfWork) {
        try {
            Quiz quiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizId, unitOfWork);
            QuizQuestion element = quiz.getQuestions().stream()
                .filter(item -> item.getQuestionAggregateId() != null &&
                               item.getQuestionAggregateId().equals(questionAggregateId))
                .findFirst()
                .orElseThrow(() -> new AnswersException("QuizQuestion not found"));
            return element.buildDto();
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving QuizQuestion: " + e.getMessage());
        }
    }

    public void removeQuizQuestion(Integer quizId, Integer questionAggregateId, UnitOfWork unitOfWork) {
        try {
            Quiz quiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizId, unitOfWork);
            quiz.getQuestions().removeIf(item ->
                item.getQuestionAggregateId() != null &&
                item.getQuestionAggregateId().equals(questionAggregateId)
            );
            unitOfWorkService.registerChanged(quiz, unitOfWork);
            QuizQuestionRemovedEvent event = new QuizQuestionRemovedEvent(quizId, questionAggregateId);
            event.setPublisherAggregateVersion(quiz.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error removing QuizQuestion: " + e.getMessage());
        }
    }

    public QuizQuestionDto updateQuizQuestion(Integer quizId, Integer questionAggregateId, QuizQuestionDto QuizQuestionDto, UnitOfWork unitOfWork) {
        try {
            Quiz quiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizId, unitOfWork);
            QuizQuestion element = quiz.getQuestions().stream()
                .filter(item -> item.getQuestionAggregateId() != null &&
                               item.getQuestionAggregateId().equals(questionAggregateId))
                .findFirst()
                .orElseThrow(() -> new AnswersException("QuizQuestion not found"));
            if (QuizQuestionDto.getTitle() != null) {
                element.setQuestionTitle(QuizQuestionDto.getTitle());
            }
            if (QuizQuestionDto.getContent() != null) {
                element.setQuestionContent(QuizQuestionDto.getContent());
            }
            unitOfWorkService.registerChanged(quiz, unitOfWork);
            QuizQuestionUpdatedEvent event = new QuizQuestionUpdatedEvent(quizId, element.getQuestionAggregateId(), element.getQuestionVersion(), element.getQuestionTitle(), element.getQuestionContent(), element.getQuestionSequence());
            event.setPublisherAggregateVersion(quiz.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return element.buildDto();
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating QuizQuestion: " + e.getMessage());
        }
    }


    public Quiz handleExecutionUpdatedEvent(Integer aggregateId, Integer executionAggregateId, Integer executionVersion, String acronym, String academicTerm, UnitOfWork unitOfWork) {
        try {
            Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);

        // Handle execution single reference
        if (newQuiz.getExecution() != null && 
            newQuiz.getExecution().getExecutionAggregateId() != null &&
            newQuiz.getExecution().getExecutionAggregateId().equals(executionAggregateId)) {
            newQuiz.getExecution().setExecutionVersion(executionVersion);
        }

            unitOfWorkService.registerChanged(newQuiz, unitOfWork);


            return newQuiz;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling ExecutionUpdatedEvent: " + e.getMessage());
        }
    }

    public Quiz handleExecutionDeletedEvent(Integer aggregateId, Integer executionAggregateId, Integer executionVersion, UnitOfWork unitOfWork) {
        try {
            Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);

        // Handle execution single reference
        if (newQuiz.getExecution() != null && 
            newQuiz.getExecution().getExecutionAggregateId() != null &&
            newQuiz.getExecution().getExecutionAggregateId().equals(executionAggregateId)) {
            newQuiz.getExecution().setExecutionState(Aggregate.AggregateState.INACTIVE);
        }

            unitOfWorkService.registerChanged(newQuiz, unitOfWork);

        unitOfWorkService.registerEvent(
            new QuizExecutionDeletedEvent(
                newQuiz.getAggregateId(),
                executionAggregateId
            ),
            unitOfWork
        );

            return newQuiz;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling ExecutionDeletedEvent: " + e.getMessage());
        }
    }

    public Quiz handleTopicUpdatedEvent(Integer aggregateId, Integer topicAggregateId, Integer topicVersion, UnitOfWork unitOfWork) {
        try {
            Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);



            unitOfWorkService.registerChanged(newQuiz, unitOfWork);


            return newQuiz;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling TopicUpdatedEvent: " + e.getMessage());
        }
    }

    public Quiz handleTopicDeletedEvent(Integer aggregateId, Integer topicAggregateId, Integer topicVersion, UnitOfWork unitOfWork) {
        try {
            Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);



            unitOfWorkService.registerChanged(newQuiz, unitOfWork);


            return newQuiz;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling TopicDeletedEvent: " + e.getMessage());
        }
    }




}