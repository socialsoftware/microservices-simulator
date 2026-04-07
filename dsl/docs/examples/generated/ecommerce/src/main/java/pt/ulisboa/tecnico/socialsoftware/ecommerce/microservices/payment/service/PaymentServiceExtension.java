package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.service;

import org.springframework.stereotype.Component;

/**
 * User-owned extension for PaymentService.
 *
 * This file is generated ONCE by Nebula and never overwritten on
 * subsequent regenerations. It is the regeneration-safe place to put
 * any hand-written Java that the DSL cannot express declaratively
 * (third-party SDK calls, complex computation, etc.).
 *
 * Methods you add here can be invoked from a .nebula method body via
 * the `extension methodName(args)` action.
 */
@Component
public class PaymentServiceExtension {
    public void recordAuthorizationAttempt(Integer orderId, Double amountInCents) {
        System.out.println("[ext] payment authorize attempt: order=" + orderId + " amount=" + amountInCents);
    }
}
