import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { TemplateDataBase } from "./template-data-base.js";
import { ValidationData, EntityValidationRules, PropertyValidationRule } from "./template-data-types.js";

export class ValidationDataExtractor extends TemplateDataBase {
    extractValidationData(aggregate: Aggregate): ValidationData[] {
        const validations: ValidationData[] = [];
        const entities = aggregate.entities || [];

        entities.forEach(entity => {
            const entityValidations = this.extractEntityValidations(entity);
            validations.push(...entityValidations);
        });

        return validations;
    }

    extractEntityValidations(entity: Entity): ValidationData[] {
        const validations: ValidationData[] = [];

        if (!entity.properties) return validations;

        entity.properties.forEach(property => {
            const propertyValidation = this.extractPropertyValidation(entity, property);
            if (propertyValidation) {
                validations.push(propertyValidation);
            }
        });

        return validations;
    }

    extractPropertyValidation(entity: Entity, property: any): ValidationData | null {
        const propertyType = this.getPropertyType(property);
        const validationRules = this.buildPropertyValidationRules(property, propertyType);

        if (validationRules.length === 0) {
            return null;
        }

        const primaryRule = validationRules[0];

        return {
            entityName: entity.name,
            propertyName: property.name,
            ruleType: primaryRule.type,
            message: primaryRule.message,
            condition: this.buildValidationCondition(property, primaryRule),
            severity: primaryRule.severity,
            customValidator: this.getCustomValidator(propertyType),
            parameters: primaryRule.parameters
        };
    }

    buildPropertyValidationRules(property: any, propertyType: string): PropertyValidationRule[] {
        const rules: PropertyValidationRule[] = [];

        if (property.required) {
            rules.push({
                type: 'required',
                parameters: {},
                message: `${this.capitalize(property.name)} is required`,
                severity: 'error'
            });
        }

        if (propertyType === 'String') {
            const minLength = this.getMinLength(propertyType) || property.minLength;
            const maxLength = this.getMaxLength(propertyType) || property.maxLength;

            if (minLength || maxLength) {
                rules.push({
                    type: 'length',
                    parameters: { min: minLength || 0, max: maxLength || 255 },
                    message: `${this.capitalize(property.name)} length must be between ${minLength || 0} and ${maxLength || 255} characters`,
                    severity: 'error'
                });
            }

            if (property.name.toLowerCase().includes('email')) {
                rules.push({
                    type: 'email',
                    parameters: {},
                    message: `${this.capitalize(property.name)} must be a valid email address`,
                    severity: 'error'
                });
            }

            const pattern = this.getPattern(propertyType) || property.pattern;
            if (pattern) {
                rules.push({
                    type: 'pattern',
                    parameters: { regex: pattern },
                    message: `${this.capitalize(property.name)} format is invalid`,
                    severity: 'error'
                });
            }
        }

        if (['Integer', 'Long', 'Double', 'Float', 'BigDecimal'].includes(propertyType)) {
            const minValue = this.getMinValue(propertyType) || property.min;
            const maxValue = this.getMaxValue(propertyType) || property.max;

            if (minValue !== undefined || maxValue !== undefined) {
                rules.push({
                    type: 'range',
                    parameters: { min: minValue, max: maxValue },
                    message: `${this.capitalize(property.name)} must be between ${minValue || 'any'} and ${maxValue || 'any'}`,
                    severity: 'error'
                });
            }

            if (property.positive || property.name.toLowerCase().includes('positive')) {
                rules.push({
                    type: 'positive',
                    parameters: {},
                    message: `${this.capitalize(property.name)} must be positive`,
                    severity: 'error'
                });
            }
        }

        if (this.isCollectionType(property.type)) {
            if (property.notEmpty || property.required) {
                rules.push({
                    type: 'notEmpty',
                    parameters: {},
                    message: `${this.capitalize(property.name)} cannot be empty`,
                    severity: 'error'
                });
            }

            const minSize = property.minSize;
            const maxSize = property.maxSize;

            if (minSize || maxSize) {
                rules.push({
                    type: 'size',
                    parameters: { min: minSize || 0, max: maxSize || 100 },
                    message: `${this.capitalize(property.name)} size must be between ${minSize || 0} and ${maxSize || 100}`,
                    severity: 'error'
                });
            }
        }

        if (['LocalDate', 'LocalDateTime', 'Date'].includes(propertyType)) {
            if (property.name.toLowerCase().includes('past') || property.past) {
                rules.push({
                    type: 'past',
                    parameters: {},
                    message: `${this.capitalize(property.name)} must be in the past`,
                    severity: 'error'
                });
            }

            if (property.name.toLowerCase().includes('future') || property.future) {
                rules.push({
                    type: 'future',
                    parameters: {},
                    message: `${this.capitalize(property.name)} must be in the future`,
                    severity: 'error'
                });
            }
        }

        const customValidator = this.getCustomValidator(propertyType) || property.customValidator;
        if (customValidator) {
            rules.push({
                type: 'custom',
                parameters: { validator: customValidator },
                message: `${this.capitalize(property.name)} validation failed`,
                severity: 'error'
            });
        }

        return rules;
    }

