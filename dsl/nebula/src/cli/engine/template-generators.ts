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

    // For now, provide a basic template context
    // Note: This generates a basic aggregate base class template
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
      fields: [], // Fields are handled by entity generation
      hasInvariants: false, // Invariants are handled by validation generation
      invariantComments: [],
      invariantChecks: [],
      hasDtoConstructor: false, // DTO constructors are handled by DTO generation
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

    const template = generator.loadTemplate('config/pom.hbs');
    return generator.renderTemplate(template, {
      projectName,
      groupId: config.getBasePackage(),
      javaVersion: '21',
      includeRetry: true // Always include retry dependency
    });
  }

  static generateGitignore(): string {
    const generator = new TemplateGenerators();
    const template = generator.loadTemplate('config/gitignore.hbs');
    return generator.renderTemplate(template, {});
  }
}
