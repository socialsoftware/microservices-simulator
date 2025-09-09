import { Method, Parameter } from "../../../language/generated/ast.js";

export interface MethodData {
    name: string;
    parameters: ParameterData[];
    returnType: string;
    javaReturnType: string;
    body?: string;
    isPublic: boolean;
    isStatic: boolean;
    isAbstract: boolean;
    isSynchronized: boolean;
    annotations: string[];
    modifiers: string[];
    visibility: 'public' | 'private' | 'protected' | 'package';
    isConstructor: boolean;
    isGetter: boolean;
    isSetter: boolean;
    isBusinessMethod: boolean;
    throwsExceptions: string[];
    genericTypes: string[];
    documentation?: string;
}

export interface ParameterData {
    name: string;
    type: string;
    javaType: string;
    isRequired: boolean;
    isCollection: boolean;
    isEntity: boolean;
    isPrimitive: boolean;
    defaultValue?: string;
    annotations: string[];
    isFinal: boolean;
    isVarArgs: boolean;
}

export class MethodParser {
    private javaTypeMap!: Map<string, string>;
    private annotationConfig!: Map<string, string[]>;
    private businessKeywords!: string[];
    private primitiveTypes!: string[];

    constructor() {
        this.initializeTypeMappings();
        this.initializeAnnotationConfig();
        this.initializeBusinessKeywords();
        this.initializePrimitiveTypes();
    }

    private initializeTypeMappings(): void {
        this.javaTypeMap = new Map([
            ['String', 'String'],
            ['Integer', 'Integer'],
            ['Long', 'Long'],
            ['Double', 'Double'],
            ['Float', 'Float'],
            ['Boolean', 'Boolean'],
            ['Date', 'LocalDateTime'],
            ['DateTime', 'LocalDateTime'],
            ['LocalDateTime', 'LocalDateTime'],
            ['LocalDate', 'LocalDate'],
            ['LocalTime', 'LocalTime'],
            ['BigDecimal', 'BigDecimal'],
            ['UUID', 'UUID'],
            ['List', 'List'],
            ['Set', 'Set'],
            ['Map', 'Map'],
            ['Optional', 'Optional']
        ]);
    }

    private initializeAnnotationConfig(): void {
        this.annotationConfig = new Map([
            ['businessMethod', ['@Transactional']],
            ['getterMethod', ['@Override']],
            ['setterMethod', ['@Override']],
            ['validationMethod', ['@Valid']],
            ['requiredParameter', ['@NotNull']],
            ['entityParameter', ['@Valid']]
        ]);
    }

    private initializeBusinessKeywords(): void {
        this.businessKeywords = ['create', 'update', 'delete', 'process', 'execute', 'handle', 'validate', 'calculate', 'submit', 'approve', 'reject', 'cancel'];
    }

    private initializePrimitiveTypes(): void {
        this.primitiveTypes = ['String', 'Integer', 'Long', 'Double', 'Float', 'Boolean', 'Date', 'DateTime', 'LocalDateTime', 'LocalDate', 'LocalTime', 'BigDecimal', 'UUID'];
    }

    parseMethod(method: Method): MethodData {
        const methodData: MethodData = {
            name: method.name || 'unnamedMethod',
            parameters: this.parseParameters(method.parameters || []),
            returnType: this.extractReturnType(method),
            javaReturnType: this.resolveJavaType(this.extractReturnType(method)),
            body: this.extractMethodBody(method),
            isPublic: this.isPublicMethod(method),
            isStatic: this.isStaticMethod(method),
            isAbstract: this.isAbstractMethod(method),
            isSynchronized: this.isSynchronizedMethod(method),
            annotations: this.extractAnnotations(method),
            modifiers: this.extractModifiers(method),
            visibility: this.determineVisibility(method),
            isConstructor: this.isConstructor(method),
            isGetter: this.isGetterMethod(method),
            isSetter: this.isSetterMethod(method),
            isBusinessMethod: this.isBusinessMethod(method),
            throwsExceptions: this.extractThrowsExceptions(method),
            genericTypes: this.extractGenericTypes(method),
            documentation: this.extractDocumentation(method)
        };

        return methodData;
    }

    private parseParameters(parameters: Parameter[]): ParameterData[] {
        return parameters.map(param => this.parseParameter(param));
    }

