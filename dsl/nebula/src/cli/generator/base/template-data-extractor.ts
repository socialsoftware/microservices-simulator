import { Aggregate, Model } from "../../../language/generated/ast.js";
import { GeneratorContext, TemplateData, DataExtractionResult, ExtractionOptions } from "./template-data-types.js";
import { TemplateDataBase } from "./template-data-base.js";
import { ValidationDataExtractor } from "./validation-data-extractor.js";
import { MethodWorkflowExtractor } from "./method-workflow-extractor.js";
import { ModelParser } from "./model-parser.js";

export { GeneratorContext, TemplateData } from "./template-data-types.js";

export class TemplateDataExtractor extends TemplateDataBase {
    private validationExtractor = new ValidationDataExtractor();
    private methodWorkflowExtractor = new MethodWorkflowExtractor();
    private modelParser = new ModelParser();

    extractData(aggregate: Aggregate, context: GeneratorContext, options: ExtractionOptions = this.getDefaultOptions()): TemplateData {
        try {
            const aggregateData = this.modelParser.parseAggregate(aggregate);

            const entities = aggregate.entities || [];
            const entitiesData = entities.map(entity => this.modelParser.parseEntity(entity));

            const templateData: TemplateData = {
                project: this.buildProjectData(context),
                aggregate: aggregateData,
                entities: entitiesData,
                methods: options.includeWorkflows ? this.methodWorkflowExtractor.extractMethodsData(aggregate) : [],
                workflows: options.includeWorkflows ? this.methodWorkflowExtractor.extractWorkflowsData(aggregate) : [],
                relationships: { oneToMany: [], manyToOne: [], manyToMany: [], allRelationships: [] },
                validations: options.includeValidations ? this.validationExtractor.extractValidationData(aggregate) : [],
                configuration: this.buildConfigurationData(context),
                metadata: options.includeMetadata ? this.buildMetadataData(aggregate, context) : this.buildMinimalMetadata(),
                exceptions: undefined
            };

            return templateData;
        } catch (error) {
            throw new Error(`Failed to extract template data: ${error instanceof Error ? error.message : 'Unknown error'}`);
        }
    }

    extractDataFromModel(model: Model, context: GeneratorContext, options: ExtractionOptions = this.getDefaultOptions()): TemplateData[] {
        const results: TemplateData[] = [];
        const aggregates = model.aggregates || [];

        aggregates.forEach(aggregate => {
            try {
                const templateData = this.extractData(aggregate, context, options);
                results.push(templateData);
            } catch (error) {
                console.warn(`Failed to extract data for aggregate ${aggregate.name}: ${error instanceof Error ? error.message : 'Unknown error'}`);
            }
        });

        return results;
    }

    extractDataSafely(aggregate: Aggregate, context: GeneratorContext, options: ExtractionOptions = this.getDefaultOptions()): DataExtractionResult {
        const result: DataExtractionResult = {
            success: false,
            errors: [],
            warnings: []
        };

        try {
            this.validateInputs(aggregate, context);

            const templateData = this.extractData(aggregate, context, options);

            const validationResult = this.validateExtractedData(templateData);

            result.success = validationResult.isValid;
            result.data = templateData;
            result.warnings = validationResult.warnings;

            if (!validationResult.isValid) {
                result.errors = validationResult.errors;
            }

        } catch (error) {
            result.success = false;
            result.errors.push(error instanceof Error ? error.message : 'Unknown extraction error');
        }

        return result;
    }

    getEntityTemplateData(aggregate: Aggregate, entityName: string, context: GeneratorContext): TemplateData | null {
        const entity = aggregate.entities?.find(e => e.name === entityName);
        if (!entity) {
            return null;
        }

        const entityAggregate: Aggregate = {
            ...aggregate,
            entities: [entity]
        };

        return this.extractData(entityAggregate, context);
    }

