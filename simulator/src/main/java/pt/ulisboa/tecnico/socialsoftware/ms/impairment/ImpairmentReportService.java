package pt.ulisboa.tecnico.socialsoftware.ms.impairment;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class ImpairmentReportService {
    private final Logger logger = LoggerFactory.getLogger(ImpairmentReportService.class);
    private BufferedWriter writer;

    @Value("${simulator.impairment.report-file:#{null}}")
    private String reportFile;

    @PostConstruct
    public void init() {
        if (reportFile == null) {
            logger.error("Report-file for impairment must be specified in application.yaml");
            return;
        }

        initReport();
    }

    // ****************************
    // * --- Public Interface --- *
    // ****************************

    public void report(String content) {
        if (writer == null) {
            return;
        }

        try {
            writer.write(content);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            logger.error("Error writing to impairment report: {}", e.getMessage());
        }
    }

    public void logInfo(String msg) {
        logger.info(msg);
    }

    public void logError(String msg) {
        logger.error(msg);
        report(msg);
    }

    public String getReport() {
        if (reportFile == null) {
            return "";
        }

        Path reportPath = Paths.get(reportFile);
        if (!Files.exists(reportPath)) {
            return "";
        }

        try {
            return Files.readString(reportPath);
        } catch (IOException e) {
            logger.error("Error reading impairment report: {}", e.getMessage());
            return "";
        }
    }

    public void cleanReportFile() {
        if (reportFile == null) {
            return;
        }

        Path reportPath = Paths.get(reportFile);
        try {
            Files.writeString(reportPath, "", StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            logger.error("Error cleaning impairment report: {}", e.getMessage());
        }
    }

    // ******************
    // * --- Set Up --- *
    // ******************

    private void initReport() {
        if (reportFile == null) {
            return;
        }

        try {
            if (writer != null) {
                writer.close();
            }
            Path reportPath = Paths.get(reportFile);
            writer = new BufferedWriter(new FileWriter(reportPath.toFile(), false));
            report("### IMPAIRMENT REPORT STARTED: " + new Date() + " ###");
        } catch (IOException e) {
            logger.error("Error initializing impairment report: {}", e.getMessage());
        }
    }
}
