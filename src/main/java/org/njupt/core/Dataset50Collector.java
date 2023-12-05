package org.njupt.core;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Top-50-repo dataset with most conflicts among 2731 Java repos Dataset Collector
 */
public class Dataset50Collector {

    public static final Logger logger = LoggerFactory.getLogger(DatasetCollector.class);

    public static final String JSON = "G:/now/2024merge/Merge50Repo_Data/jsonAll/";

    /**
     * Extract conflict tuples from every x_metadata.json file (according to 50Repo)
     */
    public List<Map<String, String>> getTupleFromJson(String jsonDirectory, String jsonName) throws IOException, JSONException {
        logger.info("Start Collect Tuples(A O B R...) From Only One JSON File : {}", jsonName);
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
            try{
                tuple.put("res_region", jsonArray.getJSONObject(i).get("resolve").toString());//may be wrong.
            }catch (JSONException e){
                tuple.put("res_region", "null");
            }
            try{
                tuple.put("res_label", jsonArray.getJSONObject(i).get("label").toString());//may be wrong.
            }catch (JSONException e){
                tuple.put("res_label", "null");
            }
            tuple.put("file_name", jsonObject.getString("filename"));
            tuple.put("json_name", jsonName);
            tuples.add(tuple);
        }
        return tuples;
    }

    /**
     * 1.Get tuple from Json(getTupleFromJson); 2.Use newTokenizer to tokens; 3. Use tokenListToNewFile to files; 4. Use Diff3 git merge to merged file.
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
            jsonObject.put("a_contents", tuples.get(i).get("a_contents"));
            jsonObject.put("o_contents", tuples.get(i).get("o_contents"));
            jsonObject.put("b_contents", tuples.get(i).get("b_contents"));
            jsonObject.put("res_region", tuples.get(i).get("res_region"));
            jsonObject.put("res_label", tuples.get(i).get("res_label"));

            //1.Judge: merge or not ?
            List<String> listA = DzyUtils.newTokenizer(tuples.get(i).get("a_contents"));
            List<String> listO = DzyUtils.newTokenizer(tuples.get(i).get("o_contents"));
            List<String> listB = DzyUtils.newTokenizer(tuples.get(i).get("b_contents"));
//            int lineA = DzyUtils.tokenCountInList("NewLineDZY", listA);
//            int lineO = DzyUtils.tokenCountInList("NewLineDZY", listO);
//            int lineB = DzyUtils.tokenCountInList("NewLineDZY", listB);
//            int maxLine = Math.max(Math.max(lineA, lineO), lineB);
//            int minLine = Math.min(Math.min(lineA, lineO), lineB);
//
//            // (1) no merge
//            System.out.println("maxLine - minLine = " + (maxLine - minLine));
//            if (false){ // merge all
////            if (maxLine - minLine > 2){ // only merge fit_merge
////            if (maxLine > 5){
//                mapCount.put("unfit_merge", mapCount.getOrDefault("unfit_merge", 0) + 1);
//                jsonObject.put("can_token_level", false);
//                jsonObject.put("can_merge_succeed", "null");
//                jsonObject.put("match_rate", 0.00);
//                jsonObject.put("token_level_result", "null");
//                jsonObject.put("key_information", "null");//line-level key_information?
//                jsonObject.put("key_context", "null");//line-level key_context?
//                jsonArrayLineLevel.put(jsonObject);
//                continue;
//            }

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
     */
    public void allTuplesToTokenDiff(String directory, String jsonName) throws Exception {
        JSONArray jsonArray = new JSONArray();
        Map<String, Integer> map = new HashMap<>();
        File repoDirectory = new File(directory);
        String[] repoList = repoDirectory.list();
        for (String repo : repoList){
            System.out.println(repo);
            String currentPath = directory + repo;
            File files = new File(currentPath);
            String[] fileList = files.list();

            for (String fileName : fileList){
                if (fileName.endsWith(".json")){
                    fromTupleToTokenDiff(currentPath + "/", fileName, jsonArray, map);
                }
            }
        }
        //Write in file.
        DzyUtils.stringToBuildFile(JSON + jsonName, jsonArray.toString());
        logger.info("Statistical results of {}:\n{}", jsonName, map);
    }

    public LinkedHashMap<String, Integer> countPerfectInEveryRepo(String jsonDirectory, String jsonName) throws IOException, JSONException {
        logger.info("Start Count Numbers Of  Perfect Match In Every Repo: {}", jsonName);
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();

        File file = new File(jsonDirectory + jsonName);
        String content = FileUtils.readFileToString(file, "UTF-8");
        JSONArray jsonArray = new JSONArray(content);
        for (int i = 0; i < jsonArray.length(); i++){
            String currentRepo = jsonArray.getJSONObject(i).getString("file_name");
            map.put(currentRepo, map.getOrDefault(currentRepo, 0) + 1);
            if (jsonArray.getJSONObject(i).getDouble("match_rate") == 100) {
                map.put(currentRepo + "100", map.getOrDefault(currentRepo + "100", 0) + 1);
            }
        }
        return map;
    }

    /**
     *  --------------------------------------------------------------------main--------------------------------------------------------------------
     */
    public static void main(String[] args) throws Exception {
        Dataset50Collector dataset50Collector = new Dataset50Collector();
        //        dataset50Collector.allTuplesToTokenDiff("G:\\now\\2024merge\\Merge50Repo_Data\\index_conflict_files\\", "50RepoV1.json");

        //System.out.println(dataset50Collector.getTupleFromJson("G:\\now\\2024merge\\Merge50Repo_Data\\testBig\\test1\\", "0_metadata.json"));
        LinkedHashMap<String, Integer> inRepo = dataset50Collector.countPerfectInEveryRepo("G:\\now\\2024merge\\Merge50Repo_Data\\jsonAll\\", "50RepoV1Pretty.json");
        System.out.println(inRepo);


        DatasetCollector datasetCollector = new DatasetCollector();

//        Map<String, Integer> labels = datasetCollector.countNumFromPrettyJson("G:\\now\\2024merge\\Merge50Repo_Data\\jsonAll\\", "50RepoV1Pretty.json");
//        System.out.println(labels);
//        Map<String, Integer> countPerfectNotNull = datasetCollector.countPerfectFromJson("G:\\now\\2024merge\\Merge50Repo_Data\\jsonAll\\", "50RepoV1Pretty.json");
//        System.out.println(countPerfectNotNull);

    }

}