    private parseParameter(parameter: Parameter): ParameterData {
        const paramType = this.extractParameterType(parameter);

        return {
            name: parameter.name || 'unnamedParam',
            type: paramType,
            javaType: this.resolveJavaType(paramType),
            isRequired: !this.isOptionalType(paramType),
            isCollection: this.isCollectionType(paramType),
            isEntity: this.isEntityType(paramType),
            isPrimitive: this.isPrimitiveType(paramType),
            defaultValue: this.extractDefaultValue(parameter),
            annotations: this.extractParameterAnnotations(parameter),
            isFinal: this.isFinalParameter(parameter),
            isVarArgs: this.isVarArgsParameter(parameter)
        };
    }

    private extractReturnType(method: Method): string {
        if (method.returnType) {
            if (typeof method.returnType === 'string') {
                return method.returnType;
            } else if (method.returnType.$type === 'PrimitiveType') {
                return (method.returnType as any).name || 'void';
            } else if (method.returnType.$type === 'EntityType') {
                return (method.returnType as any).name || 'void';
            }
        }
        return 'void';
    }

    private extractMethodBody(method: Method): string | undefined {
        return undefined;
    }

    private isPublicMethod(method: Method): boolean {
        return this.hasModifier(method, 'public') || this.determineVisibility(method) === 'public';
    }

    private isStaticMethod(method: Method): boolean {
        return this.hasModifier(method, 'static');
    }

    private isAbstractMethod(method: Method): boolean {
        return this.hasModifier(method, 'abstract') || !this.extractMethodBody(method);
    }

    private isSynchronizedMethod(method: Method): boolean {
        return this.hasModifier(method, 'synchronized');
    }

    private extractAnnotations(method: Method): string[] {
        const annotations: string[] = [];

        if (this.isBusinessMethod(method)) {
            const businessAnnotations = this.annotationConfig.get('businessMethod') || [];
            annotations.push(...businessAnnotations);
        }

        if (this.isGetterMethod(method)) {
            const getterAnnotations = this.annotationConfig.get('getterMethod') || [];
            annotations.push(...getterAnnotations);
        }

        if (this.isSetterMethod(method)) {
            const setterAnnotations = this.annotationConfig.get('setterMethod') || [];
            annotations.push(...setterAnnotations);
        }

        if (this.hasValidationParameters(method)) {
            const validationAnnotations = this.annotationConfig.get('validationMethod') || [];
            annotations.push(...validationAnnotations);
        }

        return annotations;
    }

    private extractModifiers(method: Method): string[] {
        const modifiers: string[] = [];

        if (this.isPublicMethod(method)) modifiers.push('public');
        if (this.isStaticMethod(method)) modifiers.push('static');
        if (this.isAbstractMethod(method)) modifiers.push('abstract');
        if (this.isSynchronizedMethod(method)) modifiers.push('synchronized');

        return modifiers;
    }

    private determineVisibility(method: Method): 'public' | 'private' | 'protected' | 'package' {
        if (this.hasModifier(method, 'private')) return 'private';
        if (this.hasModifier(method, 'protected')) return 'protected';
        if (this.hasModifier(method, 'public')) return 'public';
        return 'public'; // Default to public
    }

    private isConstructor(method: Method): boolean {
        return method.name === 'constructor' || method.name === 'new';
    }

    private isGetterMethod(method: Method): boolean {
        const name = method.name || '';
        return (name.startsWith('get') && name.length > 3) ||
            (name.startsWith('is') && name.length > 2) ||
            (name.startsWith('has') && name.length > 3);
    }

    private isSetterMethod(method: Method): boolean {
        const name = method.name || '';
        return name.startsWith('set') && name.length > 3;
    }

    private isBusinessMethod(method: Method): boolean {
        const name = method.name || '';
        return this.businessKeywords.some(keyword => name.toLowerCase().includes(keyword));
    }

    private extractThrowsExceptions(method: Method): string[] {
        const exceptions: string[] = [];

        if (this.isBusinessMethod(method)) {
            exceptions.push('Exception');
        }

        return exceptions;
    }

    private extractGenericTypes(method: Method): string[] {
        return [];
    }

    private extractDocumentation(method: Method): string | undefined {
        return undefined;
    }

    private extractParameterType(parameter: Parameter): string {
        if (parameter.type) {
            if (typeof parameter.type === 'string') {
                return parameter.type;
            } else if (parameter.type.$type === 'PrimitiveType') {
                return (parameter.type as any).name || 'String';
            } else if (parameter.type.$type === 'EntityType') {
                return (parameter.type as any).name || 'String';
            }
        }
        return 'String';
    }

    private extractDefaultValue(parameter: Parameter): string | undefined {
        return undefined;
    }

