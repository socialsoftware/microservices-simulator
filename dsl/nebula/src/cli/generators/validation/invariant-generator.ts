import { Aggregate, Entity } from "../../../language/generated/ast.js";
import { ValidationBaseGenerator } from "./validation-base-generator.js";
import { ValidationGenerationOptions, InvariantContext } from "./validation-types.js";

export class InvariantGenerator extends ValidationBaseGenerator {
    async generateInvariants(aggregate: Aggregate, rootEntity: Entity, options: ValidationGenerationOptions): Promise<string> {
        const context = this.buildInvariantsContext(aggregate, rootEntity, options);
        const template = this.getInvariantsTemplate();
        return this.renderTemplate(template, context);
    }

    private buildInvariantsContext(aggregate: Aggregate, rootEntity: Entity, options: ValidationGenerationOptions): InvariantContext {
        const baseContext = this.createValidationContext(aggregate, rootEntity, options, 'invariants');
        const invariantMethods = this.buildInvariantMethods(rootEntity, baseContext.aggregateName);
        const imports = this.buildInvariantsImports(aggregate, options, invariantMethods);

        return {
            ...baseContext,
            rootEntityType: rootEntity.name,
            invariants: invariantMethods,
            invariantMethods,
            imports
        };
    }

    private buildInvariantMethods(rootEntity: Entity, aggregateName: string): any[] {
        if (!rootEntity.properties) return [];

        const methods: any[] = [];

        rootEntity.properties.forEach(property => {
            if (!property?.name) {
                return;
            }
            if (!property?.type) {
                return;
            }
            const propertyMethods = this.generatePropertyInvariants(property, aggregateName);
            methods.push(...propertyMethods);
        });

        methods.push(this.generateAggregateInvariant(rootEntity, aggregateName));

        return methods;
    }

    private generatePropertyInvariants(property: any, aggregateName: string): any[] {
        const methods: any[] = [];
        const propertyName = property.name;
        const propertyType = this.resolveJavaType(property.type);
        const capitalizedProperty = this.capitalize(propertyName);

        if (this.isRequiredProperty(property)) {
            methods.push({
                name: `invariant${capitalizedProperty}NotNull`,
                property: propertyName,
                propertyType,
                condition: `this.${propertyName} != null`,
                message: `${capitalizedProperty} cannot be null`,
                logic: this.buildNotNullInvariantLogic(propertyName, capitalizedProperty, property),
                severity: 'error'
            });
        }

        if (propertyType === 'String') {
            methods.push(...this.generateStringInvariants(property, aggregateName));
        }

        if (['Integer', 'Long', 'Double', 'Float'].includes(propertyType)) {
            methods.push(...this.generateNumericInvariants(property, aggregateName));
        }

        if (this.isCollectionType(property.type)) {
            methods.push(...this.generateCollectionInvariants(property, aggregateName));
        }

        if (['LocalDateTime', 'LocalDate', 'Date'].includes(propertyType)) {
            methods.push(...this.generateTemporalInvariants(property, aggregateName));
        }

        return methods;
    }

    private generateStringInvariants(property: any, aggregateName: string): any[] {
        const methods: any[] = [];
        const propertyName = property.name;
        const capitalizedProperty = this.capitalize(propertyName);

        if (this.isRequiredProperty(property)) {
            methods.push({
                name: `invariant${capitalizedProperty}NotBlank`,
                property: propertyName,
                propertyType: 'String',
                condition: `this.${propertyName} != null && !this.${propertyName}.trim().isEmpty()`,
                message: `${capitalizedProperty} cannot be blank`,
                logic: this.buildNotBlankInvariantLogic(propertyName, capitalizedProperty),
                severity: 'error'
            });
        }

        if (property.minLength || property.maxLength) {
            const min = property.minLength || 0;
            const max = property.maxLength || 255;
            methods.push({
                name: `invariant${capitalizedProperty}Length`,
                property: propertyName,
                propertyType: 'String',
                condition: `this.${propertyName} != null && this.${propertyName}.length() >= ${min} && this.${propertyName}.length() <= ${max}`,
                message: `${capitalizedProperty} length must be between ${min} and ${max} characters`,
                logic: this.buildLengthInvariantLogic(propertyName, capitalizedProperty, min, max),
                severity: 'error'
            });
        }

        if (propertyName.toLowerCase().includes('email')) {
            methods.push({
                name: `invariant${capitalizedProperty}EmailFormat`,
                property: propertyName,
                propertyType: 'String',
                condition: `this.${propertyName} == null || this.${propertyName}.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\\\.[A-Za-z]{2,})$")`,
                message: `${capitalizedProperty} must be a valid email format`,
                logic: this.buildEmailInvariantLogic(propertyName, capitalizedProperty),
                severity: 'error'
            });
        }

        return methods;
    }

