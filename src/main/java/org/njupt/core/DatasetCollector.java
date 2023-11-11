package org.njupt.core;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.similarity.JaccardSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
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
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

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
     * Tokenize code line (use javaParser)
     * @param codeLine code line String
     * @return tokens list
     */
    private List<String> tokenize(String codeLine){

        List<String> tokenList  = new ArrayList<>();
        //Firstly, need replace "\n" with "NewLineDZY" to rebuild line-level conflict
        codeLine = codeLine.replace("\n", " NewLineDZY ");

        StringProvider provider = new StringProvider(codeLine);
        SimpleCharStream charStream = new SimpleCharStream(provider);
        GeneratedJavaParserTokenManager tokenGenerate = new GeneratedJavaParserTokenManager(charStream);
        String strToken = tokenGenerate.getNextToken().toString();
        while (!strToken.equals("")){
            tokenList.add(strToken);
            strToken = tokenGenerate.getNextToken().toString();
        }
        return tokenList;
    }

    /**
     * New Tokenizer (use javaParser) 2023年11月5日19:35:14
     * @param code code lines
     * @return tokens list
     */
    private List<String> newTokenizer(String code){

        List<String> tokenList  = new ArrayList<>();
        //Firstly, need replace "\n" with "NewLineDZY" to rebuild line-level conflict
        String newCode = code.replace("\n", "\n NewLineDZY ");

        JavaParser javaParser = new JavaParser(new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver())));
        Optional<CompilationUnit> compilationUnit = javaParser.parse(newCode).getResult();
        if (compilationUnit.isPresent()) {
            TokenRange tokenRange = compilationUnit.get().getTokenRange().get();
            tokenRange.forEach(token -> {
                if (!Objects.equals(token.getText().replaceAll("\\s*",""), "")){
                    tokenList.add(token.getText());
                }
            });
        } else {
            logger.error("Code not parsed correctly! ***************************************************Try to use Unicode");
            return DzyUtils.tokenizeUnicode(code);
        }
        return tokenList;
    }

    /**
     * Take token list to new file
     * @param filePath new file path
     * @param tokenList token list
     */
    private void tokenListToNewFile(String filePath, List<String> tokenList){
        Path path = Paths.get(filePath);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (int i = 0; i < tokenList.size(); i++){
                writer.write(tokenList.get(i));
                if (i != tokenList.size() - 1) writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 1.Get tuple from Json(getTupleFromJson); 2.Use newTokenizer to tokens; 3. Use tokenListToNewFile to files; 4. Use Diff3 git merge to merged file.
     * @param jsonDirectory Json file root path
     * @param jsonName Json file name
     * @throws JSONException JSON
     * @throws IOException IO
     */
    public void fromTupleToTokenDiff(String jsonDirectory, String jsonName, JSONArray jsonArrayLineLevel, Map<String, Integer> mapCount) throws JSONException, IOException {
        List<Map<String, String>> tuples = getTupleFromJson(jsonDirectory, jsonName);
        String preName = jsonName.substring(0, jsonName.length() - 13);

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
            List<String> listA = newTokenizer(tuples.get(i).get("a_contents"));
            List<String> listO = newTokenizer(tuples.get(i).get("o_contents"));
            List<String> listB = newTokenizer(tuples.get(i).get("b_contents"));
            int lineA = DzyUtils.tokenCountInList("NewLineDZY", listA);
            int lineO = DzyUtils.tokenCountInList("NewLineDZY", listO);
            int lineB = DzyUtils.tokenCountInList("NewLineDZY", listB);
            int maxLine = Math.max(Math.max(lineA, lineO), lineB);
            int minLine = Math.min(Math.min(lineA, lineO), lineB);

            // (1) no merge
            System.out.println("maxLine - minLine = " + (maxLine - minLine));
            if (false){ // merge all
//            if (maxLine - minLine > 1){
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
            tokenListToNewFile(aPath, listA);//A
            tokenListToNewFile(oPath, listO);//O
            tokenListToNewFile(bPath, listB);//B

            //3.Start use Diff3 merge A O B to generate Token-level conflicts
            File a = new File(aPath);
            File o = new File(oPath);
            File b = new File(bPath);
            File merged = new File(mergedPath);
            if(merged.exists()) merged.delete();
            Files.copy(a.toPath(), merged.toPath());
            logger.info("git merge-file --diff3 {} {} {}", a.getName(), o.getName(), b.getName());
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
            merged.delete();

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
            jsonObject.put("key_information", "null");
            jsonObject.put("key_context", "null");
            jsonArrayLineLevel.put(jsonObject);
        }
    }

    /**
     * Merge token-level conflicts across the entire directory.
     * @param directory json directory
     */
    public void allTuplesToTokenDiff(String directory, String jsonName) throws JSONException, IOException {
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
        logger.info("Statistical results of {} data:\n{}", jsonName, map);
    }

    public static void main(String[] args) throws IOException, JSONException {
        DatasetCollector collector = new DatasetCollector();
        //collector.getConflictFromJson("G:\\now\\2024merge\\ChatGPTResearch\\exampleData\\acceptA\\100004_metadata.json");
        collector.allTuplesToTokenDiff(JavaDirectory, "java.json");
//        collector.allTuplesToTokenDiff("G:\\now\\2024merge\\ChatGPTResearch\\exampleData\\acceptA\\", "acceptA.json");




    }

}
