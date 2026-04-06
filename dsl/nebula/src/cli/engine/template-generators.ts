import { getGlobalConfig } from "../generators/common/config.js";
import { OrchestrationBase } from "../generators/common/orchestration-base.js";

export class TemplateGenerators extends OrchestrationBase {
  static generateAggregateBaseClass(
    aggregateName: string,
    rootEntityName: string,
    projectName: string,
    aggregate?: any
  ): string {
    const generator = new TemplateGenerators();
    const config = getGlobalConfig();
    const packageName = config.buildPackageName(projectName, 'microservices', aggregateName.toLowerCase(), 'aggregate');

    
    
    const template = generator.loadTemplate('entity/aggregate-base.hbs');
    return generator.renderTemplate(template, {
      packageName,
      aggregateName,
      rootEntityName,
      imports: [
        'import jakarta.persistence.Entity;',
        `import ${generator.getBasePackage()}.ms.domain.aggregate.Aggregate;`,
        `import ${generator.getBasePackage()}.ms.domain.event.EventSubscription;`,
        'import java.util.Set;',
        'import java.util.HashSet;'
      ],
      fields: [], 
      hasInvariants: false, 
      invariantComments: [],
      invariantChecks: [],
      hasDtoConstructor: false, 
      hasCopyConstructor: true,
      dtoType: `${aggregateName}Dto`,
      dtoParamName: `${aggregateName.toLowerCase()}Dto`,
      hasEntityParams: false,
      entityParams: '',
      dtoAssignments: [],
      entityAssignments: [],
      copyAssignments: [],
      defaultFieldValues: [],
      gettersSetters: []
    });
  }

  static generatePomXml(projectName: string): string {
    const generator = new TemplateGenerators();
    const config = getGlobalConfig();
    const fullConfig = config.getConfig();

    const template = generator.loadTemplate('config/pom.hbs');
    return generator.renderTemplate(template, {
      projectName,
      groupId: config.getBasePackage(),
      version: fullConfig.version,
      javaVersion: fullConfig.javaVersion,
      springBootVersion: fullConfig.springBootVersion,
      frameworkGroupId: fullConfig.simulatorFramework.groupId,
      frameworkArtifactId: fullConfig.simulatorFramework.artifactId,
      frameworkVersion: fullConfig.simulatorFramework.version,
      includeRetry: true 
    });
  }

  static generateGitignore(): string {
    const generator = new TemplateGenerators();
    const template = generator.loadTemplate('config/gitignore.hbs');
    return generator.renderTemplate(template, {});
  }
}
