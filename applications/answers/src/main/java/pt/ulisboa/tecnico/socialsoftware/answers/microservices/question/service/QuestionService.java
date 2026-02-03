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
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish.QuestionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish.QuestionUpdatedEvent;
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
                courseDto.setState(createRequest.getCourse().getState());
                questionDto.setCourse(courseDto);
            }
            if (createRequest.getTopics() != null) {
                questionDto.setTopics(createRequest.getTopics().stream().map(srcDto -> {
                    QuestionTopicDto projDto = new QuestionTopicDto();
                    projDto.setAggregateId(srcDto.getAggregateId());
                    projDto.setVersion(srcDto.getVersion());
                    projDto.setState(srcDto.getState());
                    return projDto;
                }).collect(Collectors.toSet()));
            }
            questionDto.setOptions(createRequest.getOptions());
            
            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Question question = questionFactory.createQuestion(aggregateId, questionDto);
            unitOfWorkService.registerChanged(question, unitOfWork);
            return questionFactory.createQuestionDto(question);
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
        } catch (Exception e) {
            throw new AnswersException("Error retrieving all questions: " + e.getMessage());
        }
    }

    public QuestionDto updateQuestion(QuestionDto questionDto, UnitOfWork unitOfWork) {
        try {
            Integer id = questionDto.getAggregateId();
            Question question = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            if (questionDto.getTitle() != null) {
                question.setTitle(questionDto.getTitle());
            }
            if (questionDto.getContent() != null) {
                question.setContent(questionDto.getContent());
            }
            if (questionDto.getCreationDate() != null) {
                question.setCreationDate(questionDto.getCreationDate());
            }

            unitOfWorkService.registerChanged(question, unitOfWork);
            unitOfWorkService.registerEvent(new QuestionUpdatedEvent(question.getAggregateId(), question.getTitle(), question.getContent(), question.getCreationDate()), unitOfWork);
            return questionFactory.createQuestionDto(question);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating question: " + e.getMessage());
        }
    }

    public void deleteQuestion(Integer id, UnitOfWork unitOfWork) {
        try {
            Question question = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            question.remove();
            unitOfWorkService.registerChanged(question, unitOfWork);
            unitOfWorkService.registerEvent(new QuestionDeletedEvent(question.getAggregateId()), unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting question: " + e.getMessage());
        }
    }






}