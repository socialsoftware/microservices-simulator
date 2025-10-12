import { CodeGenerator } from "./engine/code-generator.js";

export { CodeGenerator } from "./engine/code-generator.js";
export type { TemplateGenerateOptions } from "./engine/types.js";

export const generateCode = CodeGenerator.generateCode;
