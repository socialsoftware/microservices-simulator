package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionCourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionTopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.OptionDto;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish.QuestionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish.QuestionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish.QuestionCourseDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish.QuestionCourseUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish.QuestionTopicDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish.QuestionTopicUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish.QuestionTopicRemovedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish.QuestionTopicUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish.OptionRemovedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish.OptionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateQuestionRequestDto;


@Service
@Transactional
public class QuestionService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionFactory questionFactory;

    public QuestionService() {}

    public QuestionDto createQuestion(CreateQuestionRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            QuestionDto questionDto = new QuestionDto();
            questionDto.setTitle(createRequest.getTitle());
            questionDto.setContent(createRequest.getContent());
            questionDto.setCreationDate(createRequest.getCreationDate());
            if (createRequest.getCourse() != null) {
                QuestionCourseDto courseDto = new QuestionCourseDto();
                courseDto.setAggregateId(createRequest.getCourse().getAggregateId());
                courseDto.setVersion(createRequest.getCourse().getVersion());
                courseDto.setState(createRequest.getCourse().getState() != null ? createRequest.getCourse().getState().name() : null);
                questionDto.setCourse(courseDto);
            }
            if (createRequest.getTopics() != null) {
                questionDto.setTopics(createRequest.getTopics().stream().map(srcDto -> {
                    QuestionTopicDto projDto = new QuestionTopicDto();
                    projDto.setAggregateId(srcDto.getAggregateId());
                    projDto.setVersion(srcDto.getVersion());
                    projDto.setState(srcDto.getState() != null ? srcDto.getState().name() : null);
                    return projDto;
                }).collect(Collectors.toSet()));
            }
            questionDto.setOptions(createRequest.getOptions());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Question question = questionFactory.createQuestion(aggregateId, questionDto);
            unitOfWorkService.registerChanged(question, unitOfWork);
            return questionFactory.createQuestionDto(question);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error creating question: " + e.getMessage());
        }
    }

    public QuestionDto getQuestionById(Integer id, UnitOfWork unitOfWork) {
        try {
            Question question = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return questionFactory.createQuestionDto(question);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving question: " + e.getMessage());
        }
    }

    public List<QuestionDto> getAllQuestions(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = questionRepository.findAll().stream()
                .map(Question::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Question) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(questionFactory::createQuestionDto)
                .collect(Collectors.toList());
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving question: " + e.getMessage());
        }
    }

    public QuestionDto updateQuestion(QuestionDto questionDto, UnitOfWork unitOfWork) {
        try {
            Integer id = questionDto.getAggregateId();
            Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);
            if (questionDto.getTitle() != null) {
                newQuestion.setTitle(questionDto.getTitle());
            }
            if (questionDto.getContent() != null) {
                newQuestion.setContent(questionDto.getContent());
            }
            if (questionDto.getCreationDate() != null) {
                newQuestion.setCreationDate(questionDto.getCreationDate());
            }

            unitOfWorkService.registerChanged(newQuestion, unitOfWork);            QuestionUpdatedEvent event = new QuestionUpdatedEvent(newQuestion.getAggregateId(), newQuestion.getTitle(), newQuestion.getContent(), newQuestion.getCreationDate());
            event.setPublisherAggregateVersion(newQuestion.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return questionFactory.createQuestionDto(newQuestion);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating question: " + e.getMessage());
        }
    }

    public void deleteQuestion(Integer id, UnitOfWork unitOfWork) {
        try {
            Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);
            newQuestion.remove();
            unitOfWorkService.registerChanged(newQuestion, unitOfWork);            unitOfWorkService.registerEvent(new QuestionDeletedEvent(newQuestion.getAggregateId()), unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting question: " + e.getMessage());
        }
    }

    public QuestionTopicDto addQuestionTopic(Integer questionId, Integer topicAggregateId, QuestionTopicDto QuestionTopicDto, UnitOfWork unitOfWork) {
        try {
            Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(questionId, unitOfWork);
            Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);
            QuestionTopic element = new QuestionTopic(QuestionTopicDto);
            newQuestion.getTopics().add(element);
            unitOfWorkService.registerChanged(newQuestion, unitOfWork);
            return QuestionTopicDto;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error adding QuestionTopic: " + e.getMessage());
        }
    }

    public List<QuestionTopicDto> addQuestionTopics(Integer questionId, List<QuestionTopicDto> QuestionTopicDtos, UnitOfWork unitOfWork) {
        try {
            Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(questionId, unitOfWork);
            Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);
            QuestionTopicDtos.forEach(dto -> {
                QuestionTopic element = new QuestionTopic(dto);
                newQuestion.getTopics().add(element);
            });
            unitOfWorkService.registerChanged(newQuestion, unitOfWork);
            return QuestionTopicDtos;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error adding QuestionTopics: " + e.getMessage());
        }
    }

    public QuestionTopicDto getQuestionTopic(Integer questionId, Integer topicAggregateId, UnitOfWork unitOfWork) {
        try {
            Question question = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(questionId, unitOfWork);
            QuestionTopic element = question.getTopics().stream()
                .filter(item -> item.getTopicAggregateId() != null &&
                               item.getTopicAggregateId().equals(topicAggregateId))
                .findFirst()
                .orElseThrow(() -> new AnswersException("QuestionTopic not found"));
            return element.buildDto();
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving QuestionTopic: " + e.getMessage());
        }
    }

    public void removeQuestionTopic(Integer questionId, Integer topicAggregateId, UnitOfWork unitOfWork) {
        try {
            Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(questionId, unitOfWork);
            Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);
            newQuestion.getTopics().removeIf(item ->
                item.getTopicAggregateId() != null &&
                item.getTopicAggregateId().equals(topicAggregateId)
            );
            unitOfWorkService.registerChanged(newQuestion, unitOfWork);
            QuestionTopicRemovedEvent event = new QuestionTopicRemovedEvent(questionId, topicAggregateId);
            event.setPublisherAggregateVersion(newQuestion.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error removing QuestionTopic: " + e.getMessage());
        }
    }

    public QuestionTopicDto updateQuestionTopic(Integer questionId, Integer topicAggregateId, QuestionTopicDto QuestionTopicDto, UnitOfWork unitOfWork) {
        try {
            Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(questionId, unitOfWork);
            Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);
            QuestionTopic element = newQuestion.getTopics().stream()
                .filter(item -> item.getTopicAggregateId() != null &&
                               item.getTopicAggregateId().equals(topicAggregateId))
                .findFirst()
                .orElseThrow(() -> new AnswersException("QuestionTopic not found"));

            unitOfWorkService.registerChanged(newQuestion, unitOfWork);
            QuestionTopicUpdatedEvent event = new QuestionTopicUpdatedEvent(questionId, element.getTopicAggregateId(), element.getTopicVersion(), element.getTopicName(), element.getTopicId());
            event.setPublisherAggregateVersion(newQuestion.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return element.buildDto();
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating QuestionTopic: " + e.getMessage());
        }
    }

    public OptionDto addOption(Integer questionId, Integer key, OptionDto OptionDto, UnitOfWork unitOfWork) {
        try {
            Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(questionId, unitOfWork);
            Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);
            Option element = new Option(OptionDto);
            newQuestion.getOptions().add(element);
            unitOfWorkService.registerChanged(newQuestion, unitOfWork);
            return OptionDto;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error adding Option: " + e.getMessage());
        }
    }

    public List<OptionDto> addOptions(Integer questionId, List<OptionDto> OptionDtos, UnitOfWork unitOfWork) {
        try {
            Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(questionId, unitOfWork);
            Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);
            OptionDtos.forEach(dto -> {
                Option element = new Option(dto);
                newQuestion.getOptions().add(element);
            });
            unitOfWorkService.registerChanged(newQuestion, unitOfWork);
            return OptionDtos;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error adding Options: " + e.getMessage());
        }
    }

    public OptionDto getOption(Integer questionId, Integer key, UnitOfWork unitOfWork) {
        try {
            Question question = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(questionId, unitOfWork);
            Option element = question.getOptions().stream()
                .filter(item -> item.getKey() != null &&
                               item.getKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new AnswersException("Option not found"));
            return element.buildDto();
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving Option: " + e.getMessage());
        }
    }

    public void removeOption(Integer questionId, Integer key, UnitOfWork unitOfWork) {
        try {
            Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(questionId, unitOfWork);
            Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);
            newQuestion.getOptions().removeIf(item ->
                item.getKey() != null &&
                item.getKey().equals(key)
            );
            unitOfWorkService.registerChanged(newQuestion, unitOfWork);
            OptionRemovedEvent event = new OptionRemovedEvent(questionId, key);
            event.setPublisherAggregateVersion(newQuestion.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error removing Option: " + e.getMessage());
        }
    }

    public OptionDto updateOption(Integer questionId, Integer key, OptionDto OptionDto, UnitOfWork unitOfWork) {
        try {
            Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(questionId, unitOfWork);
            Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);
            Option element = newQuestion.getOptions().stream()
                .filter(item -> item.getKey() != null &&
                               item.getKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new AnswersException("Option not found"));
            if (OptionDto.getSequence() != null) {
                element.setSequence(OptionDto.getSequence());
            }
            if (OptionDto.getCorrect() != null) {
                element.setCorrect(OptionDto.getCorrect());
            }
            if (OptionDto.getContent() != null) {
                element.setContent(OptionDto.getContent());
            }
            unitOfWorkService.registerChanged(newQuestion, unitOfWork);
            OptionUpdatedEvent event = new OptionUpdatedEvent(questionId, key);
            event.setPublisherAggregateVersion(newQuestion.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return element.buildDto();
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating Option: " + e.getMessage());
        }
    }


    public Question handleTopicUpdatedEvent(Integer aggregateId, Integer topicAggregateId, Integer topicVersion, String topicName, UnitOfWork unitOfWork) {
        try {
            Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);



            unitOfWorkService.registerChanged(newQuestion, unitOfWork);


            return newQuestion;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling TopicUpdatedEvent question: " + e.getMessage());
        }
    }




}