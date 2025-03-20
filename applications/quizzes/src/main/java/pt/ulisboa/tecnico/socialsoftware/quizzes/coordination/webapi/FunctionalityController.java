package pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.webapi;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import  pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.functionalities.FunctionalityFactory;

@RestController
public class FunctionalityController {
    
    @Autowired
    private FunctionalityFactory functionalityFactory;

    @PostMapping(value = "/functionality/execute/{functionalityName}")
    public void executeFunctionality(@PathVariable String functionalityName, @RequestBody Object[] constructorArgs) throws Exception {
        functionalityFactory.executeFunctionality(functionalityName, constructorArgs);
    }
}
    

