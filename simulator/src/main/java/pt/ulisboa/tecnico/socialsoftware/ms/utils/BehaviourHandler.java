package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class BehaviourHandler {
    private static BehaviourHandler instance;
    private static Map<String, Integer> funcCounter = new HashMap<>();
    private static String directory;
    private static Map<String, Integer> funcRetry = new HashMap<>();
    private static final String REPORT_FILE = "BehaviourReport.txt";
 

    public static synchronized BehaviourHandler getInstance() {
        if (instance == null) {
            instance = new BehaviourHandler();
        }
        return instance;
    }

    public static void setDirectory(String dir) {
        directory = dir;
    }

    
    public Map<String, List<Integer>> loadStepsFile(String funcName) {
        Map<String, List<Integer>> map = new LinkedHashMap<>();
        if (directory == null) {
            return map;
        }
        Path filePath = Paths.get(directory, funcName + ".csv");
        if (!Files.exists(filePath)) {
            return map;
        }
        int functionalityCounter = getFuncionalityCounter(funcName);
        try {
            List<String[]> block = parseCSVForBlock(filePath,funcName, functionalityCounter);
            if (!block.isEmpty()) {
                for (String[] row : block) {
                    String key = row[0];
                    List<Integer> values = Arrays.asList(
                        Integer.parseInt(row[1]),
                        Integer.parseInt(row[2]),
                        Integer.parseInt(row[3])
                        );
                        map.put(key, values);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }     
        return map;
    }
    
    private static synchronized int getFuncionalityCounter(String functionality) {
        return funcCounter.compute(functionality, (k, v) -> (v == null) ? 1 : v + 1);
    }

   
    
    public static List<String[]> parseCSVForBlock(Path filePath, String funcName, int targetBlock) throws IOException {
        List<String[]> currentBlock = new ArrayList<>();
        int blockNumber = 0;
    
        try (BufferedReader br = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.contains("retry"))
                    funcRetry.put(funcName, Integer.parseInt(line.split(",")[1]));
    
                if (line.equalsIgnoreCase("run")) {
                    blockNumber++;

                } else {
                    if (blockNumber == targetBlock && !line.isEmpty()) {
                        currentBlock.add(line.split(","));
                    }
        
                    if (blockNumber > targetBlock) {
                        break; 
                    }
                }
            }
        }
    
        return currentBlock;
    }
    
    
    public void appendToReport(String content) {
        
        if (directory == null) {
            return;
        }
        if (content == null || content.isEmpty()) {
            return;
        }
    
        Path filePath = Paths.get(directory, REPORT_FILE);
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, 
                java.nio.file.StandardOpenOption.CREATE, 
                java.nio.file.StandardOpenOption.APPEND)) {
            writer.write(content);
            writer.newLine(); 
        } catch (IOException e) {
            System.err.println("Failed to write to file: " + filePath);
            e.printStackTrace();
        }
    }
    


    public void cleanUpCounter() {
        funcCounter.clear();
        appendToReport("Test finished\n");

    }

    public void cleanReportFile() {
        if (directory == null) {
            System.out.println("Directory not set. Please set the directory first.");
            return;
        }
        Path filePath = Paths.get(directory, REPORT_FILE);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + filePath);
            e.printStackTrace();
        }
    }

    public int getRetryValue(String funcName) {
        System.out.println("\u001B[33mRetry value for " + funcName + ": " + funcRetry.getOrDefault(funcName, 0) + "\u001B[0m");
        return funcRetry.getOrDefault(funcName, 0);
    }
}
