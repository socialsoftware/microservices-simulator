package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ReadStepsFile {
    private static ReadStepsFile instance;
    private static Map<String, Integer> funcCounter = new HashMap<>();
    private static String directory;
    private static String ReportFile = "BehaviourReport.txt"; 


    public static synchronized ReadStepsFile getInstance() {
        if (instance == null) {
            instance = new ReadStepsFile();
        }
        return instance;
    }

    public static void setDirectory(String dir) {
        directory = dir;
    }

    
    public Map<String, List<Integer>> loadStepsFile(String funcName) {
        Map<String, List<Integer>> map = new LinkedHashMap<>();
        if (directory == null) {
            System.out.println("Directory not set. Please set the directory first. Directory: " + directory);
            return map;
        }
        Path filePath = Paths.get(directory, funcName + ".csv");
        if (!Files.exists(filePath)) {
            System.out.println("File not found: " + filePath);
            return map;
        }
        int functionalityCounter = getFuncionalityCounter(funcName);
        System.out.println("Functionality " + funcName + " has been called " + functionalityCounter + " times.");
        try {
            List<String[]> block = parseCSVForBlock(filePath, functionalityCounter);
            if (block.isEmpty()) {
                System.out.println("Block " + functionalityCounter + " not found.");
            } else {
                System.out.println("Selected Block " + functionalityCounter + " for functionality " + funcName + ":");
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
    
    private synchronized int getFuncionalityCounter(String functionality) {
        return funcCounter.compute(functionality, (k, v) -> (v == null) ? 1 : v + 1);
    }

    public static List<String[]> parseCSVForBlock(Path filePath, int targetBlock) throws IOException {
        List<String[]> currentBlock = new ArrayList<>();
        int blockNumber = 0;
    
        try (BufferedReader br = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
    
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
            System.out.println("Directory not set. Please set the directory first.");
            return;
        }
        if(content == null || content.isEmpty()) {
            System.out.println("Content is null or empty. Not appending to report.");
            return;
        }
    
        Path filePath = Paths.get(directory, ReportFile);
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
    


    public void cleanUp() {
        funcCounter.clear();
        appendToReport("Test finished\n");

    }

    public void cleanReportFile() {
        if (directory == null) {
            System.out.println("Directory not set. Please set the directory first.");
            return;
        }
        Path filePath = Paths.get(directory, ReportFile);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + filePath);
            e.printStackTrace();
        }
    }
}
