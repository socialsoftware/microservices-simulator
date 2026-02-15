


import { Aggregate } from "../../../../language/generated/ast.js";



export interface ProcessedAction {
    action: string;
    [key: string]: any;
}



export class MethodImplementationProcessor {

    

    processMethodImplementation(implementation: any, aggregate: Aggregate): ProcessedAction[] {
        if (!implementation?.actions) {
            return [];
        }

        return implementation.actions.map((action: any) => this.processAction(action, aggregate));
    }

    

    private processAction(action: any, aggregate: Aggregate): ProcessedAction {
        switch (action.$type) {
            case 'LoadAggregateAction':
                return this.processLoadAggregateAction(action, aggregate);

            case 'ValidateAction':
                return this.processValidateAction(action);

            case 'CreateEntityAction':
                return this.processCreateEntityAction(action);

            case 'DomainOperationAction':
                return this.processDomainOperationAction(action);

            case 'RegisterChangeAction':
                return this.processRegisterChangeAction(action);

            case 'RegisterEventAction':
                return this.processRegisterEventAction(action);

            case 'PublishEventAction':
                return this.processPublishEventAction(action);

            case 'ReturnAction':
                return this.processReturnAction(action);

            default:
                return this.processUnknownAction(action);
        }
    }

    private processLoadAggregateAction(action: any, aggregate: Aggregate): ProcessedAction {
        return {
            action: 'load',
            aggregateVar: action.aggregateVar,
            aggregateType: aggregate.name,
            aggregateId: action.aggregateId,
            unitOfWorkVar: 'unitOfWork'
        };
    }

    private processValidateAction(action: any): ProcessedAction {
        return {
            action: 'validate',
            condition: action.condition,
            exception: action.exception,
            exceptionParams: action.exceptionParams || []
        };
    }

    private processCreateEntityAction(action: any): ProcessedAction {
        return {
            action: 'create',
            entityVar: action.entityVar,
            entityType: action.entityType,
            constructorParam: action.constructorParams?.[0] || 'oldExecution'
        };
    }

    private processDomainOperationAction(action: any): ProcessedAction {
        return {
            action: 'execute',
            targetVar: action.targetVar,
            operationChain: this.processOperationChain(action.operationChain)
        };
    }

    private processRegisterChangeAction(action: any): ProcessedAction {
        return {
            action: 'register',
            aggregateVar: action.aggregateVar,
            unitOfWorkVar: action.unitOfWorkVar
        };
    }

    private processRegisterEventAction(action: any): ProcessedAction {
        return {
            action: 'registerEvent',
            eventType: action.eventType?.ref?.name || action.eventType?.$refText,
            eventParams: action.eventParams || [],
            unitOfWorkVar: action.unitOfWorkVar
        };
    }

    private processPublishEventAction(action: any): ProcessedAction {
        return {
            action: 'publish',
            eventType: action.eventType?.ref?.name || action.eventType?.$refText,
            eventParams: action.eventParams || []
        };
    }

    private processReturnAction(action: any): ProcessedAction {
        return {
            action: 'return',
            returnValue: action.returnValue,
            returnExpression: action.returnExpression
        };
    }

    private processUnknownAction(action: any): ProcessedAction {
        return {
            action: 'unknown',
            raw: action
        };
    }

    

    private processOperationChain(operationChain: any): string {
        if (!operationChain?.operations) {
            return '';
        }

        return operationChain.operations.map((operation: any) => {
            const methodName = operation.methodName;
            const params = operation.params?.map((param: any) => {
                
                if (param === 'INACTIVE') {
                    return 'Aggregate.AggregateState.INACTIVE';
                }
                return param;
            }).join(', ') || '';

            return params ? `${methodName}(${params})` : methodName;
        }).join('.');
    }
}
