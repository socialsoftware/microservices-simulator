import * as path from 'path';
import { TestBaseGenerator } from './test-base-generator.js';
import { TestGenerationOptions } from './test-types.js';

export class ApiTestGenerator extends TestBaseGenerator {
    async generateApiTests(options: TestGenerationOptions): Promise<void> {
        const { projectName, outputDirectory } = options;

        const webapiTestPath = await this.ensureTestDirectory(
            outputDirectory, 'src', 'test', 'groovy',
            'pt', 'ulisboa', 'tecnico', 'socialsoftware',
            projectName.toLowerCase(), 'webapi'
        );

        const context = this.createTestContext(options, webapiTestPath);

        await this.generateRestApiTests(context);
    }

    private async generateRestApiTests(context: any): Promise<void> {
        const { projectName, aggregateName, entityName, testPath } = context;

        const packageName = this.buildTestPackageName(projectName, 'webapi');
        const imports = this.buildApiTestImports(projectName, aggregateName, entityName);

        const classBody = this.generateApiTestMethods(context);
        const content = this.buildGroovyClass(packageName, `${aggregateName}RestApiTest`, imports, classBody);

        const filePath = path.join(testPath, `${aggregateName}RestApiTest.groovy`);
        await this.writeTestFile(filePath, content, `${aggregateName}RestApiTest.groovy`);
    }

    private buildApiTestImports(projectName: string, aggregateName: string, entityName: string): string[] {
        const baseImports = this.buildTestImports(projectName, aggregateName);
        const apiImports = [
            'import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc',
            'import org.springframework.test.web.servlet.MockMvc',
            'import org.springframework.test.web.servlet.request.MockMvcRequestBuilders',
            'import org.springframework.test.web.servlet.result.MockMvcResultMatchers',
            'import org.springframework.http.MediaType',
            'import com.fasterxml.jackson.databind.ObjectMapper',
            `import pt.ulisboa.tecnico.socialsoftware.${projectName.toLowerCase()}.microservices.${aggregateName.toLowerCase()}.aggregate.${entityName}Dto`,
            `import pt.ulisboa.tecnico.socialsoftware.${projectName.toLowerCase()}.microservices.${aggregateName.toLowerCase()}.webapi.${aggregateName}Controller`
        ];

        return this.combineImports(baseImports, apiImports);
    }

    private generateApiTestMethods(context: any): string {
        const { aggregateName, entityName } = context;
        const lowerAggregate = aggregateName.toLowerCase();

        const setupMethod = `    @Autowired
    MockMvc mockMvc

    @Autowired
    ObjectMapper objectMapper

    @Autowired
    ${aggregateName}Functionalities ${lowerAggregate}Functionalities

    def setup() {
        BehaviourService.clearBehaviour()
    }`;

        const createApiTest = this.buildSpockTest(
            `should create ${entityName} via REST API`,
            `        def ${lowerAggregate}Dto = new ${entityName}Dto()
        def jsonContent = objectMapper.writeValueAsString(${lowerAggregate}Dto)
        
        mockMvc.perform(MockMvcRequestBuilders.post("/api/${lowerAggregate}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(MockMvcResultMatchers.status().isCreated())`,
            '',
            ''
        );

        const getApiTest = this.buildSpockTest(
            `should get ${entityName} via REST API`,
            `        def ${lowerAggregate}Dto = new ${entityName}Dto()
        def created${aggregateName} = ${lowerAggregate}Functionalities.create${aggregateName}(${lowerAggregate}Dto)
        
        mockMvc.perform(MockMvcRequestBuilders.get("/api/${lowerAggregate}/" + created${aggregateName}.getId()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpected(MockMvcResultMatchers.jsonPath("$.id").value(created${aggregateName}.getId()))`,
            '',
            ''
        );

        const updateApiTest = this.buildSpockTest(
            `should update ${entityName} via REST API`,
            `        def ${lowerAggregate}Dto = new ${entityName}Dto()
        def created${aggregateName} = ${lowerAggregate}Functionalities.create${aggregateName}(${lowerAggregate}Dto)
        
        // Update the DTO
        created${aggregateName}.setName("Updated Name")
        def jsonContent = objectMapper.writeValueAsString(created${aggregateName})
        
        mockMvc.perform(MockMvcRequestBuilders.put("/api/${lowerAggregate}/" + created${aggregateName}.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpected(MockMvcResultMatchers.status().isOk())`,
            '',
            ''
        );

        const deleteApiTest = this.buildSpockTest(
            `should delete ${entityName} via REST API`,
            `        def ${lowerAggregate}Dto = new ${entityName}Dto()
        def created${aggregateName} = ${lowerAggregate}Functionalities.create${aggregateName}(${lowerAggregate}Dto)
        
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/${lowerAggregate}/" + created${aggregateName}.getId()))
                .andExpected(MockMvcResultMatchers.status().isNoContent())`,
            '',
            ''
        );

        const listApiTest = this.buildSpockTest(
            `should list all ${entityName}s via REST API`,
            `        // Create some test data
        def ${lowerAggregate}Dto1 = new ${entityName}Dto()
        def ${lowerAggregate}Dto2 = new ${entityName}Dto()
        ${lowerAggregate}Functionalities.create${aggregateName}(${lowerAggregate}Dto1)
        ${lowerAggregate}Functionalities.create${aggregateName}(${lowerAggregate}Dto2)
        
        mockMvc.perform(MockMvcRequestBuilders.get("/api/${lowerAggregate}"))
                .andExpected(MockMvcResultMatchers.status().isOk())
                .andExpected(MockMvcResultMatchers.jsonPath("$.length()").value(2))`,
            '',
            ''
        );

        return `${setupMethod}

${createApiTest}

${getApiTest}

${updateApiTest}

${deleteApiTest}

${listApiTest}`;
    }
}
