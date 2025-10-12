import { Model, Aggregate, Entity, WebAPIEndpoints as AstWebAPIEndpoints } from "../../../../language/generated/ast.js";

export type ExceptionSeverity = "error" | "warning" | "info";

export interface ExceptionMessageData {
    name: string;
    message: string;
    code?: string;
    severity: ExceptionSeverity;
}

export interface BusinessRuleExceptionRef {
    aggregate: string;
    entity: string;
    ruleName: string;
    exceptionName: string;
    affectedFields: string[];
}

export interface EndpointExceptionRef {
    aggregate: string;
    endpointName: string;
    methodName?: string;
    throwsException: boolean;
}

export interface ExceptionsSummary {
    messages: ExceptionMessageData[];
    businessRuleExceptions: BusinessRuleExceptionRef[];
    endpointExceptions: EndpointExceptionRef[];
}

export class ExceptionParser {
    parseModel(model: Model): ExceptionsSummary {
        const messages = this.parseExceptionMessages(model);
        const businessRuleExceptions = this.parseBusinessRuleExceptions(model);
        const endpointExceptions = this.parseEndpointExceptions(model);

        return { messages, businessRuleExceptions, endpointExceptions };
    }

    private parseExceptionMessages(model: Model): ExceptionMessageData[] {
        if (!(model as any).exceptions) return [];
        const exceptions = (model as any).exceptions;
        const rawMessages: any[] = exceptions.messages || [];

        return rawMessages.map((msg: any) => ({
            name: msg.name,
            message: msg.message,
            code: msg.code, // optional in grammar; tolerate absence
            severity: (msg.severity as ExceptionSeverity) || "error"
        }));
    }

    private parseBusinessRuleExceptions(model: Model): BusinessRuleExceptionRef[] {
        const refs: BusinessRuleExceptionRef[] = [];

        for (const aggregate of model.aggregates as Aggregate[]) {
            for (const entity of aggregate.entities as Entity[]) {
                const rules: any[] = []; // Business rules removed
                for (const rule of rules) {
                    const exceptionLiteral = (rule as any).exception as string | undefined;
                    if (exceptionLiteral) {
                        refs.push({
                            aggregate: aggregate.name,
                            entity: entity.name,
                            ruleName: (rule as any).name || "rule",
                            exceptionName: this.stripQuotes(exceptionLiteral),
                            affectedFields: ((rule as any).fields || []).map((f: any) => String(f))
                        });
                    }
                }
            }
        }

        return refs;
    }

    private parseEndpointExceptions(model: Model): EndpointExceptionRef[] {
        const refs: EndpointExceptionRef[] = [];

        for (const aggregate of model.aggregates as Aggregate[]) {
            const endpointsBlock: AstWebAPIEndpoints | undefined = (aggregate as any).webApiEndpoints;
            if (!endpointsBlock) continue;

            const endpoints: any[] = (endpointsBlock as any).endpoints || [];
            for (const ep of endpoints) {
                const throwsException = Boolean((ep as any).throwsException);
                refs.push({
                    aggregate: aggregate.name,
                    endpointName: ep.name,
                    methodName: (ep as any).methodName,
                    throwsException
                });
            }
        }

        return refs;
    }

    private stripQuotes(value: string): string {
        if (value.length >= 2 && ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length - 1);
        }
        return value;
    }
}
