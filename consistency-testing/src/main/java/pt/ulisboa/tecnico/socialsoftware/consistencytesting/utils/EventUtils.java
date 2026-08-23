package pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandling;

public class EventUtils {

    private EventUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static void validateEventHandlingRegistration(Collection<? extends Class<?>> beanTypes) {
        List<String> missingRegistrations = beanTypes.stream()
                .map(ClassUtils::getUserClass)
                .distinct()
                .filter(beanType -> beanType.getSimpleName().endsWith(EventHandling.class.getSimpleName()))
                .filter(EventUtils::hasScheduledMethods)
                .filter(beanType -> !EventHandling.class.isAssignableFrom(beanType))
                .map(Class::getName)
                .sorted()
                .toList();

        if (!missingRegistrations.isEmpty()) {
            throw new IllegalStateException(
                    "Scheduled event-handling beans must implement [%s], but the following beans do not: %s"
                            .formatted(EventHandling.class.getName(), missingRegistrations).strip());
        }
    }

    public static void runEventHandlingScheduledTasks(Collection<? extends EventHandling> eventHandlings) {
        for (EventHandling eventHandling : eventHandlings) {
            Class<?> targetClass = AopUtils.getTargetClass(eventHandling);

            ReflectionUtils.doWithMethods(targetClass, method -> {
                // Matches Spring's ScheduledAnnotationBeanPostProcessor logic
                // to find methods annotated with @Scheduled or @Schedules.
                boolean isScheduled = AnnotatedElementUtils.hasAnnotation(method, Scheduled.class)
                        || AnnotatedElementUtils.hasAnnotation(method, Schedules.class);

                if (isScheduled) {
                    ReflectionUtils.makeAccessible(method);
                    try {
                        method.invoke(eventHandling);
                    } catch (InvocationTargetException e) {
                        throw new IllegalStateException("Scheduled handler [%s.%s] failed"
                                .formatted(targetClass.getName(), method.getName()), e.getCause());
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException("Access denied invoking scheduled handler [%s.%s]"
                                .formatted(targetClass.getName(), method.getName()), e);
                    }
                }
            });
        }
    }

    private static boolean hasScheduledMethods(Class<?> beanType) {
        return Arrays.stream(ReflectionUtils.getAllDeclaredMethods(beanType))
                .anyMatch(method -> AnnotatedElementUtils.hasAnnotation(method, Scheduled.class)
                        || AnnotatedElementUtils.hasAnnotation(method, Schedules.class));
    }
}
