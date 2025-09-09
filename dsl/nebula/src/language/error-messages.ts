/**
 * Enhanced error messages for Nebula DSL
 */

export interface ErrorMessage {
    code: string;
    severity: 'error' | 'warning' | 'info';
    message: string;
    suggestion?: string;
    documentation?: string;
    examples?: string[];
}

export class ErrorMessageProvider {
    private static messages: Map<string, ErrorMessage> = new Map();

    static {
        this.initializeMessages();
    }

    private static initializeMessages() {
        // Validation errors
        this.addMessage({
            code: 'DUPLICATE_AGGREGATE_NAME',
            severity: 'error',
            message: 'Duplicate aggregate name: {name}',
            suggestion: 'Aggregate names must be unique within a model. Consider using a different name or check for typos.',
            documentation: 'https://docs.nebula.dev/aggregates#naming',
            examples: [
                'aggregate User { ... }',
                'aggregate UserManagement { ... }  // Different name'
            ]
        });

        this.addMessage({
            code: 'DUPLICATE_ENTITY_NAME',
            severity: 'error',
            message: 'Duplicate entity name: {name}',
            suggestion: 'Entity names must be unique within an aggregate. Consider using a different name or check for typos.',
            documentation: 'https://docs.nebula.dev/entities#naming',
            examples: [
                'entity User { ... }',
                'entity UserProfile { ... }  // Different name'
            ]
        });

        this.addMessage({
            code: 'DUPLICATE_PROPERTY_NAME',
            severity: 'error',
            message: 'Duplicate property name: {name}',
            suggestion: 'Property names must be unique within an entity. Consider using a different name or check for typos.',
            documentation: 'https://docs.nebula.dev/properties#naming',
            examples: [
                'String name;',
                'String fullName;  // Different name'
            ]
        });

        this.addMessage({
            code: 'DUPLICATE_METHOD_NAME',
            severity: 'error',
            message: 'Duplicate method name: {name}',
            suggestion: 'Method names must be unique within an entity or aggregate. Consider using a different name or check for typos.',
            documentation: 'https://docs.nebula.dev/methods#naming',
            examples: [
                'updateEmail(String email): User;',
                'updateUserEmail(String email): User;  // Different name'
            ]
        });

        // Naming convention errors
        this.addMessage({
            code: 'INVALID_AGGREGATE_NAME',
            severity: 'error',
            message: 'Invalid aggregate name: {name}',
            suggestion: 'Aggregate names must start with an uppercase letter and contain only letters, digits, underscores, and dollar signs.',
            documentation: 'https://docs.nebula.dev/aggregates#naming-conventions',
            examples: [
                'aggregate UserManagement { ... }',
                'aggregate OrderProcessing { ... }'
            ]
        });

        this.addMessage({
            code: 'INVALID_ENTITY_NAME',
            severity: 'error',
            message: 'Invalid entity name: {name}',
            suggestion: 'Entity names must start with an uppercase letter and contain only letters, digits, underscores, and dollar signs.',
            documentation: 'https://docs.nebula.dev/entities#naming-conventions',
            examples: [
                'entity User { ... }',
                'entity OrderItem { ... }'
            ]
        });

        this.addMessage({
            code: 'INVALID_PROPERTY_NAME',
            severity: 'error',
            message: 'Invalid property name: {name}',
            suggestion: 'Property names must start with a lowercase letter and contain only letters, digits, underscores, and dollar signs.',
            documentation: 'https://docs.nebula.dev/properties#naming-conventions',
            examples: [
                'String userName;',
                'Integer orderCount;'
            ]
        });

        this.addMessage({
            code: 'INVALID_METHOD_NAME',
            severity: 'error',
            message: 'Invalid method name: {name}',
            suggestion: 'Method names must start with a lowercase letter and contain only letters, digits, underscores, and dollar signs.',
            documentation: 'https://docs.nebula.dev/methods#naming-conventions',
            examples: [
                'updateEmail(String email): User;',
                'calculateTotal(): BigDecimal;'
            ]
        });

        // Reserved word errors
        this.addMessage({
            code: 'RESERVED_WORD_USED',
            severity: 'error',
            message: "'{name}' is a reserved word and cannot be used as {type} name",
            suggestion: 'Choose a different name that is not a Java reserved word or keyword.',
            documentation: 'https://docs.nebula.dev/naming#reserved-words',
            examples: [
                'String className;  // Use "typeName" instead',
                'Integer public;    // Use "isPublic" instead'
            ]
        });

        // Type errors
        this.addMessage({
            code: 'INVALID_PROPERTY_TYPE',
            severity: 'error',
            message: 'Invalid property type: {type}',
            suggestion: 'Use a valid Java type or entity reference. Supported types: String, Integer, Long, Double, Boolean, LocalDateTime, BigDecimal, List<Type>, Set<Type>, Map<Key,Value>.',
            documentation: 'https://docs.nebula.dev/properties#types',
            examples: [
                'String name;',
                'Integer age;',
                'List<String> tags;',
                'User owner;'
            ]
        });

        this.addMessage({
            code: 'INVALID_RETURN_TYPE',
            severity: 'error',
            message: 'Invalid return type: {type}',
            suggestion: 'Use a valid Java type or entity reference for method return types.',
            documentation: 'https://docs.nebula.dev/methods#return-types',
            examples: [
                'String getName(): String;',
                'User findById(Integer id): User;',
                'List<Order> getOrders(): List<Order>;'
            ]
        });

        this.addMessage({
            code: 'INVALID_PARAMETER_TYPE',
            severity: 'error',
            message: 'Invalid parameter type: {type}',
            suggestion: 'Use a valid Java type or entity reference for method parameters.',
            documentation: 'https://docs.nebula.dev/methods#parameters',
            examples: [
                'updateName(String newName): void;',
                'addItem(OrderItem item): Order;'
            ]
        });

        // Structure errors
        this.addMessage({
            code: 'MISSING_ROOT_ENTITY',
            severity: 'warning',
            message: 'Aggregate should have at least one root entity',
            suggestion: 'Add a root entity to the aggregate by setting isRoot = true on one of the entities.',
            documentation: 'https://docs.nebula.dev/aggregates#root-entity',
            examples: [
                'entity User {',
                '    isRoot = true',
                '    // properties and methods',
                '}'
            ]
        });

        this.addMessage({
            code: 'MULTIPLE_ROOT_ENTITIES',
            severity: 'error',
            message: 'Aggregate can only have one root entity',
            suggestion: 'Remove the isRoot = true from all but one entity, or create separate aggregates.',
            documentation: 'https://docs.nebula.dev/aggregates#root-entity',
            examples: [
                'entity User {',
                '    isRoot = true  // Only one root entity',
                '}',
                'entity UserProfile {',
                '    // Not a root entity',
                '}'
            ]
        });

        this.addMessage({
            code: 'MISSING_ID_PROPERTY',
            severity: 'warning',
            message: 'Root entity should have an \'id\' property or a key property',
            suggestion: 'Add an id property or mark a property as key for the root entity.',
            documentation: 'https://docs.nebula.dev/entities#root-entity-properties',
            examples: [
                'entity User {',
                '    isRoot = true',
                '    Integer id;  // Add id property',
                '    // other properties',
                '}'
            ]
        });

        // Invariant errors
        this.addMessage({
            code: 'EMPTY_INVARIANT_CONDITION',
            severity: 'error',
            message: 'Invariant must have at least one condition',
            suggestion: 'Add a condition expression to the invariant.',
            documentation: 'https://docs.nebula.dev/invariants#conditions',
            examples: [
                'invariants {',
                '    nameNotEmpty: name.length() > 0;',
                '    positiveAge: age > 0;',
                '}'
            ]
        });

        this.addMessage({
            code: 'INVALID_INVARIANT_SYNTAX',
            severity: 'warning',
            message: 'Invariant condition should end with semicolon',
            suggestion: 'Add a semicolon at the end of the invariant condition.',
            documentation: 'https://docs.nebula.dev/invariants#syntax',
            examples: [
                'invariants {',
                '    nameNotEmpty: name.length() > 0;  // Add semicolon',
                '}'
            ]
        });

        // Business rule errors
        this.addMessage({
            code: 'EMPTY_BUSINESS_RULE_CONDITIONS',
            severity: 'error',
            message: 'Business rule must have at least one condition',
            suggestion: 'Add at least one condition to the business rule.',
            documentation: 'https://docs.nebula.dev/business-rules#conditions',
            examples: [
                'businessRules {',
                '    cannotCancelCompleted {',
                '        conditions: ["status == \'COMPLETED\'", "cancelled == true"];',
                '        exception: "Cannot cancel a completed order";',
                '    }',
                '}'
            ]
        });

        this.addMessage({
            code: 'MISSING_BUSINESS_RULE_EXCEPTION',
            severity: 'warning',
            message: 'Business rule should have an exception message',
            suggestion: 'Add an exception message to describe what happens when the rule is violated.',
            documentation: 'https://docs.nebula.dev/business-rules#exceptions',
            examples: [
                'businessRules {',
                '    cannotCancelCompleted {',
                '        conditions: ["status == \'COMPLETED\'"];',
                '        exception: "Cannot cancel a completed order";  // Add exception message',
                '    }',
                '}'
            ]
        });

        this.addMessage({
            code: 'EMPTY_MODEL',
            severity: 'warning',
            message: 'Model contains no aggregates',
            suggestion: 'Add at least one aggregate to the model.',
            documentation: 'https://docs.nebula.dev/models#aggregates',
            examples: [
                'aggregate UserManagement {',
                '    // entities and methods',
                '}'
            ]
        });

        // Collection type errors
        this.addMessage({
            code: 'MISSING_COLLECTION_ELEMENT_TYPE',
            severity: 'error',
            message: 'Collection property must specify element type',
            suggestion: 'Specify the element type for the collection (e.g., List<String>, Set<User>).',
            documentation: 'https://docs.nebula.dev/properties#collection-types',
            examples: [
                'List<String> tags;',
                'Set<User> members;',
                'Map<String, Object> metadata;'
            ]
        });

        // Architecture errors
        this.addMessage({
            code: 'ARCHITECTURE_MISSING_REQUIRED_FEATURES',
            severity: 'error',
            message: 'Missing required features for {architecture}: {features}',
            suggestion: 'Add the missing features to your generation command or update the architecture configuration.',
            documentation: 'https://docs.nebula.dev/architectures#features',
            examples: [
                './bin/cli.js generate file.nebula --features entities,services,repositories'
            ]
        });

        this.addMessage({
            code: 'ARCHITECTURE_INVALID_FEATURE',
            severity: 'warning',
            message: 'Unknown feature: {feature}',
            suggestion: 'Use a valid feature name. Available features: entities, dtos, services, factories, repositories, events, coordination, webapi, validation, saga, integration.',
            documentation: 'https://docs.nebula.dev/features#available-features',
            examples: [
                './bin/cli.js generate file.nebula --features entities,services,webapi'
            ]
        });

        // Template errors
        this.addMessage({
            code: 'TEMPLATE_NOT_FOUND',
            severity: 'error',
            message: 'Template not found: {template}',
            suggestion: 'Check that the template file exists and the path is correct.',
            documentation: 'https://docs.nebula.dev/templates#template-structure',
            examples: [
                'Ensure template files are in the templates/ directory',
                'Check template naming conventions'
            ]
        });

        this.addMessage({
            code: 'TEMPLATE_RENDER_ERROR',
            severity: 'error',
            message: 'Error rendering template: {error}',
            suggestion: 'Check the template syntax and context variables.',
            documentation: 'https://docs.nebula.dev/templates#template-syntax',
            examples: [
                'Check variable names in template',
                'Verify template syntax'
            ]
        });

        // File system errors
        this.addMessage({
            code: 'FILE_NOT_FOUND',
            severity: 'error',
            message: 'File not found: {file}',
            suggestion: 'Check that the file exists and the path is correct.',
            documentation: 'https://docs.nebula.dev/cli#file-paths',
            examples: [
                './bin/cli.js generate ./path/to/file.nebula',
                'Check file permissions'
            ]
        });

        this.addMessage({
            code: 'OUTPUT_DIRECTORY_ERROR',
            severity: 'error',
            message: 'Cannot create output directory: {directory}',
            suggestion: 'Check that you have write permissions to the output directory.',
            documentation: 'https://docs.nebula.dev/cli#output-directory',
            examples: [
                './bin/cli.js generate file.nebula --output ./my-output',
                'Check directory permissions'
            ]
        });
    }

