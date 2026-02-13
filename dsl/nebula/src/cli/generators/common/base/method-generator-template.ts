/**
 * Method Generator Template (Template Method Pattern)
 *
 * Provides a standardized structure for generating Java methods across different contexts:
 * - Service CRUD methods (create, read, update, delete)
 * - Saga CRUD methods (with compensation logic)
 * - Collection methods (add, remove, update, get)
 * - Custom service methods
 *
 * The Template Method pattern defines the skeleton of the generation algorithm,
 * while allowing subclasses to override specific steps.
 *
 * Benefits:
 * - Consistent structure across all method generators
 * - Reduces code duplication
 * - Easier to maintain and extend
 * - Enforces best practices (error handling, event publishing, etc.)
 */

import { Aggregate } from "../../../../language/generated/ast.js";
import { GeneratorBase, GeneratorOptions } from "./generator-base.js";
import { ExceptionGenerator } from "../utils/exception-generator.js";

/**
 * Metadata extracted from aggregate for method generation
 */
export interface MethodMetadata {
    methodName: string;
    aggregateName: string;
    entityName: string;
    projectName: string;
    parameters: MethodParameter[];
    returnType: string;
    throwsExceptions?: string[];
    annotations?: string[];
    [key: string]: any;  // Allow subclasses to add custom metadata
}

/**
 * Method parameter definition
 */
export interface MethodParameter {
    name: string;
    type: string;
    annotations?: string[];
}

/**
 * Abstract base class implementing Template Method pattern for method generation.
 *
 * Defines the overall structure of method generation:
 * 1. Extract metadata from aggregate
 * 2. Build method signature
 * 3. Build method body
 * 4. Build event handling (optional)
 * 5. Assemble final method
 *
 * Subclasses override abstract methods to customize each step.
 */
export abstract class MethodGeneratorTemplate extends GeneratorBase {

    constructor(options?: GeneratorOptions) {
        super(options);
    }

    /**
     * Template method - defines the algorithm skeleton.
     *
     * This method orchestrates the entire method generation process.
     * Subclasses cannot override this - they override the individual steps instead.
     *
     * @param aggregate The aggregate to generate method for
     * @param options Additional generation options
     * @returns Generated Java method as string
     */
    generate(aggregate: Aggregate, options: GenerationOptions): string {
        // Step 1: Extract metadata
        const metadata = this.extractMetadata(aggregate, options);

        // Step 2: Build method signature
        const signature = this.buildMethodSignature(metadata);

        // Step 3: Build method body
        const body = this.buildMethodBody(metadata);

        // Step 4: Build event handling (optional - hook method)
        const eventHandling = this.buildEventHandling(metadata);

        // Step 5: Build error handling (hook method with default implementation)
        const errorHandling = this.buildErrorHandling(metadata);

        // Step 6: Assemble final method (hook method with default implementation)
        return this.assembleMethod(signature, body, eventHandling, errorHandling, metadata);
    }

    // ============================================================================
    // ABSTRACT METHODS - Subclasses MUST implement these
    // ============================================================================

    /**
     * Extract metadata from aggregate needed for method generation.
     *
     * This is the first step in the template method.
     * Subclasses determine what metadata they need.
     *
     * @param aggregate The aggregate being processed
     * @param options Generation options
     * @returns Metadata needed for method generation
     */
    protected abstract extractMetadata(aggregate: Aggregate, options: GenerationOptions): MethodMetadata;

    /**
     * Build the method signature (return type, name, parameters).
     *
     * Example: "public UserDto createUser(CreateUserRequestDto request, UnitOfWork unitOfWork)"
     *
     * @param metadata Extracted metadata
     * @returns Method signature as string
     */
    protected abstract buildMethodSignature(metadata: MethodMetadata): string;

    /**
     * Build the method body (main business logic).
     *
     * This is the core logic of the method, excluding error handling and event publishing.
     *
     * @param metadata Extracted metadata
     * @returns Method body as string
     */
    protected abstract buildMethodBody(metadata: MethodMetadata): string;

    // ============================================================================
    // HOOK METHODS - Subclasses CAN override these (optional)
    // ============================================================================

