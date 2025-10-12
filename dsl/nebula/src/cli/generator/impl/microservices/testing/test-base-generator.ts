import * as fs from 'fs';
import * as path from 'path';
import { OrchestrationBase } from "../../../base/orchestration-base.js";
import { TestGenerationOptions, TestContext } from "./test-types.js";
import { getGlobalConfig } from "../../../base/config.js";

export abstract class TestBaseGenerator extends OrchestrationBase {
    protected createTestContext(options: TestGenerationOptions, testPath: string): TestContext {
        const { aggregate, projectName, features = [] } = options;
        const capitalizedProjectName = this.capitalize(projectName);
        const aggregateName = aggregate.name;
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        const entityName = rootEntity?.name || aggregateName;

        return {
            aggregate,
            projectName,
            capitalizedProjectName,
            aggregateName,
            rootEntity,
            entityName,
            testPath,
            features
        };
    }

    protected async ensureTestDirectory(basePath: string, ...subPaths: string[]): Promise<string> {
        const fullPath = path.join(basePath, ...subPaths);
        await this.ensureDirectory(fullPath);
        return fullPath;
    }

    protected buildTestPackageName(projectName: string, ...subPackages: string[]): string {
        const basePackage = getGlobalConfig().buildPackageName(projectName);
        if (subPackages.length === 0) {
            return basePackage;
        }
        return `${basePackage}.${subPackages.join('.')}`;
    }

    protected buildTestImports(projectName: string, aggregateName: string, additionalImports: string[] = []): string[] {
        const baseImports = [
            'import org.springframework.beans.factory.annotation.Autowired',
            'import spock.lang.Specification',
            'import pt.ulisboa.tecnico.socialsoftware.SpockTest',
            `import ${getGlobalConfig().buildPackageName(projectName, 'coordination', 'functionalities')}.${aggregateName}Functionalities`,
            'import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler',
            'import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourService',
            '',
            'import java.time.LocalDateTime',
            ''
        ];

        return this.combineImports(baseImports, additionalImports);
    }

    protected async writeTestFile(filePath: string, content: string, description: string): Promise<void> {
        await this.ensureDirectory(path.dirname(filePath));
        await fs.promises.writeFile(filePath, content);
        console.log(`        - Generated ${description}`);
    }

    protected buildGroovyClass(packageName: string, className: string, imports: string[], classBody: string): string {
        const importsSection = imports.filter(imp => imp.trim() !== '').join('\n');

        return `package ${packageName}

${importsSection}

class ${className} extends SpockTest {
${classBody}
}
`;
    }

    protected buildSpockTest(methodName: string, testBody: string, setup: string = '', cleanup: string = ''): string {
        let test = `    def "${methodName}"() {`;

        if (setup.trim()) {
            test += `\n        setup:\n${setup}`;
        }

        test += `\n        when:\n${testBody}`;
        test += '\n        then:\n        noExceptionThrown()';

        if (cleanup.trim()) {
            test += `\n        cleanup:\n${cleanup}`;
        }

        test += '\n    }';

        return test;
    }

    protected override hasFeature(features: string[], feature: string): boolean {
        return features.includes(feature);
    }
}