    private extractParameterAnnotations(parameter: Parameter): string[] {
        const annotations: string[] = [];

        if (this.isRequiredParameter(parameter)) {
            const requiredAnnotations = this.annotationConfig.get('requiredParameter') || [];
            annotations.push(...requiredAnnotations);
        }

        if (this.isEntityType(this.extractParameterType(parameter))) {
            const entityAnnotations = this.annotationConfig.get('entityParameter') || [];
            annotations.push(...entityAnnotations);
        }

        return annotations;
    }

    private isFinalParameter(parameter: Parameter): boolean {
        return this.hasParameterModifier(parameter, 'final');
    }

    private isVarArgsParameter(parameter: Parameter): boolean {
        return this.hasParameterModifier(parameter, 'varargs') ||
            this.extractParameterType(parameter).includes('...');
    }

    private isRequiredParameter(parameter: Parameter): boolean {
        const paramType = this.extractParameterType(parameter);
        return !this.isOptionalType(paramType);
    }

    private hasValidationParameters(method: Method): boolean {
        if (!method.parameters) return false;

        return method.parameters.some(param => {
            const paramType = this.extractParameterType(param);
            return this.isEntityType(paramType) || this.isRequiredParameter(param);
        });
    }

    private hasModifier(method: Method, modifier: string): boolean {
        return false;
    }

    private hasParameterModifier(parameter: Parameter, modifier: string): boolean {
        return false;
    }

    private resolveJavaType(type: string): string {
        if (typeof type !== 'string') return 'String';
        if (type.includes('<')) {
            const baseType = type.split('<')[0];
            const genericType = type.split('<')[1].replace('>', '');
            const resolvedBase = this.javaTypeMap.get(baseType) || baseType;
            const resolvedGeneric = this.javaTypeMap.get(genericType) || genericType;
            return `${resolvedBase}<${resolvedGeneric}>`;
        }

        return this.javaTypeMap.get(type) || type;
    }

    private isCollectionType(type: string): boolean {
        if (typeof type !== 'string') return false;
        return type.includes('List') || type.includes('Set') || type.includes('Collection') || type.includes('[]');
    }

    private isEntityType(type: string): boolean {
        if (typeof type !== 'string') return false;
        return !this.isPrimitiveType(type) && !this.isCollectionType(type);
    }

    private isPrimitiveType(type: string): boolean {
        if (typeof type !== 'string') return false;
        return this.primitiveTypes.includes(type);
    }

    private isOptionalType(type: string): boolean {
        if (typeof type !== 'string') return false;
        return type.includes('Optional') || type.includes('?');
    }

    getMethodSignature(methodData: MethodData): string {
        const modifiers = methodData.modifiers.join(' ');
        const annotations = methodData.annotations.length > 0 ? methodData.annotations.join(' ') + ' ' : '';
        const parameters = methodData.parameters.map(param =>
            `${param.annotations.join(' ')} ${param.javaType} ${param.name}`
        ).join(', ');
        const throws = methodData.throwsExceptions.length > 0 ? ` throws ${methodData.throwsExceptions.join(', ')}` : '';

        return `${annotations}${modifiers} ${methodData.javaReturnType} ${methodData.name}(${parameters})${throws}`;
    }

    getParameterSignature(parameterData: ParameterData): string {
        const annotations = parameterData.annotations.length > 0 ? parameterData.annotations.join(' ') + ' ' : '';
        const finalModifier = parameterData.isFinal ? 'final ' : '';
        const varArgs = parameterData.isVarArgs ? '...' : '';

        return `${annotations}${finalModifier}${parameterData.javaType}${varArgs} ${parameterData.name}`;
    }

    configureTypeMappings(mappings: Map<string, string>): void {
        this.javaTypeMap = new Map([...this.javaTypeMap, ...mappings]);
    }

    configureAnnotations(annotations: Map<string, string[]>): void {
        this.annotationConfig = new Map([...this.annotationConfig, ...annotations]);
    }

    configureBusinessKeywords(keywords: string[]): void {
        this.businessKeywords = [...this.businessKeywords, ...keywords];
    }

    configurePrimitiveTypes(types: string[]): void {
        this.primitiveTypes = [...this.primitiveTypes, ...types];
    }

    getConfiguration(): {
        typeMappings: Map<string, string>;
        annotations: Map<string, string[]>;
        businessKeywords: string[];
        primitiveTypes: string[];
    } {
        return {
            typeMappings: new Map(this.javaTypeMap),
            annotations: new Map(this.annotationConfig),
            businessKeywords: [...this.businessKeywords],
            primitiveTypes: [...this.primitiveTypes]
        };
    }
}