    getFeatureTemplateData(aggregate: Aggregate, feature: string, context: GeneratorContext): TemplateData | null {
        if (!this.hasFeature(context.features, feature)) {
            return null;
        }

        const featureContext: GeneratorContext = {
            ...context,
            features: [feature]
        };

        const options = this.getFeatureExtractionOptions(feature);
        return this.extractData(aggregate, featureContext, options);
    }

    getMultiEntityTemplateData(aggregate: Aggregate, entityNames: string[], context: GeneratorContext): TemplateData | null {
        const entities = aggregate.entities?.filter(e => entityNames.includes(e.name));
        if (!entities || entities.length === 0) {
            return null;
        }

        const filteredAggregate: Aggregate = {
            ...aggregate,
            entities
        };

        return this.extractData(filteredAggregate, context);
    }

    extractMinimalData(aggregate: Aggregate, context: GeneratorContext): TemplateData {
        const minimalOptions: ExtractionOptions = {
            includeValidations: false,
            includeMetadata: false,
            includeExceptions: false,
            includeRelationships: false,
            includeWorkflows: false
        };

        return this.extractData(aggregate, context, minimalOptions);
    }

    extractComprehensiveData(aggregate: Aggregate, context: GeneratorContext): TemplateData {
        const comprehensiveOptions: ExtractionOptions = {
            includeValidations: true,
            includeMetadata: true,
            includeExceptions: true,
            includeRelationships: true,
            includeWorkflows: true
        };

        return this.extractData(aggregate, context, comprehensiveOptions);
    }


    private getDefaultOptions(): ExtractionOptions {
        return {
            includeValidations: true,
            includeMetadata: true,
            includeExceptions: true,
            includeRelationships: true,
            includeWorkflows: true
        };
    }

    private getFeatureExtractionOptions(feature: string): ExtractionOptions {
        const options = this.getDefaultOptions();

        switch (feature) {
            case 'validation':
                options.includeValidations = true;
                options.includeWorkflows = false;
                break;
            case 'webapi':
                options.includeValidations = true;
                options.includeWorkflows = false;
                options.includeExceptions = true;
                break;
            case 'saga':
            case 'coordination':
                options.includeWorkflows = true;
                options.includeRelationships = true;
                break;
            case 'events':
                options.includeWorkflows = true;
                options.includeExceptions = true;
                break;
            default:
                break;
        }

        return options;
    }

    private validateInputs(aggregate: Aggregate, context: GeneratorContext): void {
        if (!aggregate) {
            throw new Error('Aggregate is required');
        }

        if (!aggregate.name) {
            throw new Error('Aggregate name is required');
        }

        if (!context) {
            throw new Error('Generator context is required');
        }

        if (!context.projectName) {
            throw new Error('Project name is required in context');
        }

        if (!context.packageName) {
            throw new Error('Package name is required in context');
        }
    }

    private validateExtractedData(templateData: TemplateData): { isValid: boolean; errors: string[]; warnings: string[] } {
        const errors: string[] = [];
        const warnings: string[] = [];

        if (!templateData.project.name) {
            errors.push('Project name is missing');
        }

        if (!templateData.aggregate.name) {
            errors.push('Aggregate name is missing');
        }

        if (templateData.entities.length === 0) {
            warnings.push('No entities found in aggregate');
        }

        templateData.entities.forEach((entity, index) => {
            if (!entity.name) {
                errors.push(`Entity at index ${index} is missing name`);
            }
        });

        templateData.methods.forEach((method, index) => {
            if (!method.name) {
                errors.push(`Method at index ${index} is missing name`);
            }
            if (!method.returnType) {
                warnings.push(`Method ${method.name} is missing return type`);
            }
        });

        return {
            isValid: errors.length === 0,
            errors,
            warnings
        };
    }

    private buildMinimalMetadata(): any {
        return {
            generatedAt: new Date().toISOString(),
            version: '1.0.0',
            generator: 'Nebula DSL Generator'
        };
    }
}
