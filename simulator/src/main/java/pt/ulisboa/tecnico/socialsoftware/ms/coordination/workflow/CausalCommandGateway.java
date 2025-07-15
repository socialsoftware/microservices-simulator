package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Component
@Profile("tcc")
public class CausalCommandGateway extends CommandGateway {

    public CausalCommandGateway(ApplicationContext applicationContext) {
        super(applicationContext);
    }
}
