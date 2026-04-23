import type { ValidationAcceptor } from "langium";
import type { Property } from "../generated/ast.js";
import { NamingValidator } from "./naming-validator.js";

export class PropertyValidator {
    constructor(private readonly namingValidator: NamingValidator) { }

    checkProperty(property: Property, accept: ValidationAcceptor): void {
        this.namingValidator.validateName(property.name, "property", property, accept);

        if (!property.type) {
            accept("error", "Property must have a type", {
                node: property,
                property: "type",
            });
        }

        if (property.type && typeof property.type === 'object' && 'elementType' in property.type) {
            if (!property.type.elementType) {
                accept("error", "Collection property must specify element type", {
                    node: property,
                    property: "type",
                });
            }
        }
    }
}

