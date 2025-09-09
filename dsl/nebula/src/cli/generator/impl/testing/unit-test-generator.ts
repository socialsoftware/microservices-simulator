import * as path from 'path';
import { TestBaseGenerator } from './test-base-generator.js';
import { TestGenerationOptions } from './test-types.js';

export class UnitTestGenerator extends TestBaseGenerator {
    async generateUnitTests(options: TestGenerationOptions): Promise<void> {
        const { projectName, outputDirectory } = options;

        const testPath = path.join(outputDirectory, 'src', 'test', 'groovy',
            'pt', 'ulisboa', 'tecnico', 'socialsoftware', projectName.toLowerCase());

        const context = this.createTestContext(options, testPath);

        await this.generateSpockBaseTest(projectName, testPath);
        await this.generateProjectSpockTest(context);
        await this.generateBeanConfiguration(context);
    }

    private async generateSpockBaseTest(projectName: string, testPath: string): Promise<void> {
        const packageName = 'pt.ulisboa.tecnico.socialsoftware';
        const content = `package ${packageName}

import spock.lang.Specification

class SpockTest extends Specification {
}
`;
        const filePath = path.join(path.dirname(testPath), 'SpockTest.groovy');
        await this.writeTestFile(filePath, content, 'SpockTest.groovy');
    }

    private async generateProjectSpockTest(context: any): Promise<void> {
        const { projectName, aggregateName, entityName, testPath } = context;

        const packageName = this.buildTestPackageName(projectName);
        const imports = this.buildTestImports(projectName, aggregateName, [
            `import pt.ulisboa.tecnico.socialsoftware.${projectName.toLowerCase()}.microservices.${aggregateName.toLowerCase()}.aggregate.${entityName}Dto`
        ]);

        const classBody = this.generateProjectTestMethods(context);
        const content = this.buildGroovyClass(packageName, `${this.capitalize(projectName)}SpockTest`, imports, classBody);

        const filePath = path.join(testPath, `${this.capitalize(projectName)}SpockTest.groovy`);
        await this.writeTestFile(filePath, content, `${this.capitalize(projectName)}SpockTest.groovy`);
    }

    private generateProjectTestMethods(context: any): string {
        const { aggregateName, entityName } = context;
        const lowerAggregate = aggregateName.toLowerCase();

        const setupMethod = `    @Autowired
    ${aggregateName}Functionalities ${lowerAggregate}Functionalities

    def setup() {
        BehaviourService.clearBehaviour()
    }`;

        const testMethod = this.buildSpockTest(
            `should create ${entityName}`,
            `        def ${lowerAggregate}Dto = new ${entityName}Dto()
        ${lowerAggregate}Functionalities.create${aggregateName}(${lowerAggregate}Dto)`,
            '',
            ''
        );

        return `${setupMethod}

${testMethod}`;
    }

    private async generateBeanConfiguration(context: any): Promise<void> {
        const { projectName, features, testPath } = context;
        const capitalizedProjectName = this.capitalize(projectName);

        const packageName = this.buildTestPackageName(projectName);
        const imports = [
            'import org.springframework.boot.test.context.SpringBootTest',
            'import org.springframework.test.context.TestPropertySource',
            'import org.springframework.context.annotation.Bean',
            'import org.springframework.context.annotation.Configuration',
            'import org.springframework.context.annotation.Primary'
        ];

        if (this.hasFeature(features, 'saga')) {
            imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.coordination.SagaManager');
        }

        const classBody = this.generateBeanConfigurationBody(features);
        const content = this.buildGroovyClass(packageName, `${capitalizedProjectName}BeanConfiguration`, imports, classBody);

        const filePath = path.join(testPath, `${capitalizedProjectName}BeanConfiguration.groovy`);
        await this.writeTestFile(filePath, content, `${capitalizedProjectName}BeanConfiguration.groovy`);
    }

    private generateBeanConfigurationBody(features: string[]): string {
        let configBody = `    @Configuration
    static class TestConfiguration {`;

        if (this.hasFeature(features, 'saga')) {
            configBody += `
        
        @Bean
        @Primary
        SagaManager sagaManager() {
            return Mock(SagaManager)
        }`;
        }

        if (this.hasFeature(features, 'webapi')) {
            configBody += `
        
        @Bean
        @Primary
        RestTemplate restTemplate() {
            return Mock(RestTemplate)
        }`;
        }

        configBody += '\n    }';
        return configBody;
    }
}
