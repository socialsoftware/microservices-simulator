package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourHandler;

public class BehaviourGenerator {
    private static String directory;

    public BehaviourGenerator(String dir, Path inputFile) {
        directory = dir;
        // Parse the input file by functionality
        Map<String, Map<String, List<List<String>>>> parsed = parseFunctionalitySteps(inputFile);

        // Prepare a map to store step combinations per functionality
        Map<String, List<Map<String, List<String>>>> allFunctionalityCombinations = new LinkedHashMap<>();

        // Generate combinations for each functionality
        parsed.forEach((functionality, stepsMap) -> {
            List<Map<String, List<String>>> combinations = generateAllCombinations(stepsMap);
            allFunctionalityCombinations.put(functionality, combinations);
        });

        System.out.println("Parsed functionality map: " + allFunctionalityCombinations);
    }

    public static void writeCombinationsToCsvFiles(Map<String, List<Map<String, List<String>>>> allFunctionalityCombinations) {
        allFunctionalityCombinations.forEach((functionality, combinations) -> {
            Path file = Paths.get(directory + functionality + "_combinations.csv");

            try (BufferedWriter writer = Files.newBufferedWriter(file)) {

                for (Map<String, List<String>> combo : combinations) {
                    writer.write("run ");
                    writer.newLine();

                    for (Map.Entry<String, List<String>> entry : combo.entrySet()) {
                        String stepName = entry.getKey();
                        List<String> values = entry.getValue(); // should be exactly 3 values
                        if (values.size() != 3) {
                            throw new IllegalStateException("Each step must have exactly 3 values (fault, delayBefore, delayAfter)");
                        }
                        writer.write(String.format("%s,%s,%s,%s", stepName, values.get(0), values.get(1), values.get(2)));
                        writer.newLine();
                    }
                }

                System.out.println("Wrote file: " + file.toAbsolutePath());

            } catch (IOException e) {
                System.err.println("Failed to write CSV for " + functionality + ": " + e.getMessage());
            }
        });
    }


    private static List<Map<String, List<String>>> generateAllCombinations(Map<String, List<List<String>>> stepOptions) {
        // Convert the map entries into a list for indexable access
        List<String> stepNames = new ArrayList<>(stepOptions.keySet());

        // For each step, compute the list of combinations for that step
        List<List<List<String>>> stepCombinations = new ArrayList<>();

        for (String step : stepNames) {
            List<List<String>> optionGroups = stepOptions.get(step);
            List<List<String>> combinationsForStep = cartesianProduct(optionGroups);
            stepCombinations.add(combinationsForStep);
        }

        // Compute the cartesian product across steps
        List<Map<String, List<String>>> result = new ArrayList<>();
        generateStepProduct(stepCombinations, stepNames, 0, new HashMap<>(), result);
        return result;
    }

    private static List<List<String>> cartesianProduct(List<List<String>> optionGroups) {
        List<List<String>> result = new ArrayList<>();
        cartesianProductHelper(optionGroups, 0, new ArrayList<>(), result);
        return result;
    }

    private static void cartesianProductHelper(List<List<String>> optionGroups, int index, List<String> current, List<List<String>> result) {
        if (index == optionGroups.size()) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (String option : optionGroups.get(index)) {
            current.add(option);
            cartesianProductHelper(optionGroups, index + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    private static void generateStepProduct(List<List<List<String>>> stepCombinations, List<String> stepNames, int index,
                                            Map<String, List<String>> current, List<Map<String, List<String>>> result) {
        if (index == stepCombinations.size()) {
            result.add(new LinkedHashMap<>(current));
            return;
        }

        String stepName = stepNames.get(index);
        for (List<String> combo : stepCombinations.get(index)) {
            current.put(stepName, combo);
            generateStepProduct(stepCombinations, stepNames, index + 1, current, result);
            current.remove(stepName);
        }
    }

    private static Map<String, Map<String, List<List<String>>>> parseFunctionalitySteps(Path filePath) {
        Map<String, Map<String, List<List<String>>>> functionalityMap = new LinkedHashMap<>();
        Pattern listPattern = Pattern.compile("\\[(.*?)\\]");

        try (BufferedReader br = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", 3);
                if (parts.length < 3) continue;

                String functionality = parts[0].trim();
                String stepName = parts[1].trim();

                Matcher matcher = listPattern.matcher(line);
                List<List<String>> options = new ArrayList<>();
                while (matcher.find()) {
                    String row = matcher.group(1);
                    List<String> values = Arrays.stream(row.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();
                    options.add(values);
                }

                functionalityMap
                        .computeIfAbsent(functionality, k -> new LinkedHashMap<>())
                        .put(stepName, options);
            }
        } catch (IOException e) {
            throw new SimulatorException("Error reading file: " + e.getMessage());
        }

        return functionalityMap;
    }
    
}