    buildValidationCondition(property: any, rule: PropertyValidationRule): string {
        const propertyName = property.name;

        switch (rule.type) {
            case 'required':
                return `${propertyName} != null`;

            case 'length':
                const min = rule.parameters.min || 0;
                const max = rule.parameters.max || 255;
                return `${propertyName} != null && ${propertyName}.length() >= ${min} && ${propertyName}.length() <= ${max}`;

            case 'email':
                return `${propertyName} != null && ${propertyName}.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\\\.[A-Za-z]{2,})$")`;

            case 'pattern':
                const regex = rule.parameters.regex || '.*';
                return `${propertyName} != null && ${propertyName}.matches("${regex}")`;

            case 'range':
                const minVal = rule.parameters.min;
                const maxVal = rule.parameters.max;
                let condition = `${propertyName} != null`;
                if (minVal !== undefined) {
                    condition += ` && ${propertyName} >= ${minVal}`;
                }
                if (maxVal !== undefined) {
                    condition += ` && ${propertyName} <= ${maxVal}`;
                }
                return condition;

            case 'positive':
                return `${propertyName} != null && ${propertyName} > 0`;

            case 'notEmpty':
                return `${propertyName} != null && !${propertyName}.isEmpty()`;

            case 'size':
                const minSize = rule.parameters.min || 0;
                const maxSize = rule.parameters.max || 100;
                return `${propertyName} != null && ${propertyName}.size() >= ${minSize} && ${propertyName}.size() <= ${maxSize}`;

            case 'past':
                return `${propertyName} != null && ${propertyName}.isBefore(LocalDateTime.now())`;

            case 'future':
                return `${propertyName} != null && ${propertyName}.isAfter(LocalDateTime.now())`;

            case 'custom':
                const validator = rule.parameters.validator;
                return `${validator}.isValid(${propertyName})`;

            default:
                return 'true';
        }
    }

    buildEntityValidationRules(entity: Entity): EntityValidationRules {
        const rules: EntityValidationRules = {
            entityName: entity.name,
            properties: {},
            classLevel: []
        };

        if (!entity.properties) return rules;

        entity.properties.forEach(property => {
            const propertyType = this.getPropertyType(property);
            const propertyRules = this.buildPropertyValidationRules(property, propertyType);

            if (propertyRules.length > 0) {
                rules.properties[property.name] = propertyRules;
            }
        });

        rules.classLevel = this.buildClassLevelValidations(entity);

        return rules;
    }

    buildClassLevelValidations(entity: Entity): PropertyValidationRule[] {
        const rules: PropertyValidationRule[] = [];

        if (entity.name.toLowerCase().includes('order')) {
            rules.push({
                type: 'business',
                parameters: { rule: 'orderDateBeforeShipDate' },
                message: 'Order date must be before ship date',
                severity: 'error'
            });
        }

        if (entity.name.toLowerCase().includes('user') || entity.name.toLowerCase().includes('account')) {
            rules.push({
                type: 'business',
                parameters: { rule: 'uniqueUsername' },
                message: 'Username must be unique',
                severity: 'error'
            });
        }

        return rules;
    }

    getValidationAnnotations(validationData: ValidationData): string[] {
        const annotations: string[] = [];

        switch (validationData.ruleType) {
            case 'required':
                annotations.push('@NotNull');
                break;

            case 'length':
                const params = validationData.parameters;
                if (params) {
                    annotations.push(`@Size(min = ${params.min || 0}, max = ${params.max || 255})`);
                }
                break;

            case 'email':
                annotations.push('@Email');
                break;

            case 'pattern':
                if (validationData.parameters?.regex) {
                    annotations.push(`@Pattern(regexp = "${validationData.parameters.regex}")`);
                }
                break;

            case 'range':
                if (validationData.parameters?.min !== undefined) {
                    annotations.push(`@Min(${validationData.parameters.min})`);
                }
                if (validationData.parameters?.max !== undefined) {
                    annotations.push(`@Max(${validationData.parameters.max})`);
                }
                break;

            case 'positive':
                annotations.push('@Positive');
                break;

            case 'notEmpty':
                annotations.push('@NotEmpty');
                break;

            case 'size':
                if (validationData.parameters) {
                    annotations.push(`@Size(min = ${validationData.parameters.min || 0}, max = ${validationData.parameters.max || 100})`);
                }
                break;

            case 'past':
                annotations.push('@Past');
                break;

            case 'future':
                annotations.push('@Future');
                break;

            case 'custom':
                if (validationData.customValidator) {
                    annotations.push(`@${this.getValidatorAnnotationName(validationData.customValidator)}`);
                }
                break;
        }

        return annotations;
    }

    getValidatorAnnotationName(validatorClass: string): string {
        const parts = validatorClass.split('.');
        const className = parts[parts.length - 1];

        if (className.endsWith('Validator')) {
            return className.replace('Validator', '');
        }

        return className;
    }
}