    private generateNumericInvariants(property: any, aggregateName: string): any[] {
        const methods: any[] = [];
        const propertyName = property.name;
        const capitalizedProperty = this.capitalize(propertyName);
        const propertyType = this.resolveJavaType(property.type);

        if (property.min !== undefined || property.max !== undefined) {
            const min = property.min ?? Number.MIN_SAFE_INTEGER;
            const max = property.max ?? Number.MAX_SAFE_INTEGER;
            methods.push({
                name: `invariant${capitalizedProperty}Range`,
                property: propertyName,
                propertyType,
                condition: `this.${propertyName} != null && this.${propertyName} >= ${min} && this.${propertyName} <= ${max}`,
                message: `${capitalizedProperty} must be between ${min} and ${max}`,
                logic: this.buildRangeInvariantLogic(propertyName, capitalizedProperty, min, max),
                severity: 'error'
            });
        }

        if (propertyName.toLowerCase().includes('positive') || property.positive) {
            methods.push({
                name: `invariant${capitalizedProperty}Positive`,
                property: propertyName,
                propertyType,
                condition: `this.${propertyName} == null || this.${propertyName} > 0`,
                message: `${capitalizedProperty} must be positive`,
                logic: this.buildPositiveInvariantLogic(propertyName, capitalizedProperty),
                severity: 'error'
            });
        }

        return methods;
    }

    private generateCollectionInvariants(property: any, aggregateName: string): any[] {
        const methods: any[] = [];
        const propertyName = property.name;
        const capitalizedProperty = this.capitalize(propertyName);

        if (this.isRequiredProperty(property)) {
            methods.push({
                name: `invariant${capitalizedProperty}NotEmpty`,
                property: propertyName,
                propertyType: 'Collection',
                condition: `this.${propertyName} != null && !this.${propertyName}.isEmpty()`,
                message: `${capitalizedProperty} cannot be empty`,
                logic: this.buildCollectionNotEmptyInvariantLogic(propertyName, capitalizedProperty, property),
                severity: 'error'
            });
        }

        if (property.minSize || property.maxSize) {
            const min = property.minSize || 0;
            const max = property.maxSize || 1000;
            methods.push({
                name: `invariant${capitalizedProperty}Size`,
                property: propertyName,
                propertyType: 'Collection',
                condition: `this.${propertyName} == null || (this.${propertyName}.size() >= ${min} && this.${propertyName}.size() <= ${max})`,
                message: `${capitalizedProperty} size must be between ${min} and ${max}`,
                logic: this.buildCollectionSizeInvariantLogic(propertyName, capitalizedProperty, min, max),
                severity: 'error'
            });
        }

        return methods;
    }

    private generateTemporalInvariants(property: any, aggregateName: string): any[] {
        const methods: any[] = [];
        const propertyName = property.name;
        const capitalizedProperty = this.capitalize(propertyName);
        const propertyType = this.resolveJavaType(property.type);

        if (propertyName.toLowerCase().includes('past') || propertyName.toLowerCase().includes('created')) {
            methods.push({
                name: `invariant${capitalizedProperty}Past`,
                property: propertyName,
                propertyType,
                condition: `this.${propertyName} == null || this.${propertyName}.isBefore(LocalDateTime.now())`,
                message: `${capitalizedProperty} must be in the past`,
                logic: this.buildPastInvariantLogic(propertyName, capitalizedProperty),
                severity: 'error'
            });
        }

        if (propertyName.toLowerCase().includes('future') || propertyName.toLowerCase().includes('expires')) {
            methods.push({
                name: `invariant${capitalizedProperty}Future`,
                property: propertyName,
                propertyType,
                condition: `this.${propertyName} == null || this.${propertyName}.isAfter(LocalDateTime.now())`,
                message: `${capitalizedProperty} must be in the future`,
                logic: this.buildFutureInvariantLogic(propertyName, capitalizedProperty),
                severity: 'error'
            });
        }

        return methods;
    }

    private generateAggregateInvariant(rootEntity: Entity, aggregateName: string): any {
        return {
            name: `invariant${aggregateName}Valid`,
            property: 'aggregate',
            propertyType: aggregateName,
            condition: 'true', // Aggregate-level validation
            message: `${aggregateName} aggregate must be in a valid state`,
            logic: this.buildAggregateInvariantLogic(rootEntity, aggregateName),
            severity: 'error'
        };
    }

    private buildNotNullInvariantLogic(propertyName: string, capitalizedProperty: string, property?: any): string {
        const getter = this.getGetterMethodName(propertyName, capitalizedProperty, property);
        return `if (entity.${getter}() == null) {
            throw new IllegalStateException("${capitalizedProperty} cannot be null");
        }`;
    }

    private buildNotBlankInvariantLogic(propertyName: string, capitalizedProperty: string, property?: any): string {
        const getter = this.getGetterMethodName(propertyName, capitalizedProperty, property);
        return `if (entity.${getter}() == null || entity.${getter}().trim().isEmpty()) {
            throw new IllegalStateException("${capitalizedProperty} cannot be blank");
        }`;
    }

