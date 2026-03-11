package pt.ulisboa.tecnico.socialsoftware.ms.domain.event;

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
public class StreamEventSubscriberRegistrar implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private static final Logger logger = Logger.getLogger(StreamEventSubscriberRegistrar.class.getName());
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        String appName = environment.getProperty("spring.application.name");
        if (appName == null || appName.isBlank()) {
            logger.warning("No spring.application.name property found. Cannot register event subscriber dynamically.");
            return;
        }

        String subscriberBeanName = appName + "EventSubscriber";

        if (registry.containsBeanDefinition(subscriberBeanName)) {
            logger.info("Subscriber bean " + subscriberBeanName + " already exists. Skipping dynamic registration.");
            return;
        }

        logger.info("Auto-registering stream event subscriber: " + subscriberBeanName);

        RootBeanDefinition channelDef = new RootBeanDefinition();
        channelDef.setTargetType(ResolvableType.forClassWithGenerics(Consumer.class, ResolvableType.forClassWithGenerics(Message.class, String.class)));
        channelDef.setInstanceSupplier(() -> {
            EventSubscriberService service = ((ConfigurableListableBeanFactory) registry).getBean(EventSubscriberService.class);
            return (Consumer<Message<?>>) message -> {
                // Ensure proper wildcard generics matching for processEvent
                @SuppressWarnings("unchecked") Message<String> stringMessage = (Message<String>) message;
                service.processEvent(stringMessage);
            };
        });
        registry.registerBeanDefinition(subscriberBeanName, channelDef);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // No-op
    }
}
