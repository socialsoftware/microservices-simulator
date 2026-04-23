import { Aggregate } from "../../../language/generated/ast.js";
import { GeneratorCapabilities } from "../common/generator-capabilities.js";



export interface GenerationOptions {
    projectName: string;
    architecture?: string;
    outputDirectory?: string;
    transactionModel?: 'sagas' | 'tcc';
}



export interface GenerationResult {
    files: GeneratedFile[];
    warnings: string[];
    errors: string[];
    metadata?: GenerationMetadata;
}



export interface GeneratedFile {
    path: string;
    content: string;
    description: string;
    category?: 'entity' | 'service' | 'repository' | 'dto' | 'event' | 'controller' | 'saga' | 'config';
}



export interface GenerationMetadata {
    aggregateName: string;
    generatorName: string;
    timestamp: string;
    duration?: number;
    linesGenerated?: number;
}



export interface GeneratorMetadata {
    name: string;
    category: 'microservices' | 'coordination' | 'validation' | 'sagas' | 'config';
    dependencies: string[];
    version: string;
    description?: string;
}



export interface Generator {
    

    generate(
        aggregate: Aggregate,
        options: GenerationOptions
    ): Promise<GenerationResult>;

    

    canGenerate(aggregate: Aggregate): boolean;

    

    getMetadata(): GeneratorMetadata;
}



export abstract class BaseGenerator implements Generator {
    constructor(protected capabilities?: GeneratorCapabilities) {}

    

    abstract generate(
        aggregate: Aggregate,
        options: GenerationOptions
    ): Promise<GenerationResult>;

    

    canGenerate(aggregate: Aggregate): boolean {
        return true;
    }

    

    abstract getMetadata(): GeneratorMetadata;

    

    protected createResult(files: GeneratedFile[], warnings: string[] = [], errors: string[] = []): GenerationResult {
        return {
            files,
            warnings,
            errors,
            metadata: {
                aggregateName: '',
                generatorName: this.getMetadata().name,
                timestamp: new Date().toISOString(),
                linesGenerated: files.reduce((sum, f) => sum + f.content.split('\n').length, 0)
            }
        };
    }

    

    protected createErrorResult(error: string): GenerationResult {
        return {
            files: [],
            warnings: [],
            errors: [error]
        };
    }

    

    protected createFile(
        path: string,
        content: string,
        description: string,
        category?: GeneratedFile['category']
    ): GeneratedFile {
        return {
            path,
            content,
            description,
            category
        };
    }
}
