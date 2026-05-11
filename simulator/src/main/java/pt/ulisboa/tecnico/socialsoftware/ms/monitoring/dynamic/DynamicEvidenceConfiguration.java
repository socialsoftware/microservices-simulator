package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(DynamicEvidenceProperties.class)
public class DynamicEvidenceConfiguration {
    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean
    public DynamicEvidenceRecorder dynamicEvidenceRecorder(DynamicEvidenceProperties properties, ObjectMapper objectMapper) {
        if (!properties.isEnabled()) {
            return new DynamicEvidenceNoopRecorder();
        }

        DynamicEvidenceRecorder currentRecorder = DynamicEvidenceRecorderHolder.getRecorder();
        if (currentRecorder instanceof DynamicEvidenceJsonlRecorder jsonlRecorder
                && jsonlRecorder.hasSameConfiguration(properties)) {
            return currentRecorder;
        }

        return new DynamicEvidenceJsonlRecorder(properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicEvidenceRecorderRegistration dynamicEvidenceRecorderRegistration(DynamicEvidenceRecorder recorder,
                                                                                   DynamicEvidenceProperties properties) {
        boolean listenerOwnedRecorder = recorder instanceof DynamicEvidenceJsonlRecorder jsonlRecorder
                && DynamicEvidenceRecorderHolder.getRecorder() == recorder
                && jsonlRecorder.hasSameConfiguration(properties);
        // Listener-owned recorders stay open across sequential Spring contexts; only Spring-owned recorders close here.
        return new DynamicEvidenceRecorderRegistration(recorder, !listenerOwnedRecorder);
    }
}
