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

        int blockNumber = 1; // Track block numbers

        try (BufferedReader br = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) { 
                    if (!currentBlock.isEmpty()) {
                        if (blockNumber == targetBlock) {
                            return new ArrayList<>(currentBlock);
                        }
                        currentBlock.clear();
                        blockNumber++;
                    }
                } else {
                    currentBlock.add(line.split(","));
                }
            }
            if (!currentBlock.isEmpty() && blockNumber == targetBlock) {
                return currentBlock;
            }
        }
        return new ArrayList<>(); // Return an empty list if the block is not found
    }

    public void cleanUp() {
        funcCounter.clear();
    }
}
