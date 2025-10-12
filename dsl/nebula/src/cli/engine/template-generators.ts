import { getGlobalConfig } from "../generators/shared/config.js";

export class TemplateGenerators {
  static generateAggregateBaseClass(
    aggregateName: string,
    rootEntityName: string,
    projectName: string
  ): string {
    const packageName = `${getGlobalConfig().buildPackageName(projectName, 'microservices', aggregateName.toLowerCase(), 'aggregate')}`;

    return `package ${packageName};

import jakarta.persistence.MappedSuperclass;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import java.util.Set;
import java.util.HashSet;

@MappedSuperclass
public abstract class ${aggregateName} extends Aggregate {
    // Abstract base class for ${aggregateName} aggregate
    // Concrete implementation is ${rootEntityName}
    
    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }
    
    @Override
    public void verifyInvariants() {
        // Invariant verification implementation
        // Override in concrete classes if needed
    }
}`;
  }

  static generatePomXml(projectName: string, architecture: string, features: string[]): string {
    const deps: string[] = [];
    deps.push(`<!-- MicroservicesSimulator Framework Dependency -->`);
    deps.push(`<!-- To install locally: cd ../../simulator && mvn clean install -DskipTests -->`);
    deps.push(`<dependency>\n      <groupId>pt.ulisboa.tecnico.socialsoftware</groupId>\n      <artifactId>MicroservicesSimulator</artifactId>\n      <version>2.1.0-SNAPSHOT</version>\n    </dependency>`);
    deps.push(`<dependency>\n      <groupId>org.springframework.boot</groupId>\n      <artifactId>spring-boot-starter-web</artifactId>\n    </dependency>`);
    deps.push(`<dependency>\n      <groupId>org.springframework.boot</groupId>\n      <artifactId>spring-boot-starter-data-jpa</artifactId>\n    </dependency>`);
    deps.push(`<dependency>\n      <groupId>org.springframework.boot</groupId>\n      <artifactId>spring-boot-starter-validation</artifactId>\n    </dependency>`);
    deps.push(`<dependency>\n      <groupId>org.springframework.boot</groupId>\n      <artifactId>spring-boot-starter-actuator</artifactId>\n    </dependency>`);
    deps.push(`<dependency>\n      <groupId>org.springframework.boot</groupId>\n      <artifactId>spring-boot-starter-test</artifactId>\n      <scope>test</scope>\n    </dependency>`);
    deps.push(`<dependency>\n      <groupId>org.hibernate.orm</groupId>\n      <artifactId>hibernate-core</artifactId>\n    </dependency>`);
    deps.push(`<dependency>\n      <groupId>com.h2database</groupId>\n      <artifactId>h2</artifactId>\n      <scope>runtime</scope>\n    </dependency>`);
    deps.push(`<dependency>\n      <groupId>org.postgresql</groupId>\n      <artifactId>postgresql</artifactId>\n      <scope>runtime</scope>\n    </dependency>`);

    if (features.includes('retry')) {
      deps.push(`<dependency>\n      <groupId>org.springframework.retry</groupId>\n      <artifactId>spring-retry</artifactId>\n    </dependency>`);
    }

    return `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.9</version>
    <relativePath/>
  </parent>
  <groupId>pt.ulisboa.tecnico.socialsoftware</groupId>
  <artifactId>${projectName}</artifactId>
  <version>2.1.0-SNAPSHOT</version>
  <name>${projectName}</name>
  <description>${projectName} - Generated with Nebula DSL (${architecture} architecture)</description>
  <properties>
    <java.version>21</java.version>
  </properties>
  <dependencies>
${deps.join('\n')}
  </dependencies>
  <profiles>
    <profile>
      <id>dev</id>
      <properties>
        <activatedProperties>dev</activatedProperties>
      </properties>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
    </profile>
    <profile>
      <id>sagas</id>
      <properties>
        <activatedProperties>sagas</activatedProperties>
      </properties>
    </profile>
    <profile>
      <id>dev-sagas</id>
      <properties>
        <activatedProperties>dev,sagas</activatedProperties>
      </properties>
    </profile>
    <profile>
      <id>test-sagas</id>
      <properties>
        <activatedProperties>test,sagas</activatedProperties>
      </properties>
      <build>
        <plugins>
          <plugin>
            <groupId>org.codehaus.gmavenplus</groupId>
            <artifactId>gmavenplus-plugin</artifactId>
            <version>4.2.0</version>
            <executions>
              <execution>
                <goals>
                  <goal>addTestSources</goal>
                  <goal>compileTests</goal>
                </goals>
              </execution>
            </executions>
          </plugin>
        </plugins>
      </build>
    </profile>
    <profile>
      <id>test</id>
      <properties>
        <activatedProperties>test</activatedProperties>
      </properties>
      <build>
        <plugins>
          <plugin>
            <groupId>org.codehaus.gmavenplus</groupId>
            <artifactId>gmavenplus-plugin</artifactId>
            <version>4.2.0</version>
            <executions>
              <execution>
                <goals>
                  <goal>addTestSources</goal>
                  <goal>compileTests</goal>
                </goals>
              </execution>
            </executions>
          </plugin>
        </plugins>
      </build>
    </profile>
  </profiles>
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>`;
  }

  static generateGitignore(): string {
    return `# Build output
target/
build/

# Logs
logs/
*.log

# IDE
.idea/
.vscode/
*.iml
*.iws
*.ipr
.project
.classpath
.settings/
.factorypath

# OS
.DS_Store
Thumbs.db

# Application specific
HELP.md
**/BehaviourReport.txt

# Environment files
src/main/resources/application-dev.properties
src/main/resources/application-prod.properties
`;
  }
}
