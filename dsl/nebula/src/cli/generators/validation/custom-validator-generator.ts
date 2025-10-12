import { Aggregate, Entity } from "../../../language/generated/ast.js";
import { ValidationBaseGenerator } from "./validation-base-generator.js";
import { ValidationGenerationOptions, ValidatorContext } from "./validation-types.js";

export class CustomValidatorGenerator extends ValidationBaseGenerator {
    async generateCustomValidators(aggregate: Aggregate, rootEntity: Entity, options: ValidationGenerationOptions): Promise<{ [key: string]: string }> {
        const context = this.buildCustomValidatorsContext(aggregate, rootEntity, options);
        const results: { [key: string]: string } = {};

        for (const validator of context.customValidators) {
            const constraintTemplate = this.getConstraintTemplate();
            const validatorTemplate = this.getValidatorTemplate();

            const constraintContext = { ...context, validator };
            results[`${validator.constraintName}.java`] = this.renderTemplate(constraintTemplate, constraintContext);

            const validatorContext = { ...context, validator };
            results[`${validator.name}.java`] = this.renderTemplate(validatorTemplate, validatorContext);
        }

        return results;
    }

    private buildCustomValidatorsContext(aggregate: Aggregate, rootEntity: Entity, options: ValidationGenerationOptions): ValidatorContext {
        const baseContext = this.createValidationContext(aggregate, rootEntity, options, 'validators');
        const customValidators = this.buildCustomValidators(rootEntity, baseContext.aggregateName, aggregate);
        const imports = this.buildCustomValidatorsImports(aggregate, options, customValidators);

        return {
            ...baseContext,
            validators: customValidators,
            customValidators,
            imports
        };
    }

    private buildCustomValidators(rootEntity: Entity, aggregateName: string, aggregate: Aggregate): any[] {
        const validators: any[] = [];

        validators.push(...this.generateBusinessRuleValidators(rootEntity, aggregateName));

        validators.push(...this.generateDomainValidators(rootEntity, aggregateName));

        validators.push(...this.generateCrossPropertyValidators(rootEntity, aggregateName));

        return validators;
    }

    private generateBusinessRuleValidators(rootEntity: Entity, aggregateName: string): any[] {
        const validators: any[] = [];

        validators.push({
            name: `${aggregateName}BusinessRuleValidator`,
            constraintName: `Valid${aggregateName}BusinessRule`,
            validationType: aggregateName,
            message: `${aggregateName} must comply with business rules`,
            validationLogic: this.buildBusinessRuleValidationLogic(aggregateName),
            constraintClass: this.buildConstraintClass(`Valid${aggregateName}BusinessRule`, `${aggregateName}BusinessRuleValidator`, `${aggregateName} must comply with business rules`),
            validatorClass: this.buildValidatorClass(`${aggregateName}BusinessRuleValidator`, `Valid${aggregateName}BusinessRule`, aggregateName, this.buildBusinessRuleValidationLogic(aggregateName))
        });

        return validators;
    }

    private generateDomainValidators(rootEntity: Entity, aggregateName: string): any[] {
        const validators: any[] = [];

        if (!rootEntity.properties) return validators;

        rootEntity.properties.forEach((property, index) => {
            const propertyName = property?.name;
            if (!propertyName) {
                console.warn(`Skipping property at index ${index} in ${aggregateName}: missing name`);
                return;
            }

            if (!property?.type) {
                console.warn(`Skipping property '${propertyName}' in ${aggregateName}: missing type`);
                return;
            }

            const capitalizedProperty = this.capitalize(propertyName);
            const propertyType = this.resolveJavaType(property.type);

            if (propertyName.toLowerCase().includes('email')) {
                validators.push(this.createEmailDomainValidator(capitalizedProperty, propertyType));
            }

            if (propertyName.toLowerCase().includes('phone')) {
                validators.push(this.createPhoneValidator(capitalizedProperty, propertyType));
            }

            if (propertyName.toLowerCase().includes('url') || propertyName.toLowerCase().includes('website')) {
                validators.push(this.createUrlValidator(capitalizedProperty, propertyType));
            }

            if (propertyType === 'String' && (propertyName.toLowerCase().includes('code') || propertyName.toLowerCase().includes('id'))) {
                validators.push(this.createCodeFormatValidator(capitalizedProperty, propertyType, propertyName));
            }
        });

        return validators;
    }

