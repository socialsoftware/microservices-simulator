package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessagingObjectMapperProvider {
    private final ObjectMapper base;

    @Autowired
    public MessagingObjectMapperProvider(ObjectMapper objectMapper) {
        this.base = objectMapper;
    }

    public ObjectMapper newMapper() {
        ObjectMapper mapper = base.copy();
        mapper.findAndRegisterModules();

        mapper.registerModule(new JavaTimeModule()); // for LocalDateTime etc.
        Hibernate6Module hbm = new Hibernate6Module();
        hbm.enable(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS);
        mapper.registerModule(hbm);

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

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
                if (raw.getSimpleName().endsWith("Dto")) {
                    return false;
                }
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