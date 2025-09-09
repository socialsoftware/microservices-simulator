package com.generated.microservices.answers.coordination.functionalities;

import static com.generated.microservices.ms.TransactionalModel.SAGAS;
import static com.generated.microservices.answers.microservices.exception.AnswersErrorMessage.*;

import static com.generated.microservices.ms.TransactionalModel.SAGAS;
import static com.generated.microservices.answers.microservices.exception.AnswersErrorMessage.*;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import com.generated.microservices.answers.microservices.exception.AnswersException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import com.generated.microservices.ms.TransactionalModel;
import com.generated.microservices.ms.coordination.unitOfWork.UnitOfWork;
import com.generated.microservices.answers.microservices.quiz.service.QuizService;
import com.generated.microservices.answers.microservices.quiz.aggregate.QuizDto;
import com.generated.microservices.answers.microservices.quiz.aggregate.QuizCourseExecutionDto;
import com.generated.microservices.answers.microservices.quiz.aggregate.QuizQuestionDto;
import com.generated.microservices.answers.microservices.quiz.aggregate.QuizOptionDto;

@Service
public class QuizFunctionalities {

private QuizService quizService;

@Autowired
private Environment env;

private TransactionalModel workflowType;

@PostConstruct
public void init() {
String[] activeProfiles = env.getActiveProfiles();
if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
workflowType = SAGAS;
} else {
throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
}
}

[object Object],[object Object],[object Object],[object Object],[object Object],[object Object],[object Object],[object Object],[object Object]
}