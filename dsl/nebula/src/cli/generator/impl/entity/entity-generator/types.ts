import { Entity } from "../../../../../language/generated/ast.js";

export type ImportRequirements = {
    usesPersistence?: boolean;
    usesLocalDateTime?: boolean;
    usesBigDecimal?: boolean;
    usesSet?: boolean;
    usesList?: boolean;
    usesAggregate?: boolean;
    usesStreams?: boolean;
    usesUserDto?: boolean;
    usesOneToOne?: boolean;
    usesOneToMany?: boolean;
    usesCascadeType?: boolean;
    usesFetchType?: boolean;
    usesDateHandler?: boolean;
    usesCollectors?: boolean;
    usesGeneratedValue?: boolean;
    usesEnumerated?: boolean;
    usesAggregateState?: boolean;
    customImports?: Set<string>;
};

export type EntityGenerationOptions = {
    projectName: string;
    allSharedDtos?: any[];
    dtoMappings?: any[];
    allEntities?: Entity[];
};