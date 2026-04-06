package pt.ulisboa.tecnico.socialsoftware.ms.domain.event;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
import java.util.logging.Logger;

@Component
@Profile("remote")
public class EventSubscriberRegistrar implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private static final Logger logger = Logger.getLogger(EventSubscriberRegistrar.class.getName());
    private Environment environment;

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    // This method dynamically registers Spring Cloud Stream function beans for event subscribers based on the spring.cloud.function.definition property.
    @Override
    public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) throws BeansException {
        String functionDefinition = environment.getProperty("spring.cloud.function.definition", "");
        if (functionDefinition.isBlank()) {
            logger.warning("No spring.cloud.function.definition property found. Cannot register event subscribers dynamically.");
            return;
        }

        String[] functionNames = functionDefinition.split(";");
        for (String name : functionNames) {
            String subscriberBeanName = name.trim();
            if (!subscriberBeanName.endsWith("EventSubscriber")) {
                continue;
            }

            if (registry.containsBeanDefinition(subscriberBeanName)) {
                logger.info("Subscriber bean " + subscriberBeanName + " already exists. Skipping dynamic registration.");
                continue;
            }

            logger.info("Auto-registering stream event subscriber: " + subscriberBeanName);

            RootBeanDefinition channelDef = new RootBeanDefinition();
            channelDef.setTargetType(ResolvableType.forClassWithGenerics(Consumer.class, ResolvableType.forClassWithGenerics(Message.class, String.class)));
            channelDef.setInstanceSupplier(() -> {
                EventSubscriberService service = ((ConfigurableListableBeanFactory) registry).getBean(EventSubscriberService.class);
                return (Consumer<Message<?>>) message -> {
                    @SuppressWarnings("unchecked") Message<String> stringMessage = (Message<String>) message;
                    service.processEvent(stringMessage);
                };
            });
            registry.registerBeanDefinition(subscriberBeanName, channelDef);
        }
    }

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // No-op
    }
}
