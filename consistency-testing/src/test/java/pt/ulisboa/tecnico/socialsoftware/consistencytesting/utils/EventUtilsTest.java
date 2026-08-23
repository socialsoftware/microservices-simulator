package pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandling;

class EventUtilsTest {

    @Test
    void rejectsScheduledEventHandlingBeanWithoutMarkerInterface() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> EventUtils.validateEventHandlingRegistration(List.of(UnregisteredEventHandling.class)));

        assertEquals(
                "Scheduled event-handling beans must implement [%s], but the following beans do not: %s"
                        .formatted(EventHandling.class.getName(), List.of(UnregisteredEventHandling.class.getName()))
                        .strip(),
                exception.getMessage());
    }

    @Test
    void acceptsRegisteredEventHandlingBean() {
        assertDoesNotThrow(
                () -> EventUtils.validateEventHandlingRegistration(List.of(RegisteredEventHandling.class)));
    }

    @Test
    void ignoresUnrelatedScheduledBean() {
        assertDoesNotThrow(() -> EventUtils.validateEventHandlingRegistration(List.of(UnrelatedScheduler.class)));
    }

    private static class UnregisteredEventHandling {
        @Scheduled(fixedDelay = 1000)
        void handleEvents() {
        }
    }

    private static class RegisteredEventHandling implements EventHandling {
        @Scheduled(fixedDelay = 1000)
        void handleEvents() {
        }
    }

    private static class UnrelatedScheduler {
        @Scheduled(fixedDelay = 1000)
        void runMaintenance() {
        }
    }
}
