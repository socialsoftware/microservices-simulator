package pt.ulisboa.tecnico.socialsoftware.ms.monitoring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReportConfigurations {

    @Bean(name = "ImpairmentReportService")
    public ReportService networkReportService(
            @Value("${simulator.impairment.report-file:#{null}}") String reportFile) {
        return new ReportService(reportFile);
    }

    @Bean(name = "CapacityReportService")
    public ReportService databaseReportService(
            @Value("${simulator.capacity-management.report-file:#{null}}") String reportFile) {
        return new ReportService(reportFile);
    }
}