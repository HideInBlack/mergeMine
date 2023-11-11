package org.njupt.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyInformationCollector {

    //Those Diff marks are related to Merged file path !
    private static final String StartDiffMark = "<<<<<<< G:\\now\\2024merge\\";

    private static final String MidDiffMark1 = "||||||| G:\\now\\2024merge\\";

    private static final String MidDiffMark2 = "=======";

    private static final String EndDiffMark = ">>>>>>> G:\\now\\2024merge\\";

    /**
     * Extract Token Conflicts Block
     * @param filePath merged file
     * @return List of Token Conflicts Block
     * @throws IOException IO
     */
    public static List<String> extractTokenConflicts(String filePath) throws IOException {
        List<String> conflictBlocks = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder conflictBlock = new StringBuilder();
            String line; boolean key = false;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(StartDiffMark)){
                    conflictBlock = new StringBuilder();
                    conflictBlock.append("\n").append(line).append("\n");
                    key = true;
                    continue;
                }
                if (line.startsWith(EndDiffMark)){
                    conflictBlock.append("\n").append(line);
                    String block = conflictBlock.toString().replace("NewLineDZY", "\n");
                    conflictBlocks.add(block);
                    key = false;
                    continue;
                }
                if (key){
                    if (line.startsWith("=======") || line.startsWith("|||||||")) {
                        conflictBlock.append("\n").append(line).append("\n");
                    }else {
                        conflictBlock.append(line).append(" ");
                    }

                }
            }
        }
        return conflictBlocks;
    }

    /**
     * Extract Token-level Tuples (Key Information)
     * @param filePath merged file path
     * @return token-level tuples(Key Information)
     * @throws IOException IO
     */
    public static List<Map<String, String>> extractTokenTuples(String filePath) throws IOException {
        List<Map<String, String>> tokenTuples = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            Map<String, String> map = new HashMap<>();
            StringBuilder content = new StringBuilder();

            String line; boolean key = false;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(StartDiffMark)){
                    map = new HashMap<>();
                    content = new StringBuilder();
                    key = true;
                    continue;
                }
                if (line.startsWith(MidDiffMark1)){
                    map.put("a_tokens", content.toString().replace("NewLineDZY", "\n"));
                    content = new StringBuilder();
                    continue;
                }
                if (line.equals(MidDiffMark2)){
                    map.put("o_tokens", content.toString().replace("NewLineDZY", "\n"));
                    content = new StringBuilder();
                    continue;
                }
                if (line.startsWith(EndDiffMark)){ // Special
                    map.put("b_tokens", content.toString().replace("NewLineDZY", "\n"));
                    tokenTuples.add(map);
                    key = false;
                    continue;
                }
                if (key){
//                    content.append(line).append(" ");
                    //这一步是为了后续使用BM25方便
                    content.append("\"").append(line).append("\"").append(" ");
                }
            }
        }
        return tokenTuples;
    }

    public static void main(String[] args) throws IOException {
        System.out.println(extractTokenTuples("G:\\now\\2024merge\\ChatGPTResearch\\exampleData\\acceptA\\100004_4_merged.txt"));
        System.out.println("----------------------------------------------------------------------------------------------------------");
        System.out.println(extractTokenTuples("G:\\now\\2024merge\\ChatGPTResearch\\exampleData\\100004_3_merged.txt"));

//        List<String> conflictBlocks = extractTokenConflicts("G:\\now\\2024merge\\ChatGPTResearch\\exampleData\\100004_3_merged.txt");
//        System.out.println(conflictBlocks);

    }

}
