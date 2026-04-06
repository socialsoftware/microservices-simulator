import { type Module, inject } from "langium";
import {
  createDefaultModule,
  createDefaultSharedModule,
  type DefaultSharedModuleContext,
  type LangiumServices,
  type LangiumSharedServices,
  type PartialLangiumServices,
} from "langium/lsp";
import {
  NebulaGeneratedModule,
  NebulaGeneratedSharedModule,
} from "./generated/module.js";
import {
  NebulaValidator,
  registerValidationChecks,
} from "./validation/index.js";
import { NebulaScopeComputation } from './nebula-scope-computation.js';
import { NebulaScopeProvider } from '../cli/engine/nebula-scope-provider.js';


export type NebulaAddedServices = {
  validation: {
    NebulaValidator: NebulaValidator;
  };
};


export type NebulaServices = LangiumServices & NebulaAddedServices;


export const NebulaModule: Module<
  NebulaServices,
  PartialLangiumServices & NebulaAddedServices
> = {
  validation: {
    NebulaValidator: (services) => new NebulaValidator(services as NebulaServices),
  },
  references: {
    ScopeComputation: (services) => new NebulaScopeComputation(services),
    ScopeProvider: (services) => new NebulaScopeProvider(services),
  },
};


export function createNebulaServices(context: DefaultSharedModuleContext): {
  shared: LangiumSharedServices;
  nebulaServices: NebulaServices;
} {
  const shared = inject(
    createDefaultSharedModule(context),
    NebulaGeneratedSharedModule
  );
  const Nebula = inject(
    createDefaultModule({ shared }),
    NebulaGeneratedModule,
    NebulaModule
  );
  shared.ServiceRegistry.register(Nebula);
  registerValidationChecks(Nebula);

  return { shared, nebulaServices: Nebula };
}
