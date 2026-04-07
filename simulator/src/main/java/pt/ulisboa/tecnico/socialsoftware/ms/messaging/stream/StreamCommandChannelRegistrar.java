package pt.ulisboa.tecnico.socialsoftware.ms.messaging.stream;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ResolvableType;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;

import java.lang.reflect.Modifier;
import java.util.function.Consumer;
import java.util.logging.Logger;

@Component
@Profile("stream")
public class StreamCommandChannelRegistrar implements BeanDefinitionRegistryPostProcessor {

    private static final Logger logger = Logger.getLogger(StreamCommandChannelRegistrar.class.getName());

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        for (String beanName : registry.getBeanDefinitionNames()) {
            if (!beanName.endsWith("CommandHandler")) continue;

            BeanDefinition bd = registry.getBeanDefinition(beanName);
            String className = bd.getBeanClassName();
            if (className == null) continue;

            try {
                Class<?> clazz = Class.forName(className);
                if (CommandHandler.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                    String serviceName = beanName.replace("CommandHandler", "");
                    String channelBeanName = serviceName + "ServiceCommandChannel";

                    if (registry.containsBeanDefinition(channelBeanName)) continue;

                    logger.info("Auto-registering stream command channel: " + channelBeanName
                            + " for handler: " + beanName);

                    RootBeanDefinition channelDef = new RootBeanDefinition();
                    channelDef.setTargetType(ResolvableType.forClassWithGenerics(
                            Consumer.class, Message.class));
                    channelDef.setInstanceSupplier(() -> {
                        StreamCommandService service = ((ConfigurableListableBeanFactory) registry)
                                .getBean(StreamCommandService.class);
                        return (Consumer<Message<?>>) service::handleCommandMessage;
                    });
                    registry.registerBeanDefinition(channelBeanName, channelDef);
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // No-op
    }
}
