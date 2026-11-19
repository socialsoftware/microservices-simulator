package pt.ulisboa.tecnico.socialsoftware.ms.monitoring;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;

public class ReportService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String reportFile;
    private BufferedWriter writer;

    public ReportService(String reportFile) {
        this.reportFile = reportFile;
    }

    @PostConstruct
    public void setup() {
        if (reportFile == null || reportFile.trim().isEmpty()) {
            logger.error("Report-file for must be specified in ReportConfigurations");
            return;
        }

        initReport();
    }

    // ****************************
    // * --- Public Interface --- *
    // ****************************

    public synchronized void report(String content) {
        if (writer == null) {
            return;
        }

        try {
            writer.write(content);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            logger.error("Error writing to report: {}", e.getMessage());
        }
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
            logger.error("Error reading report: {}", e.getMessage());
            return "";
        }
    }

    public synchronized void cleanReport() {
        logger.info("Cleaning report file");
        initReport();
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
            report("### REPORT STARTED: " + new Date() + " ###");
        } catch (IOException e) {
            logger.error("Error initializing report: {}", e.getMessage());
        }
    }
}
