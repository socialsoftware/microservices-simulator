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
                executionDto.setState(createRequest.getExecution().getState() != null ? createRequest.getExecution().getState().name() : null);
                quizDto.setExecution(executionDto);
            }
            if (createRequest.getQuestions() != null) {
                quizDto.setQuestions(createRequest.getQuestions().stream().map(srcDto -> {
                    QuizQuestionDto projDto = new QuizQuestionDto();
                    projDto.setAggregateId(srcDto.getAggregateId());
                    projDto.setVersion(srcDto.getVersion());
                    projDto.setState(srcDto.getState() != null ? srcDto.getState().name() : null);
                    return projDto;
                }).collect(Collectors.toSet()));
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Quiz quiz = quizFactory.createQuiz(aggregateId, quizDto);
            unitOfWorkService.registerChanged(quiz, unitOfWork);
            return quizFactory.createQuizDto(quiz);
        } catch (AnswersException e) {
            throw e;
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
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving quiz: " + e.getMessage());
        }
    }

    public QuizDto updateQuiz(QuizDto quizDto, UnitOfWork unitOfWork) {
        try {
            Integer id = quizDto.getAggregateId();
            Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);
            if (quizDto.getTitle() != null) {
                newQuiz.setTitle(quizDto.getTitle());
            }
            if (quizDto.getQuizType() != null) {
                newQuiz.setQuizType(QuizType.valueOf(quizDto.getQuizType()));
            }
            if (quizDto.getCreationDate() != null) {
                newQuiz.setCreationDate(quizDto.getCreationDate());
            }
            if (quizDto.getAvailableDate() != null) {
                newQuiz.setAvailableDate(quizDto.getAvailableDate());
            }
            if (quizDto.getConclusionDate() != null) {
                newQuiz.setConclusionDate(quizDto.getConclusionDate());
            }
            if (quizDto.getResultsDate() != null) {
                newQuiz.setResultsDate(quizDto.getResultsDate());
            }

            unitOfWorkService.registerChanged(newQuiz, unitOfWork);            QuizUpdatedEvent event = new QuizUpdatedEvent(newQuiz.getAggregateId(), newQuiz.getTitle(), newQuiz.getCreationDate(), newQuiz.getAvailableDate(), newQuiz.getConclusionDate(), newQuiz.getResultsDate());
            event.setPublisherAggregateVersion(newQuiz.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return quizFactory.createQuizDto(newQuiz);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating quiz: " + e.getMessage());
        }
    }

    public void deleteQuiz(Integer id, UnitOfWork unitOfWork) {
        try {
            Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);
            newQuiz.remove();
            unitOfWorkService.registerChanged(newQuiz, unitOfWork);            unitOfWorkService.registerEvent(new QuizDeletedEvent(newQuiz.getAggregateId()), unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting quiz: " + e.getMessage());
        }
    }

    public QuizQuestionDto addQuizQuestion(Integer quizId, Integer questionAggregateId, QuizQuestionDto QuizQuestionDto, UnitOfWork unitOfWork) {
        try {
            Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizId, unitOfWork);
            Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);
            QuizQuestion element = new QuizQuestion(QuizQuestionDto);
            newQuiz.getQuestions().add(element);
            unitOfWorkService.registerChanged(newQuiz, unitOfWork);
            return QuizQuestionDto;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error adding QuizQuestion: " + e.getMessage());
        }
    }

    public List<QuizQuestionDto> addQuizQuestions(Integer quizId, List<QuizQuestionDto> QuizQuestionDtos, UnitOfWork unitOfWork) {
        try {
            Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizId, unitOfWork);
            Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);
            QuizQuestionDtos.forEach(dto -> {
                QuizQuestion element = new QuizQuestion(dto);
                newQuiz.getQuestions().add(element);
            });
            unitOfWorkService.registerChanged(newQuiz, unitOfWork);
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
            Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizId, unitOfWork);
            Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);
            newQuiz.getQuestions().removeIf(item ->
                item.getQuestionAggregateId() != null &&
                item.getQuestionAggregateId().equals(questionAggregateId)
            );
            unitOfWorkService.registerChanged(newQuiz, unitOfWork);
            QuizQuestionRemovedEvent event = new QuizQuestionRemovedEvent(quizId, questionAggregateId);
            event.setPublisherAggregateVersion(newQuiz.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error removing QuizQuestion: " + e.getMessage());
        }
    }

    public QuizQuestionDto updateQuizQuestion(Integer quizId, Integer questionAggregateId, QuizQuestionDto QuizQuestionDto, UnitOfWork unitOfWork) {
        try {
            Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizId, unitOfWork);
            Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);
            QuizQuestion element = newQuiz.getQuestions().stream()
                .filter(item -> item.getQuestionAggregateId() != null &&
                               item.getQuestionAggregateId().equals(questionAggregateId))
                .findFirst()
                .orElseThrow(() -> new AnswersException("QuizQuestion not found"));

            unitOfWorkService.registerChanged(newQuiz, unitOfWork);
            QuizQuestionUpdatedEvent event = new QuizQuestionUpdatedEvent(quizId, element.getQuestionAggregateId(), element.getQuestionVersion(), element.getQuestionTitle(), element.getQuestionContent(), element.getQuestionSequence());
            event.setPublisherAggregateVersion(newQuiz.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return element.buildDto();
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating QuizQuestion: " + e.getMessage());
        }
    }


    public Quiz handleExecutionUpdatedEvent(Integer aggregateId, Integer executionAggregateId, Integer executionVersion, String executionAcronym, String executionAcademicTerm, UnitOfWork unitOfWork) {
        try {
            Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);



            unitOfWorkService.registerChanged(newQuiz, unitOfWork);


            return newQuiz;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling ExecutionUpdatedEvent quiz: " + e.getMessage());
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
            throw new AnswersException("Error handling TopicUpdatedEvent quiz: " + e.getMessage());
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
            throw new AnswersException("Error handling TopicDeletedEvent quiz: " + e.getMessage());
        }
    }




}