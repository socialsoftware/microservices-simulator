import type { ValidationAcceptor } from "langium";
import type { Invariant } from "../generated/ast.js";
import { NamingValidator } from "./naming-validator.js";

export class InvariantValidator {
    constructor(private readonly namingValidator: NamingValidator) { }

    checkInvariant(invariant: Invariant, accept: ValidationAcceptor): void {
        this.namingValidator.validateName(invariant.name, "invariant", invariant, accept);

        if (!invariant.conditions || invariant.conditions.length === 0) {
            accept("error", "Invariant must have at least one condition", {
                node: invariant,
                property: "conditions",
            });
        }

        for (const condition of invariant.conditions || []) {
            if (condition && typeof condition === 'object' && 'expression' in condition) {
                const expr = (condition as any).expression;
                if (expr && typeof expr === 'string' && !expr.includes(';')) {
                    accept("warning", "Invariant condition should end with semicolon", {
                        node: invariant,
                        property: "conditions",
                    });
                }
            }
        }
    }
}

