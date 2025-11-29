import type { ValidationAcceptor } from "langium";
import type { Method } from "../generated/ast.js";
import { NamingValidator } from "./naming-validator.js";

export class MethodValidator {
    private readonly javaNamingPattern = /^[a-zA-Z_$][a-zA-Z0-9_$]*$/;

    constructor(private readonly namingValidator: NamingValidator) { }

    checkMethod(method: Method, accept: ValidationAcceptor): void {
        this.namingValidator.validateName(method.name, "method", method, accept);

        const paramNames = new Set<string>();
        for (const param of method.parameters || []) {
            if (param.name && paramNames.has(param.name.toLowerCase())) {
                accept("error", `Duplicate parameter name: ${param.name}`, {
                    node: param,
                    property: "name",
                });
            } else if (param.name) {
                paramNames.add(param.name.toLowerCase());
            }
        }

        if (method.returnType && typeof method.returnType === 'string') {
            if (!this.javaNamingPattern.test(method.returnType)) {
                accept("error", `Invalid return type: ${method.returnType}`, {
                    node: method,
                    property: "returnType",
                });
            }
        }
    }
}

