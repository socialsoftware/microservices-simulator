import * as path from 'path';
import { TestBaseGenerator } from './test-base-generator.js';
import { TestGenerationOptions } from './test-types.js';

export class TestDataBuilder extends TestBaseGenerator {
    async generateTestDataBuilders(options: TestGenerationOptions): Promise<void> {
        const { projectName, outputDirectory } = options;

        const buildersTestPath = await this.ensureTestDirectory(
            outputDirectory, 'src', 'test', 'groovy',
            'pt', 'ulisboa', 'tecnico', 'socialsoftware',
            projectName.toLowerCase(), 'builders'
        );

        const context = this.createTestContext(options, buildersTestPath);

        await this.generateEntityBuilders(context);
        await this.generateDtoBuilders(context);
    }

    private async generateEntityBuilders(context: any): Promise<void> {
        const { aggregate, projectName, testPath } = context;

        for (const entity of aggregate.entities) {
            await this.generateEntityBuilder(entity, projectName, testPath);
        }
    }

    private async generateEntityBuilder(entity: any, projectName: string, testPath: string): Promise<void> {
        const entityName = entity.name;
        const packageName = this.buildTestPackageName(projectName, 'builders');

        const imports = [
            `import pt.ulisboa.tecnico.socialsoftware.${projectName.toLowerCase()}.microservices.${entity.aggregate?.toLowerCase() || 'default'}.aggregate.${entityName}`,
            'import java.time.LocalDateTime'
        ];

        const classBody = this.generateEntityBuilderBody(entity);
        const content = this.buildGroovyClass(packageName, `${entityName}Builder`, imports, classBody);

        const filePath = path.join(testPath, `${entityName}Builder.groovy`);
        await this.writeTestFile(filePath, content, `${entityName}Builder.groovy`);
    }

    private generateEntityBuilderBody(entity: any): string {
        const entityName = entity.name;
        const properties = entity.properties || [];

        let builderBody = `    private ${entityName} ${entityName.toLowerCase()}

    static ${entityName}Builder a${entityName}() {
        return new ${entityName}Builder()
    }

    ${entityName}Builder() {
        this.${entityName.toLowerCase()} = new ${entityName}()
        // Set default values
        this.${entityName.toLowerCase()}.setId(1L)
        this.${entityName.toLowerCase()}.setVersion(1)`;

        properties.forEach((property: any) => {
            const propName = property.name;
            const defaultValue = this.getDefaultValueForProperty(property);
            if (defaultValue && propName.toLowerCase() !== 'id' && propName.toLowerCase() !== 'version') {
                builderBody += `\n        this.${entityName.toLowerCase()}.set${this.capitalize(propName)}(${defaultValue})`;
            }
        });

        builderBody += '\n    }';

        properties.forEach((property: any) => {
            const propName = property.name;
            const capitalizedProp = this.capitalize(propName);
            const propType = this.resolveJavaType(property.type);

            builderBody += `

    ${entityName}Builder with${capitalizedProp}(${propType} ${propName}) {
        this.${entityName.toLowerCase()}.set${capitalizedProp}(${propName})
        return this
    }`;
        });

        builderBody += `

    ${entityName} build() {
        return this.${entityName.toLowerCase()}
    }`;

        return builderBody;
    }

    private async generateDtoBuilders(context: any): Promise<void> {
        const { aggregate, projectName, testPath } = context;

        for (const entity of aggregate.entities) {
            await this.generateDtoBuilder(entity, projectName, testPath);
        }
    }

    private async generateDtoBuilder(entity: any, projectName: string, testPath: string): Promise<void> {
        const entityName = entity.name;
        const dtoName = `${entityName}Dto`;
        const packageName = this.buildTestPackageName(projectName, 'builders');

        const imports = [
            `import pt.ulisboa.tecnico.socialsoftware.${projectName.toLowerCase()}.microservices.${entity.aggregate?.toLowerCase() || 'default'}.aggregate.${dtoName}`,
            'import java.time.LocalDateTime'
        ];

        const classBody = this.generateDtoBuilderBody(entity);
        const content = this.buildGroovyClass(packageName, `${dtoName}Builder`, imports, classBody);

        const filePath = path.join(testPath, `${dtoName}Builder.groovy`);
        await this.writeTestFile(filePath, content, `${dtoName}Builder.groovy`);
    }

    private generateDtoBuilderBody(entity: any): string {
        const entityName = entity.name;
        const dtoName = `${entityName}Dto`;
        const properties = entity.properties || [];

        let builderBody = `    private ${dtoName} ${entityName.toLowerCase()}Dto

    static ${dtoName}Builder a${dtoName}() {
        return new ${dtoName}Builder()
    }

    ${dtoName}Builder() {
        this.${entityName.toLowerCase()}Dto = new ${dtoName}()
        // Set default values`;

        // Add default values for properties
        properties.forEach((property: any) => {
            const propName = property.name;
            const defaultValue = this.getDefaultValueForProperty(property);
            if (defaultValue) {
                builderBody += `\n        this.${entityName.toLowerCase()}Dto.set${this.capitalize(propName)}(${defaultValue})`;
            }
        });

        builderBody += '\n    }';

        // Add builder methods for each property
        properties.forEach((property: any) => {
            const propName = property.name;
            const capitalizedProp = this.capitalize(propName);
            const propType = this.resolveJavaType(property.type);

            builderBody += `

    ${dtoName}Builder with${capitalizedProp}(${propType} ${propName}) {
        this.${entityName.toLowerCase()}Dto.set${capitalizedProp}(${propName})
        return this
    }`;
        });

        builderBody += `

    ${dtoName} build() {
        return this.${entityName.toLowerCase()}Dto
    }`;

        return builderBody;
    }

    private getDefaultValueForProperty(property: any): string | null {
        const propType = this.resolveJavaType(property.type);
        const propName = property.name;

        if (propName.toLowerCase() === 'id') {
            return '1L';
        }

        if (propName.toLowerCase() === 'version') {
            return '1';
        }

        switch (propType.toLowerCase()) {
            case 'string':
                return `"Default ${propName}"`;
            case 'integer':
            case 'int':
                return '1';
            case 'long':
                return '1L';
            case 'double':
                return '1.0';
            case 'float':
                return '1.0f';
            case 'boolean':
                return 'false';
            case 'localdatetime':
                return 'LocalDateTime.now()';
            case 'localdate':
                return 'LocalDate.now()';
            default:
                return null;
        }
    }
}
