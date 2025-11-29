import type { ValidationChecks } from "langium";
import type { NebulaAstType } from "../generated/ast.js";
import type { NebulaServices } from "../nebula-module.js";
import { NebulaValidator } from "./validators.js";

export function registerValidationChecks(services: NebulaServices) {
    const registry = services.validation.ValidationRegistry;
    const validator = services.validation.NebulaValidator;
    const checks: ValidationChecks<NebulaAstType> = {
        Model: validator.checkModel,
        Aggregate: validator.checkAggregate,
        Entity: validator.checkEntity,
        Property: validator.checkProperty,
        Method: validator.checkMethod,
        Invariant: validator.checkInvariant,
        RepositoryMethod: validator.checkRepositoryMethod,
    };
    registry.register(checks, validator);
}

export { NebulaValidator };

