import type { ValidationAcceptor } from "langium";
import type { SubscribedEvent } from "../../generated/ast.js";

// Minimal event validator after simplifying the event subscription model.
// We currently don't enforce any semantic checks beyond basic shape, since
// many events (standard CRUD events) are generated implicitly.

export class EventValidator {
    checkSubscribedEvent(_event: SubscribedEvent, _accept: ValidationAcceptor): void {
        // Intentionally left minimal – no validation errors.
    }
}

