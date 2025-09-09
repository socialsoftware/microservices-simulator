import * as path from 'path';
import { TestBaseGenerator } from './test-base-generator.js';
import { TestGenerationOptions } from './test-types.js';

export class SagaTestGenerator extends TestBaseGenerator {
    async generateSagaTests(options: TestGenerationOptions): Promise<void> {
        const { projectName, outputDirectory } = options;

        const sagaTestPath = await this.ensureTestDirectory(
            outputDirectory, 'src', 'test', 'groovy',
            'pt', 'ulisboa', 'tecnico', 'socialsoftware',
            projectName.toLowerCase(), 'sagas', 'coordination'
        );

        const context = this.createTestContext(options, sagaTestPath);

        await this.generateSagaCoordinationTests(context);
    }

    private async generateSagaCoordinationTests(context: any): Promise<void> {
        const { projectName, aggregateName, testPath } = context;

        const packageName = this.buildTestPackageName(projectName, 'sagas', 'coordination');
        const imports = this.buildSagaTestImports(projectName, aggregateName);

        const classBody = this.generateSagaTestMethods(context);
        const content = this.buildGroovyClass(packageName, `${aggregateName}SagaCoordinationTest`, imports, classBody);

        const filePath = path.join(testPath, `${aggregateName}SagaCoordinationTest.groovy`);
        await this.writeTestFile(filePath, content, `${aggregateName}SagaCoordinationTest.groovy`);
    }

    private buildSagaTestImports(projectName: string, aggregateName: string): string[] {
        const baseImports = this.buildTestImports(projectName, aggregateName);
        const sagaImports = [
            `import pt.ulisboa.tecnico.socialsoftware.${projectName.toLowerCase()}.coordination.sagas.${aggregateName}SagaState`,
            `import pt.ulisboa.tecnico.socialsoftware.${projectName.toLowerCase()}.coordination.sagas.${aggregateName}SagaCoordination`,
            'import pt.ulisboa.tecnico.socialsoftware.ms.coordination.SagaManager',
            'import pt.ulisboa.tecnico.socialsoftware.ms.coordination.SagaState',
            'import pt.ulisboa.tecnico.socialsoftware.ms.coordination.SagaTransaction'
        ];

        return this.combineImports(baseImports, sagaImports);
    }

    private generateSagaTestMethods(context: any): string {
        const { aggregateName, entityName } = context;
        const lowerAggregate = aggregateName.toLowerCase();

        const setupMethod = `    @Autowired
    ${aggregateName}SagaCoordination ${lowerAggregate}SagaCoordination

    @Autowired
    SagaManager sagaManager

    def setup() {
        BehaviourService.clearBehaviour()
    }`;

        const createSagaTest = this.buildSpockTest(
            `should create ${aggregateName} saga successfully`,
            `        def sagaId = "${lowerAggregate}_saga_" + System.currentTimeMillis()
        def ${lowerAggregate}Dto = new ${entityName}Dto()
        
        def sagaState = ${lowerAggregate}SagaCoordination.create${aggregateName}Saga(sagaId, ${lowerAggregate}Dto)`,
            '',
            ''
        );

        const compensateSagaTest = this.buildSpockTest(
            `should compensate ${aggregateName} saga on failure`,
            `        def sagaId = "${lowerAggregate}_saga_" + System.currentTimeMillis()
        def ${lowerAggregate}Dto = new ${entityName}Dto()
        
        // Simulate failure
        BehaviourService.addBehaviour("${aggregateName}Service", "create${aggregateName}", "EXCEPTION")
        
        def sagaState = ${lowerAggregate}SagaCoordination.create${aggregateName}Saga(sagaId, ${lowerAggregate}Dto)`,
            '',
            'BehaviourService.clearBehaviour()'
        );

        const sagaTimeoutTest = this.buildSpockTest(
            `should handle ${aggregateName} saga timeout`,
            `        def sagaId = "${lowerAggregate}_saga_" + System.currentTimeMillis()
        def ${lowerAggregate}Dto = new ${entityName}Dto()
        
        // Simulate timeout
        BehaviourService.addBehaviour("${aggregateName}Service", "create${aggregateName}", "TIMEOUT")
        
        def sagaState = ${lowerAggregate}SagaCoordination.create${aggregateName}Saga(sagaId, ${lowerAggregate}Dto)`,
            '',
            'BehaviourService.clearBehaviour()'
        );

        return `${setupMethod}

${createSagaTest}

${compensateSagaTest}

${sagaTimeoutTest}`;
    }
}
