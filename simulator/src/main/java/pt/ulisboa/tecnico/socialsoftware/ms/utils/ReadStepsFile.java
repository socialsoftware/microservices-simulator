package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import java.io.*;
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
        InputStream inputStream = ReadStepsFile.class.getClassLoader().getResourceAsStream(directory+fileName);
        if (inputStream == null) {
            System.err.println(directory + fileName + " not found");
            return map;
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
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