    private generateCrossPropertyValidators(rootEntity: Entity, aggregateName: string): any[] {
        const validators: any[] = [];

        if (!rootEntity.properties) return validators;

        const dateProperties = rootEntity.properties.filter(prop => {
            if (!prop?.type) {
                console.warn(`Filtering out property '${prop?.name || 'unknown'}' in ${aggregateName}: missing type`);
                return false;
            }
            try {
                return ['LocalDateTime', 'LocalDate', 'Date'].includes(this.resolveJavaType(prop.type));
            } catch (error) {
                console.warn(`Error resolving type for property '${prop?.name || 'unknown'}' in ${aggregateName}:`, error);
                return false;
            }
        });

        for (let i = 0; i < dateProperties.length; i++) {
            for (let j = i + 1; j < dateProperties.length; j++) {
                const startProp = dateProperties[i];
                const endProp = dateProperties[j];

                if (this.isDateRangePair(startProp.name, endProp.name)) {
                    validators.push(this.createDateRangeValidator(startProp, endProp, aggregateName));
                }
            }
        }

        return validators;
    }

    private createEmailDomainValidator(propertyName: string, propertyType: string): any {
        return {
            name: `${propertyName}DomainValidator`,
            constraintName: `Valid${propertyName}Domain`,
            validationType: propertyType,
            message: `${propertyName} must be from an allowed domain`,
            validationLogic: this.buildEmailDomainValidationLogic(propertyName),
            constraintClass: this.buildConstraintClass(`Valid${propertyName}Domain`, `${propertyName}DomainValidator`, `${propertyName} must be from an allowed domain`),
            validatorClass: this.buildValidatorClass(`${propertyName}DomainValidator`, `Valid${propertyName}Domain`, propertyType, this.buildEmailDomainValidationLogic(propertyName))
        };
    }

    private createPhoneValidator(propertyName: string, propertyType: string): any {
        return {
            name: `${propertyName}FormatValidator`,
            constraintName: `Valid${propertyName}Format`,
            validationType: propertyType,
            message: `${propertyName} must be a valid phone number format`,
            validationLogic: this.buildPhoneValidationLogic(propertyName),
            constraintClass: this.buildConstraintClass(`Valid${propertyName}Format`, `${propertyName}FormatValidator`, `${propertyName} must be a valid phone number format`),
            validatorClass: this.buildValidatorClass(`${propertyName}FormatValidator`, `Valid${propertyName}Format`, propertyType, this.buildPhoneValidationLogic(propertyName))
        };
    }

    private createUrlValidator(propertyName: string, propertyType: string): any {
        return {
            name: `${propertyName}FormatValidator`,
            constraintName: `Valid${propertyName}Format`,
            validationType: propertyType,
            message: `${propertyName} must be a valid URL format`,
            validationLogic: this.buildUrlValidationLogic(propertyName),
            constraintClass: this.buildConstraintClass(`Valid${propertyName}Format`, `${propertyName}FormatValidator`, `${propertyName} must be a valid URL format`),
            validatorClass: this.buildValidatorClass(`${propertyName}FormatValidator`, `Valid${propertyName}Format`, propertyType, this.buildUrlValidationLogic(propertyName))
        };
    }

    private createCodeFormatValidator(propertyName: string, propertyType: string, originalPropertyName: string): any {
        return {
            name: `${propertyName}FormatValidator`,
            constraintName: `Valid${propertyName}Format`,
            validationType: propertyType,
            message: `${propertyName} must follow the required format`,
            validationLogic: this.buildCodeFormatValidationLogic(originalPropertyName),
            constraintClass: this.buildConstraintClass(`Valid${propertyName}Format`, `${propertyName}FormatValidator`, `${propertyName} must follow the required format`),
            validatorClass: this.buildValidatorClass(`${propertyName}FormatValidator`, `Valid${propertyName}Format`, propertyType, this.buildCodeFormatValidationLogic(originalPropertyName))
        };
    }

    private createDateRangeValidator(startProp: any, endProp: any, aggregateName: string): any {
        const startName = this.capitalize(startProp.name);
        const endName = this.capitalize(endProp.name);

        return {
            name: `${startName}${endName}RangeValidator`,
            constraintName: `Valid${startName}${endName}Range`,
            validationType: aggregateName,
            message: `${startName} must be before ${endName}`,
            validationLogic: this.buildDateRangeValidationLogic(startProp.name, endProp.name),
            constraintClass: this.buildConstraintClass(`Valid${startName}${endName}Range`, `${startName}${endName}RangeValidator`, `${startName} must be before ${endName}`),
            validatorClass: this.buildValidatorClass(`${startName}${endName}RangeValidator`, `Valid${startName}${endName}Range`, aggregateName, this.buildDateRangeValidationLogic(startProp.name, endProp.name))
        };
    }

