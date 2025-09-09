import { Aggregate, Entity } from "../../../language/generated/ast.js";
import { TemplateDataBase } from "./template-data-base.js";
import { MethodData, WorkflowData } from "./model-parser.js";

export class MethodWorkflowExtractor extends TemplateDataBase {
    extractMethodsData(aggregate: Aggregate): MethodData[] {
        const methods: MethodData[] = [];
        const entities = aggregate.entities || [];

        entities.forEach(entity => {
            const entityMethods = this.extractEntityMethods(entity);
            methods.push(...entityMethods);
        });

        const aggregateMethods = this.extractAggregateMethods(aggregate);
        methods.push(...aggregateMethods);

        return methods;
    }

    extractEntityMethods(entity: Entity): MethodData[] {
        const methods: MethodData[] = [];
        const entityMethods = (entity as any).methods || [];

        entityMethods.forEach((method: any) => {
            const methodData = this.buildMethodData(method, entity.name);
            methods.push(methodData);
        });


        return methods;
    }

    extractAggregateMethods(aggregate: Aggregate): MethodData[] {
        const methods: MethodData[] = [];
        const aggregateMethods = (aggregate as any).methods || [];

        aggregateMethods.forEach((method: any) => {
            const methodData = this.buildMethodData(method, aggregate.name);
            methods.push(methodData);
        });

        return methods;
    }

    buildMethodData(method: any, contextName: string): MethodData {
        return {
            name: method.name,
            returnType: this.resolveMethodReturnType(method),
            javaReturnType: this.resolveMethodReturnType(method),
            parameters: this.extractMethodParameters(method),
            annotations: this.extractMethodAnnotations(method),
            isPublic: (method.visibility || 'public') === 'public',
            isStatic: method.static || false,
            body: this.generateMethodBody(method, contextName)
        };
    }

    extractMethodParameters(method: any): any[] {
        const parameters = method.parameters || [];

        return parameters.map((param: any) => ({
            name: param.name,
            type: param.type,
            javaType: this.resolveJavaType(param.type),
            isRequired: !param.optional,
            isCollection: this.isCollectionType(param.type),
            isEntity: this.isEntityType(param.type),
            isPrimitive: !this.isEntityType(param.type) && !this.isCollectionType(param.type)
        }));
    }

    extractMethodAnnotations(method: any): string[] {
        const annotations: string[] = [];

        const httpMethod = this.getHttpMethod(method);
        if (httpMethod) {
            annotations.push(`@${httpMethod}Mapping`);
        }

        if (method.validate) {
            annotations.push('@Valid');
        }

        if (method.transactional !== false) {
            annotations.push('@Transactional');
        }

        if (method.secured) {
            annotations.push('@PreAuthorize("hasRole(\'USER\')")');
        }

        if (method.annotations) {
            annotations.push(...method.annotations);
        }

        return annotations;
    }

    generateMethodBody(method: any, contextName: string): string {
        const methodType = this.getMethodType(method);

        switch (methodType) {
            case 'create':
                return this.generateCreateMethodBody(method, contextName);
            case 'update':
                return this.generateUpdateMethodBody(method, contextName);
            case 'delete':
                return this.generateDeleteMethodBody(method, contextName);
            case 'find':
                return this.generateFindMethodBody(method, contextName);
            case 'list':
                return this.generateListMethodBody(method, contextName);
            case 'business':
                return this.generateBusinessMethodBody(method, contextName);
            default:
                return this.generateGenericMethodBody(method, contextName);
        }
    }

    generateCreateMethodBody(method: any, contextName: string): string {
        const entityName = this.capitalize(contextName);
        return `// Create new ${entityName}
        ${entityName} entity = new ${entityName}();
        // Set properties from parameters
        // Validate business rules
        // Save entity
        return repository.save(entity);`;
    }

    generateUpdateMethodBody(method: any, contextName: string): string {
        const entityName = this.capitalize(contextName);
        return `// Find existing ${entityName}
        ${entityName} entity = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("${entityName} not found"));
        
        // Update properties
        // Validate business rules
        // Save changes
        return repository.save(entity);`;
    }

    generateDeleteMethodBody(method: any, contextName: string): string {
        const entityName = this.capitalize(contextName);
        return `// Find existing ${entityName}
        ${entityName} entity = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("${entityName} not found"));
        
        // Check business rules for deletion
        // Perform soft or hard delete
        repository.delete(entity);`;
    }

    generateFindMethodBody(method: any, contextName: string): string {
        const entityName = this.capitalize(contextName);
        return `// Find ${entityName} by criteria
        return repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("${entityName} not found"));`;
    }

    generateListMethodBody(method: any, contextName: string): string {
        return `// List entities with pagination and filtering
        Pageable pageable = PageRequest.of(page, size);
        return repository.findAll(pageable);`;
    }

    generateBusinessMethodBody(method: any, contextName: string): string {
        return `// Business logic for ${method.name}
        // Implement domain-specific business rules
        // Validate preconditions
        // Execute business logic
        // Return result`;
    }

    generateGenericMethodBody(method: any, contextName: string): string {
        return `// Implementation for ${method.name}
        // TODO: Implement method logic
        throw new UnsupportedOperationException("Method not implemented");`;
    }

