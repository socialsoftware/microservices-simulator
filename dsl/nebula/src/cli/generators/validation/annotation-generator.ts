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

    private buildValidationAnnotations(rootEntity: Entity, aggregateName: string, _aggregate: Aggregate): any[] {
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

        const projectName = options?.projectName?.toLowerCase() || 'unknown';
        const lowerAggregate = aggregate.name.toLowerCase();
        const basePackage = 'pt.ulisboa.tecnico.socialsoftware';

        // Collect entity names from this aggregate
        const entityNames = new Set<string>();
        const aggregateElements = (aggregate as any).aggregateElements || [];
        for (const elem of aggregateElements) {
            if (elem?.name) {
                entityNames.add(elem.name);
            }
        }

        const standardTypes = new Set([
            'String', 'Integer', 'Long', 'Float', 'Double', 'Boolean',
            'LocalDateTime', 'LocalDate', 'Date', 'BigDecimal',
            'Object', 'Void', 'Byte', 'Short', 'Character'
        ]);

        const typeImports: string[] = [];
        const addedTypes = new Set<string>();

        for (const annotation of annotations) {
            const propType = annotation.propertyType;
            if (!propType) continue;

            // Check for collection types like Set<X> or List<X>
            const collectionMatch = propType.match(/^(Set|List)<(.+)>$/);
            if (collectionMatch) {
                const collectionType = collectionMatch[1];
                const innerType = collectionMatch[2];

                if (!addedTypes.has(collectionType)) {
                    typeImports.push(`import java.util.${collectionType};`);
                    addedTypes.add(collectionType);
                }

                if (!standardTypes.has(innerType) && !addedTypes.has(innerType)) {
                    if (entityNames.has(innerType)) {
                        typeImports.push(`import ${basePackage}.${projectName}.microservices.${lowerAggregate}.aggregate.${innerType};`);
                    } else {
                        typeImports.push(`import ${basePackage}.${projectName}.shared.enums.${innerType};`);
                    }
                    addedTypes.add(innerType);
                }
            } else if (!standardTypes.has(propType) && !addedTypes.has(propType)) {
                if (entityNames.has(propType)) {
                    typeImports.push(`import ${basePackage}.${projectName}.microservices.${lowerAggregate}.aggregate.${propType};`);
                } else {
                    typeImports.push(`import ${basePackage}.${projectName}.shared.enums.${propType};`);
                }
                addedTypes.add(propType);
            }
        }

        return this.combineImports(baseImports, annotationImports, typeImports);
    }

    private getValidationAnnotationsTemplate(): string {
        return `package {{packageName}};

{{#each imports}}
{{this}}
{{/each}}

public class {{capitalizedAggregate}}ValidationAnnotations {

{{#each validationAnnotations}}
    public static class {{capitalizedProperty}}Validation {
        {{{annotationsString}}}
        private {{{propertyType}}} {{propertyName}};
        
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