    private isDateRangePair(prop1Name: string, prop2Name: string): boolean {
        const lowerProp1 = prop1Name.toLowerCase();
        const lowerProp2 = prop2Name.toLowerCase();

        return (lowerProp1.includes('start') && lowerProp2.includes('end')) ||
            (lowerProp1.includes('begin') && lowerProp2.includes('end')) ||
            (lowerProp1.includes('from') && lowerProp2.includes('to')) ||
            (lowerProp1.includes('created') && lowerProp2.includes('updated')) ||
            (lowerProp1.includes('open') && lowerProp2.includes('close'));
    }

    private buildBusinessRuleValidationLogic(aggregateName: string): string {
        return `// Implement business rule validation logic for ${aggregateName}
        // Example: Check business hours, validate against external systems, etc.
        // TODO: Implement specific business rules
        return true;`;
    }

    private buildEmailDomainValidationLogic(propertyName: string): string {
        return `// Validate email domain
        if (value.isEmpty()) return true; // Let other validators handle empty values
        
        String[] allowedDomains = {"company.com", "organization.org", "domain.net"};
        String domain = value.substring(value.indexOf('@') + 1);
        
        for (String allowedDomain : allowedDomains) {
            if (domain.equalsIgnoreCase(allowedDomain)) {
                return true;
            }
        }
        
        return false;`;
    }

    private buildPhoneValidationLogic(propertyName: string): string {
        return `// Validate phone number format
        if (value.isEmpty()) return true;
        
        // Remove all non-digits
        String digitsOnly = value.replaceAll("\\\\D", "");
        
        // Check if it's a valid length (10-15 digits)
        return digitsOnly.length() >= 10 && digitsOnly.length() <= 15;`;
    }

    private buildUrlValidationLogic(propertyName: string): string {
        return `// Validate URL format
        if (value.isEmpty()) return true;
        
        try {
            new java.net.URL(value);
            return true;
        } catch (java.net.MalformedURLException e) {
            return false;
        }`;
    }

    private buildCodeFormatValidationLogic(propertyName: string): string {
        return `// Validate code format
        if (value.isEmpty()) return true;
        
        // Example: Code should be alphanumeric with dashes, 6-12 characters
        return value.matches("^[A-Za-z0-9-]{6,12}$");`;
    }

    private buildDateRangeValidationLogic(startPropertyName: string, endPropertyName: string): string {
        return `// Validate date range
        java.lang.reflect.Field startField = null;
        java.lang.reflect.Field endField = null;
        
        try {
            startField = value.getClass().getDeclaredField("${startPropertyName}");
            endField = value.getClass().getDeclaredField("${endPropertyName}");
            startField.setAccessible(true);
            endField.setAccessible(true);
            
            Object startValue = startField.get(value);
            Object endValue = endField.get(value);
            
            if (startValue == null || endValue == null) {
                return true; // Let other validators handle null values
            }
            
            if (startValue instanceof java.time.LocalDateTime && endValue instanceof java.time.LocalDateTime) {
                return ((java.time.LocalDateTime) startValue).isBefore((java.time.LocalDateTime) endValue);
            }
            
            // Add more date type comparisons as needed
            return true;
            
        } catch (Exception e) {
            return false; // Validation failed due to reflection issues
        }`;
    }

    private buildCustomValidatorsImports(aggregate: Aggregate, options: ValidationGenerationOptions, validators: any[]): string[] {
        const projectName = options?.projectName || 'unknown';
        const baseImports = this.buildValidationImports(projectName, aggregate.name);
        const validatorImports = [
            'import java.net.URL;',
            'import java.net.MalformedURLException;',
            'import java.lang.reflect.Field;',
            'import java.time.LocalDateTime;',
            'import java.time.LocalDate;'
        ];

        return this.combineImports(baseImports, validatorImports);
    }

    private getConstraintTemplate(): string {
        return `package {{packageName}};

{{#each imports}}
{{this}}
{{/each}}

{{{validator.constraintClass}}}`;
    }

    private getValidatorTemplate(): string {
        return `package {{packageName}};

{{#each imports}}
{{this}}
{{/each}}

{{{validator.validatorClass}}}`;
    }

}
