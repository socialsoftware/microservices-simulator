package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Provides an ObjectMapper configured for stream messaging:
 * - polymorphic typing for domain objects under pt.ulisboa.tecnico
 * - skips containers, arrays, maps, primitives and enums
 * - registers modules and ignores unknown properties
 */
@Component
@Profile("stream")
public class MessagingObjectMapperProvider {
    private final ObjectMapper base;

    @Autowired
    public MessagingObjectMapperProvider(ObjectMapper objectMapper) {
        this.base = objectMapper;
    }

    public ObjectMapper newMapper() {
        ObjectMapper mapper = base.copy();
        mapper.findAndRegisterModules();

        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("pt.ulisboa.tecnico")
                .build();

        TypeResolverBuilder<?> typer = new ObjectMapper.DefaultTypeResolverBuilder(
                ObjectMapper.DefaultTyping.NON_FINAL, ptv) {
            @Override
            public boolean useForType(JavaType t) {
                if (t.isPrimitive() || t.isEnumType()) return false;
                if (t.isArrayType() || t.isCollectionLikeType() || t.isMapLikeType() || t.isContainerType())
                    return false;
                Class<?> raw = t.getRawClass();
                Package p = raw.getPackage();
                String pkg = (p == null) ? "" : p.getName();
                return pkg.startsWith("pt.ulisboa.tecnico");
            }
        };
        typer = typer.init(JsonTypeInfo.Id.CLASS, null).inclusion(JsonTypeInfo.As.PROPERTY);
        mapper.setDefaultTyping(typer);

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}