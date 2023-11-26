package org.njupt.util;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.apache.commons.text.similarity.JaccardSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;


public class DzyUtils {

    public static final Logger logger = LoggerFactory.getLogger(DzyUtils.class);


    /**
     * Tokenize code line (use javaParser)
     * @param codeLine code line String
     * @return tokens list
     */
    public static List<String> tokenize(String codeLine){

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
    public static List<String> newTokenizer(String code){

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
    public static String newTokenizerToString(String code){
        StringBuilder tokenString = new StringBuilder();
        JavaParser javaParser = new JavaParser(new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver())));
        Optional<CompilationUnit> compilationUnit = javaParser.parse(code).getResult();
        if (compilationUnit.isPresent()) {
            TokenRange tokenRange = compilationUnit.get().getTokenRange().get();
            tokenRange.forEach(token -> {
                if (!Objects.equals(token.getText().replaceAll("\\s*",""), "")){
                    tokenString.append(token.getText()).append(" ");
                }
            });
        } else {
//            logger.error("Code not parsed correctly! ***************************************************Try to use Unicode");
            return DzyUtils.tokenizeUnicodeToString(code);
        }
        return tokenString.toString();
    }

    /**
     * Unicode tokenize code(with comment)
     * @param codeComment code with comment(javaParser can't have use)
     * @return list
     */
    public static List<String> tokenizeUnicode(String codeComment){
        codeComment = codeComment.replace("\n", " NewLineDZY ");
        String regex = "[\\p{L}\\p{M}\\p{N}]+(?:\\p{Pi}[\\p{L}\\p{M}\\p{N}]+)*|[\\p{P}\\p{S}]";
        String[] parts = Pattern.compile(regex).matcher(codeComment).results().map(MatchResult::group).toArray(String[]::new);
        return List.of(parts);
    }
    public static String tokenizeUnicodeToString(String codeComment){
        String regex = "[\\p{L}\\p{M}\\p{N}]+(?:\\p{Pi}[\\p{L}\\p{M}\\p{N}]+)*|[\\p{P}\\p{S}]";
        String[] parts = Pattern.compile(regex).matcher(codeComment).results().map(MatchResult::group).toArray(String[]::new);
        StringBuilder tokenString = new StringBuilder();
        for (String part : parts){
            tokenString.append(part).append(" ");
        }
        return tokenString.toString();
    }

    /**
     * According to String, write in file
     * @param filePath file's path
     * @param context file's context
     */
    public static void stringToBuildFile(String filePath, String context){
        Path path = Paths.get(filePath);

        // Check if the parent directory exists, if not, create it
        Path parentDir = path.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create parent directory: " + parentDir, e);
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Official Code perfect matching rate.
     * @param ours ours code
     * @param target target code
     * @return percent of match rate
     */
    public static Double perfectMatchRate(String ours, String target){
        ours = ours.replaceAll("\\s*","");
        target = target.replaceAll("\\s*","");
        if (ours.length() == 0 && target.length() == 0) return 100.0;

        //1.Use Apache Commons Text(Jaccard Similarity)
//        JaccardSimilarity jaccardSimilarity = new JaccardSimilarity();
//        double similarity = jaccardSimilarity.apply(ours, target);

        //2.Use Apache Commons Text(Levenshtein Similarity)
        int distance = LevenshteinDistance.getDefaultInstance().apply(ours, target);
        double similarity = 1 - (double) distance / Math.max(ours.length(), target.length());

        DecimalFormat decimalFormat = new DecimalFormat("#0.00");
        logger.info("Code's similarity is : {}%", decimalFormat.format(similarity * 100));
        return Double.valueOf(decimalFormat.format(similarity * 100));
    }

    /**
     * Take token list to new file
     * @param filePath new file path
     * @param tokenList token list
     */
    public static void tokenListToNewFile(String filePath, List<String> tokenList){
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
     * Count the number of times a character appears in the list
     * @param target target token
     * @param list token list
     * @return count
     */
    public static int tokenCountInList(String target, List<String> list){
        int count = 0;
        for (String token : list){
            if (Objects.equals(target, token)) count++;
        }
        return count;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(perfectMatchRate("  \n\n\n","      "));


//        System.out.println(DzyUtils.newTokenizerToString("eventsRepository.syncTagsFilter(historyManagerParams.getFilter().getTagsFilter());\n"));
//        System.out.println(DzyUtils.tokenizeUnicodeToString("eventsRepository.syncTagsFilter(historyManagerParams.getFilter().getTagsFilter());\n"));
    }

}
