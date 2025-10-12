/**
 * Entity Generation Module
 * 
 * This module handles the generation of JPA entity classes for microservices.
 * It provides a clean facade for entity generation with modular components.
 */

// Main exports
export { EntityGenerator, generateEntityCode } from "./entity-orchestrator.js";
export type { EntityGenerationOptions, ImportRequirements } from "./types.js";

// Component generators (for advanced usage)
export { generateFields } from "./fields.js";
export { generateDefaultConstructor, generateEntityDtoConstructor, generateCopyConstructor } from "./constructors.js";
export { generateGettersSetters, generateBackReferenceGetterSetter } from "./methods.js";
export { generateInvariants } from "./invariants.js";
export { scanCodeForImports } from "./imports.js";
