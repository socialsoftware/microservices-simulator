


import { Aggregate } from "../../../../language/generated/ast.js";
import { GeneratorBase, GeneratorOptions } from "./generator-base.js";
import { ExceptionGenerator } from "../utils/exception-generator.js";



export interface MethodMetadata {
    methodName: string;
    aggregateName: string;
    entityName: string;
    projectName: string;
    parameters: MethodParameter[];
    returnType: string;
    throwsExceptions?: string[];
    annotations?: string[];
    [key: string]: any;  
}



export interface MethodParameter {
    name: string;
    type: string;
    annotations?: string[];
}



export abstract class MethodGeneratorTemplate extends GeneratorBase {

    constructor(options?: GeneratorOptions) {
        super(options);
    }

    

    generate(aggregate: Aggregate, options: GenerationOptions): string {
        
        const metadata = this.extractMetadata(aggregate, options);

        
        const signature = this.buildMethodSignature(metadata);

        
        const body = this.buildMethodBody(metadata);

        
        const eventHandling = this.buildEventHandling(metadata);

        
        const errorHandling = this.buildErrorHandling(metadata);

        
        return this.assembleMethod(signature, body, eventHandling, errorHandling, metadata);
    }

    
    
    

    

    protected abstract extractMetadata(aggregate: Aggregate, options: GenerationOptions): MethodMetadata;

    

    protected abstract buildMethodSignature(metadata: MethodMetadata): string;

    

    protected abstract buildMethodBody(metadata: MethodMetadata): string;

    
    
    

    

    protected buildEventHandling(metadata: MethodMetadata): string {
        return '';
    }

    

    protected buildErrorHandling(metadata: MethodMetadata): string {
        
        const action = this.extractActionFromMethodName(metadata.methodName);
        const entityName = this.lowercase(metadata.aggregateName);

        return ExceptionGenerator.generateCatchBlock(
            metadata.projectName,
            action,
            entityName
        );
    }

    

    private extractActionFromMethodName(methodName: string): string {
        if (methodName.startsWith('create')) return 'creating';
        if (methodName.startsWith('update')) return 'updating';
        if (methodName.startsWith('delete')) return 'deleting';
        if (methodName.startsWith('get') || methodName.startsWith('find')) return 'retrieving';
        if (methodName.startsWith('add')) return 'adding';
        if (methodName.startsWith('remove')) return 'removing';
        return 'processing';
    }

    

    protected assembleMethod(
        signature: string,
        body: string,
        eventHandling: string,
        errorHandling: string,
        metadata: MethodMetadata
    ): string {
        
        return `    ${signature} {
        try {
${body}${eventHandling}
${errorHandling}
    }`;
    }

    
    
    

    

    protected buildParameterList(parameters: MethodParameter[]): string {
        return parameters
            .map(p => {
                const annotations = p.annotations ? p.annotations.join(' ') + ' ' : '';
                return `${annotations}${p.type} ${p.name}`;
            })
            .join(', ');
    }

    

    protected buildAnnotations(annotations?: string[]): string {
        if (!annotations || annotations.length === 0) {
            return '';
        }
        return annotations.map(a => `    ${a}`).join('\n') + '\n';
    }

    

    protected buildVariableDeclaration(type: string, name: string, initialization: string): string {
        return `            ${type} ${name} = ${initialization};`;
    }

    

    protected buildMethodCall(target: string, method: string, ...args: string[]): string {
        return `            ${target}.${method}(${args.join(', ')});`;
    }

    

    protected buildReturnStatement(expression: string): string {
        return `            return ${expression};`;
    }
}



export interface GenerationOptions extends GeneratorOptions {
    includeEventHandling?: boolean;
    includeErrorHandling?: boolean;
    useImmutablePattern?: boolean;
    [key: string]: any;  
}