    /**
     * Build event handling code (publishing domain events).
     *
     * Default: returns empty string (no event handling).
     * Override to add event publishing logic.
     *
     * @param metadata Extracted metadata
     * @returns Event handling code as string
     */
    protected buildEventHandling(metadata: MethodMetadata): string {
        return '';
    }

    /**
     * Build error handling code (try-catch blocks).
     *
     * Default: generates standard try-catch with project exception using ExceptionGenerator.
     * Override for custom error handling.
     *
     * @param metadata Extracted metadata
     * @returns Error handling code as string
     */
    protected buildErrorHandling(metadata: MethodMetadata): string {
        // Determine action from method name (e.g., "createUser" → "creating")
        const action = this.extractActionFromMethodName(metadata.methodName);
        const entityName = this.lowercase(metadata.aggregateName);

        return ExceptionGenerator.generateCatchBlock(
            metadata.projectName,
            action,
            entityName
        );
    }

    /**
     * Extract action verb from method name.
     * createUser → creating
     * updateUser → updating
     * deleteUser → deleting
     * getUser → retrieving
     *
     * @param methodName Method name
     * @returns Action description
     */
    private extractActionFromMethodName(methodName: string): string {
        if (methodName.startsWith('create')) return 'creating';
        if (methodName.startsWith('update')) return 'updating';
        if (methodName.startsWith('delete')) return 'deleting';
        if (methodName.startsWith('get') || methodName.startsWith('find')) return 'retrieving';
        if (methodName.startsWith('add')) return 'adding';
        if (methodName.startsWith('remove')) return 'removing';
        return 'processing';
    }

    /**
     * Assemble the complete method from all parts.
     *
     * Default: wraps body in try-catch and adds signature.
     * Override for custom assembly (e.g., Saga compensation).
     *
     * @param signature Method signature
     * @param body Method body
     * @param eventHandling Event handling code
     * @param errorHandling Error handling code
     * @param metadata Extracted metadata
     * @returns Complete method as string
     */
    protected assembleMethod(
        signature: string,
        body: string,
        eventHandling: string,
        errorHandling: string,
        metadata: MethodMetadata
    ): string {
        // Default assembly: signature + try + body + events + catch
        return `    ${signature} {
        try {
${body}${eventHandling}
${errorHandling}
    }`;
    }

    // ============================================================================
    // UTILITY METHODS - Common helpers for subclasses
    // ============================================================================

    /**
     * Build a parameter list string from parameter metadata.
     *
     * Example: "Integer id, UserDto dto, UnitOfWork unitOfWork"
     */
    protected buildParameterList(parameters: MethodParameter[]): string {
        return parameters
            .map(p => {
                const annotations = p.annotations ? p.annotations.join(' ') + ' ' : '';
                return `${annotations}${p.type} ${p.name}`;
            })
            .join(', ');
    }

    /**
     * Build method annotations string.
     *
     * Example: "@Transactional\n    @Override"
     */
    protected buildAnnotations(annotations?: string[]): string {
        if (!annotations || annotations.length === 0) {
            return '';
        }
        return annotations.map(a => `    ${a}`).join('\n') + '\n';
    }

    /**
     * Build a variable declaration with initialization.
     *
     * Example: "User user = userFactory.createUser(dto)"
     */
    protected buildVariableDeclaration(type: string, name: string, initialization: string): string {
        return `            ${type} ${name} = ${initialization};`;
    }

    /**
     * Build a method call statement.
     *
     * Example: "unitOfWorkService.registerChanged(user, unitOfWork);"
     */
    protected buildMethodCall(target: string, method: string, ...args: string[]): string {
        return `            ${target}.${method}(${args.join(', ')});`;
    }

    /**
     * Build a return statement.
     *
     * Example: "return userDto;"
     */
    protected buildReturnStatement(expression: string): string {
        return `            return ${expression};`;
    }
}

/**
 * Simplified generation options for method generators
 */
export interface GenerationOptions extends GeneratorOptions {
    includeEventHandling?: boolean;
    includeErrorHandling?: boolean;
    useImmutablePattern?: boolean;
    [key: string]: any;  // Allow additional options
}
