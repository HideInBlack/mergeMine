package org.njupt.core;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.njupt.util.DzyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class DatasetCollector {

    public static final Logger logger = LoggerFactory.getLogger(DatasetCollector.class);

    public static final String JavaDirectory = "G:/now/2024merge/MergeBERT_Data/fse2022/automated-analysis-data/Java/";

    public static final String CSharpDirectory = "G:/now/2024merge/MergeBERT_Data/fse2022/automated-analysis-data/CSharp/";

    public static final String JavaScriptDirectory = "G:/now/2024merge/MergeBERT_Data/fse2022/automated-analysis-data/JavaScript/";

    public static final String TypeScriptDirectory = "G:/now/2024merge/MergeBERT_Data/fse2022/automated-analysis-data/TypeScript/";

    public static final String JSON = "/json/";

    /**
     * Extract conflict tuples from every x_metadata.json file (according to MergeBERT)
     * @param jsonDirectory one x_metadata.json directory path
     * @return tuples are this file's (A、O、B、R)s
     * @throws IOException IO
     * @throws JSONException JSON
     */
    public List<Map<String, String>> getTupleFromJson(String jsonDirectory, String jsonName) throws IOException, JSONException {
        logger.info("Start Collect Tuples(A O B R) From Only One JSON File : {}", jsonName);
        List<Map<String, String>> tuples = new ArrayList<>();

        File file = new File(jsonDirectory + jsonName);
        String content = FileUtils.readFileToString(file, "UTF-8");
        JSONObject jsonObject = new JSONObject(content);
        JSONArray jsonArray = jsonObject.getJSONArray("conflicting_chunks");
        logger.info("Merged file name is : {}, and it has {} conflicts", jsonName, jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++){

            Map<String, String> tuple = new HashMap<>();
            tuple.put("a_contents", jsonArray.getJSONObject(i).get("a_contents").toString());
            tuple.put("o_contents", jsonArray.getJSONObject(i).get("base_contents").toString());
            tuple.put("b_contents", jsonArray.getJSONObject(i).get("b_contents").toString());
            tuple.put("res_region", jsonArray.getJSONObject(i).get("res_region").toString());
            try{
                tuple.put("res_label", jsonArray.getJSONObject(i).get("label").toString());//may be wrong.
            }catch (JSONException e){
                tuple.put("res_label", "null");
            }
            tuple.put("file_name", jsonObject.getString("fname"));
            tuple.put("json_name", jsonName);
            tuple.put("repo", jsonObject.getString("repo"));

            tuples.add(tuple);
        }
        return tuples;
    }

    /**
     * Extract conflict blocks from every x_metadata.json file (according to MergeBERT)
     * @param jsonPath one x_metadata.json file path
     * @throws IOException IO
     * @throws JSONException JSON
     */
    public void getConflictFromJson(String jsonPath) throws IOException, JSONException {
        logger.info("Start Collect Conflicts(Block) From Only One JSON File : {}", jsonPath);

        File file = new File(jsonPath);
        String content = FileUtils.readFileToString(file, "UTF-8");
        JSONObject jsonObject = new JSONObject(content);
        JSONArray jsonArray = jsonObject.getJSONArray("conflicting_chunks");
        logger.info("Merged file name is : {}, and it has {} conflicts", jsonObject.get("fname"), jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++){
            logger.info("this is {}th conflict:-----------------------------------{}", i + 1, "\n<<<<<<<\n" + jsonArray.getJSONObject(i).get("a_contents") + "|||||||\n" + jsonArray.getJSONObject(i).get("base_contents") + "=======\n" + jsonArray.getJSONObject(i).get("b_contents") + ">>>>>>>");
        }
        logger.info("Finished processing a JSON file {}", jsonPath);
    }

    /**
     * 1.Get tuple from Json(getTupleFromJson); 2.Use newTokenizer to tokens; 3. Use tokenListToNewFile to files; 4. Use Diff3 git merge to merged file.
     * @param jsonDirectory Json file root path
     * @param jsonName Json file name
     * @throws JSONException JSON
     * @throws IOException IO
     */
    public void fromTupleToTokenDiff(String jsonDirectory, String jsonName, JSONArray jsonArrayLineLevel, Map<String, Integer> mapCount) throws Exception {
        List<Map<String, String>> tuples = getTupleFromJson(jsonDirectory, jsonName);
        String preName = jsonName.substring(0, jsonName.length() - 13);

        // Create index (BM25) [one json one merged index]
        Analyzer analyzer = new StandardAnalyzer();// custom Analyzer
        Directory directory = KeyContextCollector.createBM25(jsonDirectory + preName + "merged.java", analyzer);

        //logger.info("Start use Diff3 merge A O B to generate Token-level conflicts:-------------------------------------------------{}", jsonName);
        for (int i = 0; i < tuples.size(); i++){

            //Store to JSON file
            logger.info("Start Collect Line-level Conflict(contain token-level) To Generate JSON File From Only One JSON File : {}", jsonName);
            mapCount.put("line_allCount", mapCount.getOrDefault("line_allCount", 0) + 1);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", mapCount.get("line_allCount"));
            jsonObject.put("id_inFile", i + 1);
            jsonObject.put("file_name", tuples.get(i).get("file_name"));
            jsonObject.put("json_name", tuples.get(i).get("json_name"));
            jsonObject.put("repo", tuples.get(i).get("repo"));
            jsonObject.put("a_contents", tuples.get(i).get("a_contents"));
            jsonObject.put("o_contents", tuples.get(i).get("o_contents"));
            jsonObject.put("b_contents", tuples.get(i).get("b_contents"));
            jsonObject.put("res_region", tuples.get(i).get("res_region"));
            jsonObject.put("res_label", tuples.get(i).get("res_label"));

            //1.Judge: merge or not ?
            List<String> listA = DzyUtils.newTokenizer(tuples.get(i).get("a_contents"));
            List<String> listO = DzyUtils.newTokenizer(tuples.get(i).get("o_contents"));
            List<String> listB = DzyUtils.newTokenizer(tuples.get(i).get("b_contents"));
            int lineA = DzyUtils.tokenCountInList("NewLineDZY", listA);
            int lineO = DzyUtils.tokenCountInList("NewLineDZY", listO);
            int lineB = DzyUtils.tokenCountInList("NewLineDZY", listB);
            int maxLine = Math.max(Math.max(lineA, lineO), lineB);
            int minLine = Math.min(Math.min(lineA, lineO), lineB);

            // (1) no merge
            System.out.println("maxLine - minLine = " + (maxLine - minLine));
            if (false){ // merge all
//            if (maxLine - minLine > 2){ // only merge fit_merge
//            if (maxLine > 5){
                mapCount.put("unfit_merge", mapCount.getOrDefault("unfit_merge", 0) + 1);
                jsonObject.put("can_token_level", false);
                jsonObject.put("can_merge_succeed", "null");
                jsonObject.put("match_rate", 0.00);
                jsonObject.put("token_level_result", "null");
                jsonObject.put("key_information", "null");//line-level key_information?
                jsonObject.put("key_context", "null");//line-level key_context?
                jsonArrayLineLevel.put(jsonObject);
                continue;
            }

            // (2) yes merge
            mapCount.put("fit_merge", mapCount.getOrDefault("fit_merge", 0) + 1);

            //2.Define temp file path and Build new file
            String aPath = jsonDirectory + preName + (i + 1) + "_A.txt";
            String oPath = jsonDirectory + preName + (i + 1) + "_O.txt";
            String bPath = jsonDirectory + preName + (i + 1) + "_B.txt";
            String mergedPath = jsonDirectory + preName + (i + 1) + "_merged.txt";
            DzyUtils.tokenListToNewFile(aPath, listA);//A
            DzyUtils.tokenListToNewFile(oPath, listO);//O
            DzyUtils.tokenListToNewFile(bPath, listB);//B

            //3.Start use Diff3 merge A O B to generate Token-level conflicts
            File a = new File(aPath);
            File o = new File(oPath);
            File b = new File(bPath);
            File merged = new File(mergedPath);
            if(merged.exists()) merged.delete();
            Files.copy(a.toPath(), merged.toPath());
            logger.info("Id : {}. git merge-file --diff3 {} {} {}", mapCount.get("line_allCount"), a.getName(), o.getName(), b.getName());
            ProcessBuilder pb = new ProcessBuilder(
                    "git",
                    "merge-file",
                    "--diff3",
                    merged.getPath(),
                    o.getPath(),
                    b.getPath());
            try {
                pb.start().waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Core : Get Key Information
            List<Map<String, String>> keyInformation = KeyInformationCollector.extractTokenTuples(mergedPath);
            JSONArray jsonArrayInformation = new JSONArray();
            int count = 1;
            for (Map<String, String> map : keyInformation){
                JSONObject jsonObject1 = new JSONObject();
                jsonObject1.put("id_tokenLevel", count++);
                jsonObject1.put("a_keyInfo", map.get("a_tokens"));
                jsonObject1.put("o_keyInfo", map.get("o_tokens"));
                jsonObject1.put("b_keyInfo", map.get("b_tokens"));
                jsonArrayInformation.put(jsonObject1);
            }
            jsonObject.put("key_information", jsonArrayInformation);

            // Core : Get Key Context
            List<Map<String, String>> keyContext = KeyContextCollector.useBM25(directory, keyInformation, 1, analyzer);// Get keyContext with BM25Index
            JSONArray jsonArrayContext = new JSONArray();
            count = 1;
            for (Map<String, String> map : keyContext){
                JSONObject jsonObject1 = new JSONObject();
                jsonObject1.put("id_tokenLevel", count++);
                jsonObject1.put("a_keyContext", map.get("a_keyContext"));
                jsonObject1.put("o_keyContext", map.get("o_keyContext"));
                jsonObject1.put("b_keyContext", map.get("b_keyContext"));
                jsonArrayContext.put(jsonObject1);
            }
            jsonObject.put("key_context", jsonArrayContext);

            //4.Restoring conflicting blocks of token-level to row line-level construction
            FileReader fileReader = new FileReader(merged);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            StringBuilder fileContext = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith("<<<<<<<") || line.startsWith("|||||||") || line.startsWith("=======") || line.startsWith(">>>>>>>")){
                    fileContext.append("\n");
                    fileContext.append(line);
                    fileContext.append("\n");
                }else {
                    fileContext.append(line + " ");
                }
            }
            String tokenMergeResult = fileContext.toString().replace("NewLineDZY", "\n");
//            String lineMergedPath = jsonDirectory + preName + (i + 1) + "_lineMerged.txt";
//            Path path = Paths.get(lineMergedPath);
//            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
//                //write in Line-Merged file
//                writer.write(tokenMergeResult);//Restore special token
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }

            //5.Delete temp file(A O B merged)
            a.delete();
            o.delete();
            b.delete();

            //Store to JSON file
            jsonObject.put("can_token_level", true);//yes merge
            if (!tokenMergeResult.contains("<<<<<<<")){
                mapCount.put("merge_succeed", mapCount.getOrDefault("merge_succeed", 0) + 1);
                jsonObject.put("can_merge_succeed", true);
            }else {
                jsonObject.put("can_merge_succeed", false);
            }
            Double matchRate = DzyUtils.perfectMatchRate(tokenMergeResult, tuples.get(i).get("res_region"));
            if (matchRate == 100) mapCount.put("merge_correct", mapCount.getOrDefault("merge_correct", 0) + 1);
            jsonObject.put("match_rate", matchRate);
            jsonObject.put("token_level_result", tokenMergeResult);
            jsonArrayLineLevel.put(jsonObject);
        }
        directory.close();
    }

    /**
     * Merge token-level conflicts across the entire directory.
     * @param directory json directory
     */
    public void allTuplesToTokenDiff(String directory, String jsonName) throws Exception {
        JSONArray jsonArray = new JSONArray();
        Map<String, Integer> map = new HashMap<>();
        File files = new File(directory);
        String[] list = files.list();
        for (String fileName : list){
            if (fileName.endsWith(".json")){
                fromTupleToTokenDiff(directory, fileName, jsonArray, map);
            }
        }
        //Write in file.
        DzyUtils.stringToBuildFile(directory + JSON + jsonName, jsonArray.toString());
        logger.info("Statistical results of {}:\n{}", jsonName, map);
    }

    /**
     * Start Count Numbers Of Resolution Label
     * @param jsonDirectory json directory
     * @param jsonName json file name
     */
    public Map<String, Integer> countNumFromPrettyJson(String jsonDirectory, String jsonName) throws IOException, JSONException {
        logger.info("Start Count Numbers Of Resolution Label: {}", jsonName);
        Map<String, Integer> map = new HashMap<>();

        File file = new File(jsonDirectory + jsonName);
        String content = FileUtils.readFileToString(file, "UTF-8");
        JSONArray jsonArray = new JSONArray(content);
        for (int i = 0; i < jsonArray.length(); i++){
            map.put(jsonArray.getJSONObject(i).getString("res_label"), map.getOrDefault(jsonArray.getJSONObject(i).getString("res_label"), 0) + 1);
        }
        return map;
    }
    //Resolution Label In Perfect Match
    public Map<String, Integer> countPerfectFromJson(String jsonDirectory, String jsonName) throws IOException, JSONException {
        logger.info("Start Count Numbers Of Resolution Label in Perfect Match: {}", jsonName);
        Map<String, Integer> map = new HashMap<>();

        File file = new File(jsonDirectory + jsonName);
        String content = FileUtils.readFileToString(file, "UTF-8");
        JSONArray jsonArray = new JSONArray(content);
        for (int i = 0; i < jsonArray.length(); i++){
            if (jsonArray.getJSONObject(i).getBoolean("can_merge_succeed")){
                map.put("all_succeeded", map.getOrDefault("all_succeeded", 0) + 1);
                map.put(jsonArray.getJSONObject(i).getString("res_label"), map.getOrDefault(jsonArray.getJSONObject(i).getString("res_label"), 0) + 1);
            }
        }
        return map;
    }

    public Map<String, Integer> countChatGPTMatchRate(String jsonDirectory, String jsonName) throws IOException, JSONException {
        logger.info("Get Match Rate Of ChatGPT Answer: {}", jsonName);
        Map<String, Integer> map = new HashMap<>();

        File file = new File(jsonDirectory + jsonName);
        String content = FileUtils.readFileToString(file, "UTF-8");
        JSONArray jsonArray = new JSONArray(content);
        for (int i = 0; i < jsonArray.length(); i++){
            JSONObject curJson = jsonArray.getJSONObject(i);
            String resolution = curJson.getString("res_region");

            double line_no_maxMatch = getMaxMatchInJSONArray(curJson.getJSONArray("line_noContext_answer"), resolution);
            curJson.put("line_no_maxMatch", line_no_maxMatch);
            double line_with_maxMatch = getMaxMatchInJSONArray(curJson.getJSONArray("line_withContext_answer"), resolution);
            curJson.put("line_with_maxMatch", line_with_maxMatch);
            double token_no_maxMatch = getMaxMatchInJSONArray(curJson.getJSONArray("token_noContext_answer"), resolution);
            curJson.put("token_no_maxMatch", token_no_maxMatch);
            double token_with_maxMatch = getMaxMatchInJSONArray(curJson.getJSONArray("token_withContext_answer"), resolution);
            curJson.put("token_with_maxMatch", token_with_maxMatch);
        }
        //Rewrite in file.
        DzyUtils.stringToBuildFile(jsonDirectory + jsonName, jsonArray.toString());
        return map;
    }
    private double getMaxMatchInJSONArray(JSONArray answers, String resolution){// Be Used by last
        double maxMatchRate = 0.0;
        for (int k = 0; k < answers.length(); k++) {
            String currentAnswer = (String) answers.get(k);
            double currentMatchRate = DzyUtils.perfectMatchRate(currentAnswer, resolution);
            maxMatchRate = Math.max(maxMatchRate, currentMatchRate);
        }
        return maxMatchRate;
    }

    public static void main(String[] args) throws Exception {
        DatasetCollector collector = new DatasetCollector();

        collector.countChatGPTMatchRate("G:/now/2024merge/mergeMinePython/json/","test.json");

        //Count Numbers Of Resolution Label
//        Map<String, Integer> map = collector.countNumFromPrettyJson("G:\\now\\2024merge\\MergeBERT_Data\\fse2022\\automated-analysis-data\\Java\\json\\", "javaContextPrettyVersionAll2.json");
//        Map<String, Integer> map = collector.countPerfectFromJson("G:\\now\\2024merge\\MergeBERT_Data\\fse2022\\automated-analysis-data\\Java\\json\\", "javaContextPrettyVersionAll2.json");
//        System.out.println(map);

        //Generate Json File.
//        collector.allTuplesToTokenDiff(JavaDirectory, "javaContextVersion2.json");//maxLine <= 5
//        collector.allTuplesToTokenDiff("G:\\now\\2024merge\\ChatGPTResearch\\exampleData\\acceptA\\", "acceptA.json");


    }

}
