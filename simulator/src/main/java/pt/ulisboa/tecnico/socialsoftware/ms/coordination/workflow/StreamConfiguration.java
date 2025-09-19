//package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.messaging.converter.MappingJackson2MessageConverter;
//
//@Configuration
//public class StreamConfiguration {
//
//    @Bean
//    public ObjectMapper objectMapper() {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
//        mapper.findAndRegisterModules();
//        return mapper;
//    }
//
//    @Bean
//    public MappingJackson2MessageConverter mappingJackson2MessageConverter(ObjectMapper objectMapper) {
//        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
//        converter.setObjectMapper(objectMapper);
//        return converter;
//    }
//}
