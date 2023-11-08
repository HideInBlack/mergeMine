package org.njupt.core;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DatasetCollector {

    public static final Logger logger = LoggerFactory.getLogger(DatasetCollector.class);

    public static final String JavaDirectory = "G:/now/2024merge/MergeBERT_Data/fse2022/automated-analysis-data/Java/";

    public static final String CSharpDirectory = "G:/now/2024merge/MergeBERT_Data/fse2022/automated-analysis-data/CSharp/";

    public static final String JavaScriptDirectory = "G:/now/2024merge/MergeBERT_Data/fse2022/automated-analysis-data/JavaScript/";

    public static final String TypeScriptDirectory = "G:/now/2024merge/MergeBERT_Data/fse2022/automated-analysis-data/TypeScript/";

    /**
     * Extract conflict tuples from every x_metadata.json file (according to MergeBERT)
     * @param jsonPath one x_metadata.json file path
     * @return tuples are this file's (A、O、B、R)s
     * @throws IOException IO
     * @throws JSONException JSON
     */
    public List<List<String>> getTupleFromJson(String jsonPath) throws IOException, JSONException {
        logger.info("Start Collect Tuples(A O B R) From Only One JSON File : {}", jsonPath);

        List<List<String>> tuples = new ArrayList<>();

        File file = new File(jsonPath);
        String content = FileUtils.readFileToString(file, "UTF-8");
        JSONObject jsonObject = new JSONObject(content);
        JSONArray jsonArray = jsonObject.getJSONArray("conflicting_chunks");
        logger.info("Merged file name is : {}, and it has {} conflicts", jsonObject.get("fname"), jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++){
            List<String> tuple = new ArrayList<>();
            tuple.add(jsonArray.getJSONObject(i).get("a_contents").toString());
            tuple.add(jsonArray.getJSONObject(i).get("base_contents").toString());
            tuple.add(jsonArray.getJSONObject(i).get("b_contents").toString());
            tuple.add(jsonArray.getJSONObject(i).get("res_region").toString());
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
        //Firstly, need replace "\n" with "_NewLine_" to rebuild line-level conflict
        codeLine = codeLine.replace("\n", " _NewLine_ ");

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
        //Firstly, need replace "\n" with "_NewLine_" to rebuild line-level conflict
        code = code.replace("\n", "\n _NewLine_ ");

        JavaParser javaParser = new JavaParser(new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver())));
        Optional<CompilationUnit> compilationUnit = javaParser.parse(code).getResult();
        if (compilationUnit.isPresent()) {
            TokenRange tokenRange = compilationUnit.get().getTokenRange().get();
            tokenRange.forEach(token -> {
                if (!Objects.equals(token.getText().replaceAll("\\s*",""), "")){
                    tokenList.add(token.getText());
                }
            });
        } else {
            logger.error("Code not parsed correctly! ******************************************************\n{}", code);
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
     * @param jsonPath Json file root path
     * @param jsonName Json file name
     * @throws JSONException JSON
     * @throws IOException IO
     */
    public void fromTupleToTokenDiff(String jsonPath, String jsonName) throws JSONException, IOException {

        List<List<String>> tuples = getTupleFromJson(jsonPath + jsonName);
        String preName = jsonName.substring(0, jsonName.length() - 13);

        logger.info("Start use Diff3 merge A O B to generate Token-level conflicts:-------------------------------------------------");
        for (int i = 0; i < tuples.size(); i++){
            String aPath = jsonPath + preName + (i + 1) + "_A.txt";
            String oPath = jsonPath + preName + (i + 1) + "_O.txt";
            String bPath = jsonPath + preName + (i + 1) + "_B.txt";
            String mergedPath = jsonPath + preName + (i + 1) + "_merged.txt";

            //1.tokenize 2.listToFile 3.turn:A.O.B.R
            tokenListToNewFile(aPath,newTokenizer(tuples.get(i).get(0)));//A
            tokenListToNewFile(oPath,newTokenizer(tuples.get(i).get(1)));//O
            tokenListToNewFile(bPath,newTokenizer(tuples.get(i).get(2)));//B

            //Start use Diff3 merge A O B to generate Token-level conflicts
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

            //Restoring conflicting blocks of token-level to row line-level construction
            String lineMergedPath = jsonPath + preName + (i + 1) + "_lineMerged.txt";
            Path path = Paths.get(lineMergedPath);

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

            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                //write in Line-Merged file
                writer.write(fileContext.toString().replace("_NewLine_", "\n"));//Restore special token
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            //Delete temp file(A O B)
//            a.delete();
//            o.delete();
//            b.delete();
        }
    }

    /**
     * Customized calculation of perfect match rate. (Note: This calculation is not accurate and can only calculate a perfect match!)
     * @param ours ours code
     * @param target target code
     * @return
     */
    private Double perfectMatchRate(String ours, String target){
        List<String> oursTokens  = new ArrayList<>();
        List<String> targetTokens  = new ArrayList<>();

        ours = ours.replace("\n", " ");
        target = target.replace("\n", " ");

        //USE JavaParser tokenize code.
        JavaParser javaParser = new JavaParser(new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver())));
        Optional<CompilationUnit> compilationUnitOurs = javaParser.parse(ours).getResult();
        Optional<CompilationUnit> compilationUnitTarget = javaParser.parse(target).getResult();
        if (compilationUnitOurs.isPresent()) {
            TokenRange tokenRange = compilationUnitOurs.get().getTokenRange().get();
            tokenRange.forEach(token -> {
                if (!Objects.equals(token.getText().replaceAll("\\s*",""), "")){
                    oursTokens.add(token.getText());
                }
            });
        } else {
            logger.error("Code not parsed correctly! here:\n{}", ours);
        }if (compilationUnitTarget.isPresent()) {
            TokenRange tokenRange = compilationUnitTarget.get().getTokenRange().get();
            tokenRange.forEach(token -> {
                if (!Objects.equals(token.getText().replaceAll("\\s*",""), "")){
                    targetTokens.add(token.getText());
                }
            });
        } else {
            logger.error("Code not parsed correctly! here:\n{}", target);
        }

        //make them change to be same length.
        if (oursTokens.size() < targetTokens.size()){
            return (double) oursTokens.size() / targetTokens.size();//Note: this is false; It shouldn't be calculated like this
        }else if (oursTokens.size() > targetTokens.size()){
            return (double) targetTokens.size() / oursTokens.size();//Note: this is false; It shouldn't be calculated like this
        }else {
            int count = 0;
            for (int i = 0; i < oursTokens.size(); i++){
                if (Objects.equals(oursTokens.get(i), targetTokens.get(i))) count++;
            }
            return (double) count / oursTokens.size() * 100;
        }
    }

    /**
     * Official Code perfect matching rate.
     */
    private Double officialPerfectMatchRate(String ours, String target){

        return 100.00;
    }

    /**
     * Merge token-level conflicts across the entire directory.
     * @param directory json directory
     */
    public void allTuplesToTokenDiff(String directory) throws JSONException, IOException {
        File files = new File(directory);
        String[] list = files.list();
        for (String fileName : list){
            if (fileName.endsWith(".json")){
                System.out.println(fileName);
                fromTupleToTokenDiff(directory, fileName);
            }
        }
    }



    public static void main(String[] args) throws IOException, JSONException {
        DatasetCollector collector = new DatasetCollector();
        //collector.getConflictFromJson("G:\\now\\2024merge\\ChatGPTResearch\\exampleData\\acceptA\\100004_metadata.json");

        String dir = "G:\\now\\2024merge\\ChatGPTResearch\\exampleData\\acceptA\\";
        String dirB = "G:\\now\\2024merge\\ChatGPTResearch\\exampleData\\acceptB\\";
        String dirBA = "G:\\now\\2024merge\\ChatGPTResearch\\exampleData\\acceptBA\\";
//        collector.fromTupleToTokenDiff(dir, "100004_metadata.json");
//        collector.fromTupleToTokenDiff(dir, "100034_metadata.json");
//        collector.fromTupleToTokenDiff(dirB, "106855_metadata.json");
//        collector.fromTupleToTokenDiff(dirBA, "101216_metadata.json");
//        collector.fromTupleToTokenDiff(dirBA, "140328_metadata.json");
//        System.out.println(collector.perfectMatchRate("\n" +
//                " private final static IngestServices ingestServices = IngestServices . getInstance ( ) ; \n" +
//                " private final static Logger logger = ingestServices . getLogger ( SQLiteReader . class . getName ( ) ) ; \n" +
//                " \n" +
//                " ", "    public final static IngestServices ingestServices = IngestServices.getInstance();\n    private final static Logger logger = ingestServices.getLogger(SQLiteReader.class.getName());\n    \n"));

        collector.allTuplesToTokenDiff("G:/now/2024merge/ChatGPTResearch/exampleData/acceptB/");



//        System.out.println(collector.tokenize("/**\n" +
//                "     * Select an actor in a Human task in the list of Process Actor\n" +
//                "     *"));
//        System.out.println(collector.newTokenizer("/**\n" +
//                "     * Select an actor in a Human task in the list of Process Actor\n" +
//                "     *"));


        /**
         * 演示 1 tokenize 与 newTokenizer 区别
         */
//        String codeLines = "Platform.runLater(() -> {\n" +
//                "            //if there is an existing prompt or progressdialog,...\n" +
//                "            if (promptDialogManager.bringCurrentDialogToFront()) {\n" +
//                "                //... just show that\n" +
//                "            } else {\n" +
//                "\n" +
//                "                if ( //confirm timeline during ingest\n" +
//                "                        IngestManager.getInstance().isIngestRunning()\n" +
//                "                        && promptDialogManager.confirmDuringIngest() == false) {\n" +
//                "                    return;  //if they cancel, do nothing.\n" +
//                "                }";
//        System.out.println(collector.tokenize(codeLines));
//        System.out.println("------------------------------------------------------------------------------------------------------------------------------");
//        System.out.println(collector.newTokenizer(codeLines));


    }

}
