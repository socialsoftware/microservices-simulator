package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils.EventUtils;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandling;

/**
 * Brings a catalog's setup state to event-delivery quiescence.
 * <p>
 * Event deliveries are first captured, then executed after capture has closed.
 * A final empty capture pass is the quiescence assertion: persisted events without
 * a matching delivery are allowed, but no registered handler may still have work to perform.
 */
final class BaselineEventDrainer {

    private static final Comparator<DeferredEventInvocation> DELIVERY_ORDER = Comparator
            .comparing(DeferredEventInvocation::eventId)
            .thenComparing(invocation -> invocation.handler().getClass().getName())
            .thenComparing(DeferredEventInvocation::subscriberAggregateId);

    private BaselineEventDrainer() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    static void drainToQuiescence(
            Set<EventHandling> eventHandlings,
            DeferredEventApplicationService eventApplicationService) {

        drainToQuiescence(eventHandlings, eventApplicationService, ScheduleExecutor.STEP_EXECUTION_LIMIT);
    }

    static void drainToQuiescence(
            Set<EventHandling> eventHandlings,
            DeferredEventApplicationService eventApplicationService,
            int deliveryLimit) {

        Objects.requireNonNull(eventHandlings, "Event handlings cannot be null");
        Objects.requireNonNull(eventApplicationService, "Deferred event application service cannot be null");
        if (deliveryLimit < 1) {
            throw new IllegalArgumentException("Delivery limit must be >= 1, got " + deliveryLimit);
        }

        int executedDeliveries = 0;
        while (true) {
            List<DeferredEventInvocation> pendingDeliveries = capturePendingDeliveries(
                    eventHandlings, eventApplicationService);

            if (pendingDeliveries.isEmpty()) {
                return;
            }

            if (executedDeliveries + pendingDeliveries.size() > deliveryLimit) {
                throw new IllegalStateException(
                        ("Catalog setup did not reach event-delivery quiescence after %d handled delivery(ies) "
                                + "(limit=%d); %d delivery(ies) remain pending: %s")
                                        .formatted(executedDeliveries, deliveryLimit, pendingDeliveries.size(),
                                                pendingDeliveries.stream()
                                                        .map(BaselineEventDrainer::describe).toList()));
            }

            for (DeferredEventInvocation invocation : pendingDeliveries) {
                invocation.invocation().run();
                executedDeliveries++;
            }
        }
    }

    private static List<DeferredEventInvocation> capturePendingDeliveries(
            Set<EventHandling> eventHandlings,
            DeferredEventApplicationService eventApplicationService) {

        try (DeferredEventApplicationService.CaptureSession captureSession = eventApplicationService.beginCapture()) {
            EventUtils.runEventHandlingScheduledTasks(eventHandlings);
            return captureSession.drain().stream().sorted(DELIVERY_ORDER).toList();
        }
    }

    private static String describe(DeferredEventInvocation invocation) {
        return "eventId=%d, handler=%s, subscriberAggregateId=%d".formatted(
                invocation.eventId(), invocation.handler().getClass().getName(), invocation.subscriberAggregateId());
    }
}
