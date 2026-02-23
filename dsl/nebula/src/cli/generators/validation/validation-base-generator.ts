import Handlebars from 'handlebars';
import { Aggregate, Entity } from "../../../language/generated/ast.js";
import { ValidationGenerationOptions, ValidationContext } from "./validation-types.js";
import { UnifiedTypeResolver as TypeResolver } from "../common/unified-type-resolver.js";
import { UnifiedTypeResolver } from "../common/unified-type-resolver.js";

export abstract class ValidationBaseGenerator {
    
    protected capitalize(str: string): string {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1);
    }

    protected generatePackageName(projectName: string, aggregateName: string, subPackage: string, ...additionalSubPackages: string[]): string {
        const basePackage = 'pt.ulisboa.tecnico.socialsoftware';
        const microservicePackage = `microservices.${aggregateName.toLowerCase()}`;
        const subPackages = [subPackage, ...additionalSubPackages].filter(p => p).join('.');
        return `${basePackage}.${projectName.toLowerCase()}.${microservicePackage}.${subPackages}`;
    }

    protected createAggregateNaming(aggregateName: string) {
        return {
            original: aggregateName,
            capitalized: this.capitalize(aggregateName),
            lower: aggregateName.toLowerCase()
        };
    }

    protected combineImports(...importArrays: string[][]): string[] {
        const combined = new Set<string>();
        importArrays.forEach(arr => arr.forEach(imp => combined.add(imp)));
        return Array.from(combined).sort();
    }

    protected buildStandardImports(projectName: string, aggregateName: string): string[] {
        return [];
    }

    protected isCollectionType(type: any): boolean {
        return UnifiedTypeResolver.isCollectionType(type);
    }

    protected isEntityType(type: any): boolean {
        return UnifiedTypeResolver.isEntityType(type);
    }

    protected resolveJavaType(type: any): string {
        return TypeResolver.resolveJavaType(type);
    }

    protected renderTemplate(template: string, context: any): string {
        const compiledTemplate = Handlebars.compile(template, { noEscape: true });
        return compiledTemplate(context);
    }
    protected createValidationContext(aggregate: Aggregate, rootEntity: Entity, options: ValidationGenerationOptions, subPackage: string): ValidationContext {
        const naming = this.createAggregateNaming(aggregate.name);
        const projectName = options?.projectName || 'unknown';
        const packageName = this.generatePackageName(projectName, aggregate.name, `validation.${subPackage}`);

        return {
            aggregateName: naming.original,
            capitalizedAggregate: naming.capitalized,
            lowerAggregate: naming.lower,
            packageName,
            rootEntity,
            projectName,
            imports: this.buildValidationImports(projectName, aggregate.name)
        };
    }

    protected buildValidationImports(projectName: string, aggregateName: string): string[] {
        const baseImports = this.buildStandardImports(projectName, aggregateName);
        const validationImports = [
            'import jakarta.validation.constraints.*;',
            'import jakarta.validation.ConstraintValidator;',
            'import jakarta.validation.ConstraintValidatorContext;',
            'import jakarta.validation.Constraint;',
            'import jakarta.validation.Payload;',
            '',
            'import java.lang.annotation.Documented;',
            'import java.lang.annotation.ElementType;',
            'import java.lang.annotation.Retention;',
            'import java.lang.annotation.RetentionPolicy;',
            'import java.lang.annotation.Target;',
            '',
            'import java.time.LocalDateTime;',
            'import java.time.LocalDate;',
            'import java.util.Collection;',
            'import java.util.regex.Pattern;',
            ''
        ];

        return this.combineImports(baseImports, validationImports);
    }

    protected buildValidationRule(property: any, ruleType: string, aggregateName: string): any {
        const propertyName = property.name;
        const propertyType = TypeResolver.resolveJavaType(property.type);
        const capitalizedProperty = this.capitalize(propertyName);

        return {
            propertyName,
            propertyType,
            capitalizedProperty,
            ruleType,
            aggregateName,
            isRequired: this.isRequiredProperty(property),
            isCollection: this.isCollectionType(property.type),
            isEntity: this.isEntityType(property.type),
            constraints: this.getPropertyConstraints(property, propertyType)
        };
    }

    protected isRequiredProperty(property: any): boolean {
        return property.required !== false && property.nullable !== true;
    }

    protected getPropertyConstraints(property: any, propertyType: string): string[] {
        const constraints: string[] = [];
        const propertyName = property.name;

        if (this.isRequiredProperty(property)) {
            constraints.push('@NotNull');
        }

        if (propertyType === 'String') {
            if (this.isRequiredProperty(property)) {
                constraints.push('@NotBlank');
            }

            if (property.minLength || property.maxLength) {
                const min = property.minLength || 0;
                const max = property.maxLength || 255;
                constraints.push(`@Size(min = ${min}, max = ${max})`);
            }

            if (propertyName.toLowerCase().includes('email')) {
                constraints.push('@Email');
            }
        }

        if (['Integer', 'Long', 'Double', 'Float'].includes(propertyType)) {
            if (property.min !== undefined) {
                constraints.push(`@Min(${property.min})`);
            }
            if (property.max !== undefined) {
                constraints.push(`@Max(${property.max})`);
            }
            if (propertyName.toLowerCase().includes('positive')) {
                constraints.push('@Positive');
            }
        }

        if (this.isCollectionType(property.type)) {
            constraints.push('@NotEmpty');
            if (property.minSize || property.maxSize) {
                const min = property.minSize || 0;
                const max = property.maxSize || 1000;
                constraints.push(`@Size(min = ${min}, max = ${max})`);
            }
        }

        if (['LocalDateTime', 'LocalDate', 'Date'].includes(propertyType)) {
            if (propertyName.toLowerCase().includes('past')) {
                constraints.push('@Past');
            } else if (propertyName.toLowerCase().includes('future')) {
                constraints.push('@Future');
            }
        }

        return constraints;
    }

    protected buildValidationMessage(ruleName: string, propertyName: string, aggregateName: string): string {
        const messageKey = `${aggregateName.toLowerCase()}.${propertyName}.${ruleName.toLowerCase()}`;
        return `{${messageKey}}`;
    }

    protected generateValidationMethodName(property: any, ruleType: string): string {
        const propertyName = this.capitalize(property.name);
        const ruleTypeName = this.capitalize(ruleType);
        return `validate${propertyName}${ruleTypeName}`;
    }

    protected buildConstraintClass(constraintName: string, validatorClass: string, message: string): string {
        return `@Documented
@Constraint(validatedBy = ${validatorClass}.class)
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ${constraintName} {
    String message() default "${message}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}`;
    }

    protected buildValidatorClass(validatorName: string, constraintName: string, validationType: string, validationLogic: string): string {
        return `public class ${validatorName} implements ConstraintValidator<${constraintName}, ${validationType}> {
    
    @Override
    public void initialize(${constraintName} constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(${validationType} value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        ${validationLogic}
    }
}`;
    }

    protected getValidationSeverity(ruleType: string): 'error' | 'warning' | 'info' {
        switch (ruleType.toLowerCase()) {
            case 'invariant':
            case 'constraint':
                return 'error';
            case 'business':
                return 'warning';
            default:
                return 'info';
        }
    }
}
