package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BehaviourGenerator {
    private String directory;

    public BehaviourGenerator(String dir, Path inputFile) {
        this.directory = dir;
        generateBalancedBehaviours(inputFile);
    }

    private void generateBalancedBehaviours(Path inputFile) {
        // Parse the input file by functionality
        Map<String, Map<String, List<List<String>>>> parsed = parseFunctionalitySteps(inputFile);

        // Generate combinations for each functionality
        Map<String, List<Map<String, List<String>>>> allCombinations = new LinkedHashMap<>();
        parsed.forEach((functionality, stepsMap) -> {
            List<Map<String, List<String>>> combinations = generateAllCombinations(stepsMap);
            allCombinations.put(functionality, combinations);
            System.out.println("Generated " + combinations.size() + " combinations for " + functionality);
        });

        // Balance all functionalities to have the same number of behaviors
        Map<String, List<Map<String, List<String>>>> balancedCombinations = balanceCombinations(allCombinations);

        // Write balanced CSV files
        writeCsvFiles(balancedCombinations);
    }

    private Map<String, List<Map<String, List<String>>>> balanceCombinations(
            Map<String, List<Map<String, List<String>>>> originalCombinations) {
        
        if (originalCombinations.size() <= 1) {
            return originalCombinations;
        }

        // Calculate target count as product of all combination counts
        long targetCount = 1;
        for (List<Map<String, List<String>>> combinations : originalCombinations.values()) {
            targetCount *= combinations.size();
        }

        System.out.println("Creating cross-product with " + targetCount + " behaviors per functionality");

        Map<String, List<Map<String, List<String>>>> balanced = new LinkedHashMap<>();
        
        // Convert to list for easier indexing
        List<String> functionalityNames = new ArrayList<>(originalCombinations.keySet());
        List<List<Map<String, List<String>>>> allCombinationsList = new ArrayList<>();
        
        for (String name : functionalityNames) {
            allCombinationsList.add(originalCombinations.get(name));
        }
        
        // Generate cross-product for each functionality
        for (int funcIndex = 0; funcIndex < functionalityNames.size(); funcIndex++) {
            String functionality = functionalityNames.get(funcIndex);
            List<Map<String, List<String>>> balancedBehaviors = new ArrayList<>();
            
            // Generate all combinations where this functionality cycles through all others
            generateCrossProductForFunctionality(allCombinationsList, funcIndex, 
                                               new int[functionalityNames.size()], 
                                               balancedBehaviors);
            
            balanced.put(functionality, balancedBehaviors);
            System.out.printf("%s: %d behaviors (cross-product)%n", 
                            functionality, balancedBehaviors.size());
        }

        return balanced;
    }
    
    private void generateCrossProductForFunctionality(List<List<Map<String, List<String>>>> allCombinations,
                                                     int targetFuncIndex,
                                                     int[] indices,
                                                     List<Map<String, List<String>>> result) {
        
        if (targetFuncIndex == allCombinations.size()) {
            return; // Invalid index
        }
        
        // Generate all combinations where other functionalities cycle through their combinations
        generateCrossProductRecursive(allCombinations, targetFuncIndex, indices, 0, result);
    }
    
    private void generateCrossProductRecursive(List<List<Map<String, List<String>>>> allCombinations,
                                              int targetFuncIndex,
                                              int[] indices,
                                              int currentFunc,
                                              List<Map<String, List<String>>> result) {
        
        if (currentFunc == allCombinations.size()) {
            // We've set indices for all functionalities, add the target functionality's combination
            result.add(allCombinations.get(targetFuncIndex).get(indices[targetFuncIndex]));
            return;
        }
        
        if (currentFunc == targetFuncIndex) {
            // For the target functionality, iterate through all its combinations
            for (int i = 0; i < allCombinations.get(currentFunc).size(); i++) {
                indices[currentFunc] = i;
                generateCrossProductRecursive(allCombinations, targetFuncIndex, indices, currentFunc + 1, result);
            }
        } else {
            // For other functionalities, iterate through all their combinations
            for (int i = 0; i < allCombinations.get(currentFunc).size(); i++) {
                indices[currentFunc] = i;
                generateCrossProductRecursive(allCombinations, targetFuncIndex, indices, currentFunc + 1, result);
            }
        }
    }

    private void writeCsvFiles(Map<String, List<Map<String, List<String>>>> allCombinations) {
        allCombinations.forEach((functionality, combinations) -> {
            Path file = Paths.get(directory + functionality + ".csv");
            
            try (BufferedWriter writer = Files.newBufferedWriter(file)) {
                for (Map<String, List<String>> combo : combinations) {
                    writer.write("run ");
                    writer.newLine();

                    for (Map.Entry<String, List<String>> entry : combo.entrySet()) {
                        String stepName = entry.getKey();
                        List<String> values = entry.getValue();
                        
                        if (values.size() != 3) {
                            throw new IllegalStateException("Each step must have exactly 3 values (fault, delayBefore, delayAfter)");
                        }
                        
                        writer.write(String.format("%s,%s,%s,%s", 
                                   stepName, values.get(0), values.get(1), values.get(2)));
                        writer.newLine();
                    }
                }

                System.out.println("Created: " + file.toAbsolutePath());

            } catch (IOException e) {
                System.err.println("Failed to write CSV for " + functionality + ": " + e.getMessage());
            }
        });
    }

    private List<Map<String, List<String>>> generateAllCombinations(Map<String, List<List<String>>> stepOptions) {
        List<String> stepNames = new ArrayList<>(stepOptions.keySet());
        List<List<List<String>>> stepCombinations = new ArrayList<>();

        for (String step : stepNames) {
            List<List<String>> optionGroups = stepOptions.get(step);
            List<List<String>> combinationsForStep = cartesianProduct(optionGroups);
            stepCombinations.add(combinationsForStep);
        }

        List<Map<String, List<String>>> result = new ArrayList<>();
        generateStepProduct(stepCombinations, stepNames, 0, new HashMap<>(), result);
        return result;
    }

    private List<List<String>> cartesianProduct(List<List<String>> optionGroups) {
        List<List<String>> result = new ArrayList<>();
        cartesianProductHelper(optionGroups, 0, new ArrayList<>(), result);
        return result;
    }

    private void cartesianProductHelper(List<List<String>> optionGroups, int index, 
                                       List<String> current, List<List<String>> result) {
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

    private void generateStepProduct(List<List<List<String>>> stepCombinations, List<String> stepNames, 
                                   int index, Map<String, List<String>> current, 
                                   List<Map<String, List<String>>> result) {
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

    private Map<String, Map<String, List<List<String>>>> parseFunctionalitySteps(Path filePath) {
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