    getMethodType(method: any): string {
        const name = method.name.toLowerCase();

        if (name.startsWith('create') || name.startsWith('add') || name.startsWith('new')) {
            return 'create';
        }
        if (name.startsWith('update') || name.startsWith('modify') || name.startsWith('edit')) {
            return 'update';
        }
        if (name.startsWith('delete') || name.startsWith('remove')) {
            return 'delete';
        }
        if (name.startsWith('find') || name.startsWith('get') || name.startsWith('retrieve')) {
            return 'find';
        }
        if (name.startsWith('list') || name.startsWith('getall') || name.startsWith('findall')) {
            return 'list';
        }

        return 'business';
    }

    getHttpMethod(method: any): string | null {
        if (method.httpMethod) {
            return method.httpMethod.toUpperCase();
        }

        const methodType = this.getMethodType(method);

        switch (methodType) {
            case 'create':
                return 'POST';
            case 'update':
                return 'PUT';
            case 'delete':
                return 'DELETE';
            case 'find':
            case 'list':
                return 'GET';
            default:
                return 'POST';
        }
    }

    generateEndpoint(method: any, contextName: string): string {
        if (method.endpoint) {
            return method.endpoint;
        }

        const basePath = `/${contextName.toLowerCase()}`;
        const methodType = this.getMethodType(method);

        switch (methodType) {
            case 'create':
                return basePath;
            case 'update':
                return `${basePath}/{id}`;
            case 'delete':
                return `${basePath}/{id}`;
            case 'find':
                return `${basePath}/{id}`;
            case 'list':
                return basePath;
            default:
                return `${basePath}/${this.toKebabCase(method.name)}`;
        }
    }


    extractWorkflowsData(aggregate: Aggregate): WorkflowData[] {
        const workflows: WorkflowData[] = [];
        const aggregateWorkflows = (aggregate as any).workflows || [];

        aggregateWorkflows.forEach((workflow: any) => {
            const workflowData = this.buildWorkflowData(workflow, aggregate.name);
            workflows.push(workflowData);
        });

        return workflows;
    }

    buildWorkflowData(workflow: any, aggregateName: string): WorkflowData {
        return {
            name: workflow.name,
            steps: this.extractWorkflowSteps(workflow),
            parameters: [],
            returnType: 'void',
            javaReturnType: 'void',
            isPublic: true,
            annotations: []
        };
    }

    extractWorkflowSteps(workflow: any): any[] {
        return workflow.steps || [];
    }

    extractWorkflowTriggers(workflow: any): string[] {
        return workflow.triggers || [];
    }

    extractWorkflowConditions(workflow: any): any[] {
        return workflow.conditions || [];
    }

    extractWorkflowActions(workflow: any): any[] {
        return workflow.actions || [];
    }

    extractErrorHandling(workflow: any): any {
        return workflow.errorHandling || {
            strategy: 'retry',
            maxAttempts: 3,
            backoffStrategy: 'exponential'
        };
    }

    extractCompensationLogic(workflow: any): any[] {
        return workflow.compensation || [];
    }

    extractRetryPolicy(workflow: any): any {
        return workflow.retryPolicy || {
            maxAttempts: 3,
            delay: 1000,
            backoffMultiplier: 2.0
        };
    }

    extractStateTransitions(workflow: any): any[] {
        return workflow.stateTransitions || [];
    }

    resolveMethodReturnType(method: any): string {
        if (method.returnType) {
            return this.resolveJavaType(method.returnType);
        }

        const methodType = this.getMethodType(method);

        switch (methodType) {
            case 'create':
            case 'update':
            case 'find':
                return 'Entity';
            case 'delete':
                return 'void';
            case 'list':
                return 'Page<Entity>';
            default:
                return 'Object';
        }
    }

    getParameterAnnotations(param: any): string[] {
        const annotations: string[] = [];

        if (param.annotations) {
            annotations.push(...param.annotations);
        }

        if (param.required) {
            annotations.push('@NotNull');
        }

        if (param.validate) {
            annotations.push('@Valid');
        }

        return annotations;
    }

    getParameterValidation(param: any): any {
        return {
            required: param.required || false,
            minLength: param.minLength,
            maxLength: param.maxLength,
            min: param.min,
            max: param.max,
            pattern: param.pattern,
            customValidator: param.customValidator
        };
    }

    extractMethodValidations(method: any): string[] {
        const validations: string[] = [];

        if (method.validate) {
            validations.push('@Valid');
        }

        if (method.validations) {
            validations.push(...method.validations);
        }

        return validations;
    }

    extractBusinessRules(method: any): string[] {
        const rules: string[] = [];

        if (method.businessRules) {
            rules.push(...method.businessRules);
        }

        const methodType = this.getMethodType(method);
        const methodName = method.name;

        switch (methodType) {
            case 'create':
                rules.push(`validate${this.capitalize(methodName.replace('create', ''))}CreationRules`);
                break;
            case 'update':
                rules.push(`validate${this.capitalize(methodName.replace('update', ''))}UpdateRules`);
                break;
            case 'delete':
                rules.push(`validate${this.capitalize(methodName.replace('delete', ''))}DeletionRules`);
                break;
        }

        return rules;
    }
}
