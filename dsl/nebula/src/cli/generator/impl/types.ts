export interface GenerationOptions {
    projectName: string;
    outputPath: string;
    architecture?: 'default' | 'external-dto-removal' | 'causal-saga';
    features?: string[];
}

export interface CoordinationGenerationOptions extends GenerationOptions { }

export interface EventGenerationOptions extends GenerationOptions { }

export interface EventHandlerGenerationOptions extends GenerationOptions { }

export interface ValidationGenerationOptions extends GenerationOptions { }

export interface WebApiGenerationOptions extends GenerationOptions { }

export interface ServiceGenerationOptions extends GenerationOptions { }

export interface EntityGenerationOptions extends GenerationOptions { }

export interface DtoGenerationOptions extends GenerationOptions { }

export interface RepositoryGenerationOptions extends GenerationOptions { }

export interface FactoryGenerationOptions extends GenerationOptions { }

export interface ExceptionGenerationOptions extends GenerationOptions { }

export interface IntegrationGenerationOptions extends GenerationOptions { }

export interface SagaGenerationOptions extends GenerationOptions { }
