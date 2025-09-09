import { Aggregate, Entity } from "../../../language/generated/ast.js";
import { ValidationBaseGenerator } from "./validation-base-generator.js";
import { ValidationGenerationOptions, AnnotationContext } from "./validation-types.js";

export class AnnotationGenerator extends ValidationBaseGenerator {
    async generateValidationAnnotations(aggregate: Aggregate, rootEntity: Entity, options: ValidationGenerationOptions): Promise<string> {
        const context = this.buildValidationAnnotationsContext(aggregate, rootEntity, options);
        const template = this.getValidationAnnotationsTemplate();
        return this.renderTemplate(template, context);
    }

    private buildValidationAnnotationsContext(aggregate: Aggregate, rootEntity: Entity, options: ValidationGenerationOptions): AnnotationContext {
        const baseContext = this.createValidationContext(aggregate, rootEntity, options, 'annotations');
        const validationAnnotations = this.buildValidationAnnotations(rootEntity, baseContext.aggregateName, aggregate);
        const imports = this.buildValidationAnnotationsImports(aggregate, options, validationAnnotations);

        return {
            ...baseContext,
            annotations: validationAnnotations,
            validationAnnotations,
            imports
        };
    }

    private buildValidationAnnotations(rootEntity: Entity, aggregateName: string, aggregate: Aggregate): any[] {
        if (!rootEntity.properties) return [];

        return rootEntity.properties.map(property => {
            const rule = this.buildValidationRule(property, 'annotation', aggregateName);
            const constraints = this.getPropertyConstraints(property, rule.propertyType);

            return {
                ...rule,
                annotations: constraints,
                annotationsString: constraints.join('\n    '),
                validationLogic: this.buildAnnotationValidationLogic(property, constraints)
            };
        });
    }

    private buildAnnotationValidationLogic(property: any, constraints: string[]): string {
        const propertyName = property.name;
        const capitalizedProperty = this.capitalize(propertyName);

        let logic = `// Validation logic for ${capitalizedProperty}\n`;

        constraints.forEach(constraint => {
            if (constraint.includes('@NotNull')) {
                logic += `        // @NotNull validation\n`;
            } else if (constraint.includes('@NotBlank')) {
                logic += `        // @NotBlank validation\n`;
            } else if (constraint.includes('@Size')) {
                logic += `        // @Size validation\n`;
            } else if (constraint.includes('@Email')) {
                logic += `        // @Email validation\n`;
            } else if (constraint.includes('@Min') || constraint.includes('@Max')) {
                logic += `        // Range validation\n`;
            }
        });

        return logic;
    }

    private buildValidationAnnotationsImports(aggregate: Aggregate, options: ValidationGenerationOptions, annotations: any[]): string[] {
        const baseImports = this.buildValidationImports(options.projectName, aggregate.name);
        const annotationImports = [
            'import jakarta.validation.constraints.*;',
            'import jakarta.validation.Valid;'
        ];

        return this.combineImports(baseImports, annotationImports);
    }

    private getValidationAnnotationsTemplate(): string {
        return `package {{packageName}};

{{#each imports}}
{{this}}
{{/each}}

/**
 * Validation annotations for {{capitalizedAggregate}} properties
 */
public class {{capitalizedAggregate}}ValidationAnnotations {

{{#each validationAnnotations}}
    /**
     * Validation annotations for {{{propertyName}}}
     */
    public static class {{capitalizedProperty}}Validation {
        {{{annotationsString}}}
        private {{{propertyType}}} {{propertyName}};
        
        // Getter and setter
        public {{{propertyType}}} get{{capitalizedProperty}}() {
            return {{propertyName}};
        }
        
        public void set{{capitalizedProperty}}({{{propertyType}}} {{propertyName}}) {
            this.{{propertyName}} = {{propertyName}};
        }
    }

{{/each}}
}`;
    }
}