    private buildLengthInvariantLogic(propertyName: string, capitalizedProperty: string, min: number, max: number): string {
        return `if (entity.get${capitalizedProperty}() != null && (entity.get${capitalizedProperty}().length() < ${min} || entity.get${capitalizedProperty}().length() > ${max})) {
            throw new IllegalStateException("${capitalizedProperty} length must be between ${min} and ${max} characters");
        }`;
    }

    private buildEmailInvariantLogic(propertyName: string, capitalizedProperty: string): string {
        return `if (entity.get${capitalizedProperty}() != null && !entity.get${capitalizedProperty}().matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\\\.[A-Za-z]{2,})$")) {
            throw new IllegalStateException("${capitalizedProperty} must be a valid email format");
        }`;
    }

    private buildRangeInvariantLogic(propertyName: string, capitalizedProperty: string, min: number, max: number): string {
        return `if (entity.get${capitalizedProperty}() != null && (entity.get${capitalizedProperty}() < ${min} || entity.get${capitalizedProperty}() > ${max})) {
            throw new IllegalStateException("${capitalizedProperty} must be between ${min} and ${max}");
        }`;
    }

    private buildPositiveInvariantLogic(propertyName: string, capitalizedProperty: string): string {
        return `if (entity.get${capitalizedProperty}() != null && entity.get${capitalizedProperty}() <= 0) {
            throw new IllegalStateException("${capitalizedProperty} must be positive");
        }`;
    }

    private buildCollectionNotEmptyInvariantLogic(propertyName: string, capitalizedProperty: string, property?: any): string {
        const getter = this.getGetterMethodName(propertyName, capitalizedProperty, property);
        return `if (entity.${getter}() == null || ((java.util.Collection) entity.${getter}()).isEmpty()) {
            throw new IllegalStateException("${capitalizedProperty} cannot be empty");
        }`;
    }

    private buildCollectionSizeInvariantLogic(propertyName: string, capitalizedProperty: string, min: number, max: number): string {
        return `if (entity.get${capitalizedProperty}() != null && (entity.get${capitalizedProperty}().size() < ${min} || entity.get${capitalizedProperty}().size() > ${max})) {
            throw new IllegalStateException("${capitalizedProperty} size must be between ${min} and ${max}");
        }`;
    }

    private buildPastInvariantLogic(propertyName: string, capitalizedProperty: string): string {
        return `if (entity.get${capitalizedProperty}() != null && !entity.get${capitalizedProperty}().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("${capitalizedProperty} must be in the past");
        }`;
    }

    private buildFutureInvariantLogic(propertyName: string, capitalizedProperty: string): string {
        return `if (entity.get${capitalizedProperty}() != null && !entity.get${capitalizedProperty}().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("${capitalizedProperty} must be in the future");
        }`;
    }

    private buildAggregateInvariantLogic(rootEntity: Entity, aggregateName: string): string {
        return `// Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // TODO: Implement aggregate-specific business rules`;
    }

    private buildInvariantsImports(aggregate: Aggregate, options: ValidationGenerationOptions, invariants: any[]): string[] {
        const baseImports = this.buildValidationImports(options.projectName, aggregate.name);
        const invariantImports = [
            'import java.time.LocalDateTime;',
            'import java.util.Collection;',
            'import java.util.regex.Pattern;'
        ];

        return this.combineImports(baseImports, invariantImports);
    }

    private getInvariantsTemplate(): string {
        return `package {{packageName}};

{{#each imports}}
{{this}}
{{/each}}

/**
 * Invariant validation methods for {{capitalizedAggregate}}
 */
public class {{capitalizedAggregate}}Invariants {

{{#each invariantMethods}}
    /**
     * {{{message}}}
     */
    public static void {{name}}({{../rootEntityType}} entity) {
        {{{logic}}}
    }

{{/each}}
}`;
    }

    private getGetterMethodName(propertyName: string, capitalizedProperty: string, property?: any): string {
        if (this.isBooleanProperty(property) || this.isBooleanPropertyName(propertyName)) {
            return `is${capitalizedProperty}`;
        }
        return `get${capitalizedProperty}`;
    }

    private isBooleanProperty(property: any): boolean {
        if (!property || !property.type) return false;

        const type = property.type;

        if (typeof type === 'string') {
            return type.toLowerCase() === 'boolean';
        }

        if (type.$type === 'PrimitiveType') {
            return type.name?.toLowerCase() === 'boolean';
        }

        return false;
    }

    private isBooleanPropertyName(propertyName: string): boolean {
        const booleanNames = ['completed', 'active', 'enabled', 'visible', 'valid', 'required', 'available'];
        return booleanNames.includes(propertyName.toLowerCase());
    }
}
