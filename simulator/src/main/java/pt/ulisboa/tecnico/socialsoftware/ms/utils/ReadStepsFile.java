package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ReadStepsFile {
    private static ReadStepsFile instance;
    private static Map<String, Integer> funcCounter;
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
    
    public Map<String, List<Integer>> loadStepsFile(String fileName) {
        Map<String, List<Integer>> map = new LinkedHashMap<>();
        Path filePath = Paths.get(directory, fileName);
        
        if (!Files.exists(filePath)) {
            System.out.println("File not found: " + filePath);
            return map;
        }
        
        // Read file
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            System.out.println("Reading file: " + filePath);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    String key = parts[0];
                    List<Integer> values = Arrays.asList(
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[3])
                    );
                    map.put(key, values);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }
}
