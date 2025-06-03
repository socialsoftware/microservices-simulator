import type { Model } from "../language/generated/ast.js";
// import chalk from "chalk";
import { Command } from "commander";
import { NebulaLanguageMetaData } from "../language/generated/module.js";
import { createNebulaServices } from "../language/nebula-module.js";
import { extractAstNode } from "./cli-util.js";
import { collectNebulaFiles } from "./file-utils.js";
import { generate } from "./generator/index.js";
import { NodeFileSystem } from "langium/node";
import * as url from "node:url";
import * as fs from "node:fs/promises";
import * as path from "node:path";

const DEFAULT_OUTPUT_DIR = "../applications";
const DEFAULT_PROJECT_NAME = "project";
// const DEFAULT_JAVA_PATH = "../applications/project/src/main/java/pt/ulisboa/tecnico/socialsoftware/project";


const __dirname = url.fileURLToPath(new URL(".", import.meta.url));
const packagePath = path.resolve(__dirname, "..", "..", "package.json");
const packageContent = await fs.readFile(packagePath, "utf-8");


function extractAggregateNames(models: Model[]): string[] {
  // Extract all aggregate names from all models
  const aggregateNames = new Set<string>();

  for (const model of models) {
    for (const aggregate of model.aggregates) {
      aggregateNames.add(aggregate.name.toLowerCase());
    }
  }

  return Array.from(aggregateNames);
}

function generatePomXml(aggregateNames: string[], projectName: string): string {
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
    <description>${projectName}</description>
    <properties>
        <java.version>21</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>pt.ulisboa.tecnico.socialsoftware</groupId>
            <artifactId>MicroservicesSimulator</artifactId>
            <version>2.1.0-SNAPSHOT</version>
        </dependency>
        <!-- Automatically generated dependencies for aggregates: ${aggregateNames.join(", ")} -->
    </dependencies>
</project>`;
}

/**
 * Creates the base structure of the project
 * @param basePath The base path of the project
 * @param aggregateNames Names of all aggregates to create directories for
 */
async function createProjectStructure(basePath: string, aggregateNames: string[]): Promise<void> {
  // Define standard directories that should exist regardless of aggregate structure
  const standardDirs = ['causal', 'coordination', 'microservices', 'sagas'];

  try {
    await fs.mkdir(basePath, { recursive: true });

    for (const dir of standardDirs) {
      await fs.mkdir(path.join(basePath, dir), { recursive: true });
    }

    // Create aggregate-specific directories under microservices
    for (const aggregate of aggregateNames) {
      const aggregatePath = path.join(basePath, 'microservices', aggregate);
      await fs.mkdir(aggregatePath, { recursive: true });

      // Create subdirectories for each aggregate
      const subDirs = ['aggregate', 'events', 'service'];
      for (const subDir of subDirs) {
        await fs.mkdir(path.join(aggregatePath, subDir), { recursive: true });
      }

    }
  } catch (error) {
    console.error(`Error creating project structure: ${error}`);
    throw error;
  }
}

export type GenerateOptions = {
  destination?: string;
  name?: string;
};

export const generateAction = async (
  inputPath: string,
  opts: GenerateOptions
): Promise<void> => {
  try {
    const nebulaFiles = await collectNebulaFiles(inputPath);

    if (nebulaFiles.length === 0) {
      console.error(`No Nebula files found at path: ${inputPath}`);
      process.exit(1);
    }

    const baseOutputDir = opts.destination || DEFAULT_OUTPUT_DIR;
    const projectName = opts.name || DEFAULT_PROJECT_NAME;
    const projectPath = path.join(baseOutputDir, projectName);
    // Construct the standard Java source path within the project
    const javaSrcPath = path.join('src', 'main', 'java');
    const javaPackagePath = path.join('pt', 'ulisboa', 'tecnico', 'socialsoftware', 'project', projectName.toLowerCase());
    const javaPath = path.join(projectPath, javaSrcPath, javaPackagePath);

    // Clean previous output if exists
    try {
      await fs.access(projectPath);
      await fs.rm(projectPath, { recursive: true, force: true });
    } catch (accessErr) {
      // Directory did not exist – nothing to clean
    }

    // Ensure the base project directory exists before creating sub-structure or writing pom.xml
    await fs.mkdir(projectPath, { recursive: true });

    const services = createNebulaServices(NodeFileSystem).nebulaServices;

    // First pass: parse models to collect aggregate names
    const allModels: Model[] = [];
    for (const filePath of nebulaFiles) {
      const model = await extractAstNode<Model>(filePath, services);
      allModels.push(model);
    }

    const aggregateNames = extractAggregateNames(allModels);

    await createProjectStructure(javaPath, aggregateNames);

    const pomContent = generatePomXml(aggregateNames, projectName);
    await fs.writeFile(path.join(projectPath, "pom.xml"), pomContent, "utf-8");

    // Second pass: generate code for each model
    for (const filePath of nebulaFiles) {
      const model = await extractAstNode<Model>(filePath, services);
      generate(path.join(javaPath, "microservices"), model, projectName);
    }
  } catch (error) {
    console.error(`Error processing path ${inputPath}: ${error}`);
    process.exit(1);
  }
};

export default function (): void {
  const program = new Command();

  program.version(JSON.parse(packageContent).version);

  const fileExtensions = NebulaLanguageMetaData.fileExtensions.join(", ");
  program
    .command("generate")
    .argument(
      "<path>",
      `source file or directory (possible file extensions: ${fileExtensions})`
    )
    .option("-d, --destination <dir>", "destination directory for generating code")
    .option("-n, --name <name>", "name of the project (default: 'quizzes')")
    .description(
      'generates Java code for each Nebula file in the provided directory or for a single file'
    )
    .action(generateAction);

  program.parse(process.argv);
}