    private static addMessage(message: ErrorMessage) {
        this.messages.set(message.code, message);
    }

    static getMessage(code: string, context?: Record<string, any>): ErrorMessage {
        const message = this.messages.get(code);
        if (!message) {
            return {
                code,
                severity: 'error',
                message: `Unknown error: ${code}`,
                suggestion: 'This is an unknown error. Please report it to the development team.'
            };
        }

        // Replace placeholders in message
        let formattedMessage = message.message;
        if (context) {
            for (const [key, value] of Object.entries(context)) {
                formattedMessage = formattedMessage.replace(`{${key}}`, String(value));
            }
        }

        return {
            ...message,
            message: formattedMessage
        };
    }

    static getAllMessages(): ErrorMessage[] {
        return Array.from(this.messages.values());
    }

    static getMessagesBySeverity(severity: 'error' | 'warning' | 'info'): ErrorMessage[] {
        return Array.from(this.messages.values()).filter(msg => msg.severity === severity);
    }

    static formatErrorMessage(error: ErrorMessage, context?: Record<string, any>): string {
        let formatted = `[${error.code}] ${error.message}`;

        if (error.suggestion) {
            formatted += `\n💡 Suggestion: ${error.suggestion}`;
        }

        if (error.documentation) {
            formatted += `\n📚 Documentation: ${error.documentation}`;
        }

        if (error.examples && error.examples.length > 0) {
            formatted += `\n📝 Examples:\n${error.examples.map(ex => `  ${ex}`).join('\n')}`;
        }

        return formatted;
    }
}
