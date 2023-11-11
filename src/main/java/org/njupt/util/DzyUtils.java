package org.njupt.util;

import org.apache.commons.text.similarity.JaccardSimilarity;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
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
import java.util.regex.MatchResult;
import java.util.regex.Pattern;


public class DzyUtils {

    public static final Logger logger = LoggerFactory.getLogger(DzyUtils.class);

    public static final String BM25INDEX = "G:/now/2024merge/backup/BM25Index";

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

        //Use Apache Commons Text(Jaccard Similarity)
        JaccardSimilarity jaccardSimilarity = new JaccardSimilarity();
        double similarity = jaccardSimilarity.apply(ours, target);

        DecimalFormat decimalFormat = new DecimalFormat("#0.00");
        logger.info("Code's similarity is : {}%", decimalFormat.format(similarity * 100));
        return Double.valueOf(decimalFormat.format(similarity * 100));
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


    /**
     * Get BM25 score by query KeyCode in merged.java
     * @param javaFilePath merged.java file path
     * @param queryKeyCode query code token/line
     * @param analyzer Analyzer(StandardAnalyzer、KeywordAnalyzer、EnglishAnalyzer...)
     * @throws Exception
     */
    public void getBM25(String javaFilePath, String queryKeyCode, Analyzer analyzer) throws Exception {
        // 1.创建磁盘索引、配置索引解析器、应用到索引写入器
        Directory directory = FSDirectory.open(new File(BM25INDEX).toPath());
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        // 2.读取merged.java file中每一行代码内容，添加到到索引文件
        indexWriter.deleteAll();

        FileReader fileReader = new FileReader(javaFilePath);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String lineContent; int lineCount = 1;
        while ((lineContent = bufferedReader.readLine()) != null) {
            System.out.println(lineContent);
            addDocument(indexWriter, lineCount++, lineContent);
        }

        indexWriter.close();

        // 3.创建索引搜索器、设置模型为BM25算法
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        BM25Similarity similarity = new BM25Similarity();
        indexSearcher.setSimilarity(similarity);

        // 4.创建查询解析器
        QueryParser queryParser = new QueryParser("codeContent", analyzer);
        Query query = queryParser.parse(queryKeyCode);

        // 5.执行查询
        TopDocs topDocs = indexSearcher.search(query, 10);
        System.out.println("QueryKeyCode: " + queryKeyCode);
        System.out.println("Results:" + topDocs.scoreDocs.length);
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document document = indexSearcher.doc(scoreDoc.doc);
            System.out.println("lineId: " + document.get("lineId") + ", Score: " + scoreDoc.score + " codeContent: " + document.get("codeContent"));
        }

        // 6.关闭资源
        indexReader.close();
        directory.close();
    }
    private static void addDocument(IndexWriter indexWriter, int lineId, String codeContent) throws Exception {
        Document document = new Document();
        document.add(new TextField("lineId", String.valueOf(lineId), Field.Store.YES));
        document.add(new TextField("codeContent", codeContent, Field.Store.YES));
        indexWriter.addDocument(document);
    }

    public static void main(String[] args) throws Exception {
        DzyUtils dzyUtils = new DzyUtils();
        dzyUtils.getBM25(
                "G:\\now\\2024merge\\ChatGPTResearch\\exampleData\\TestBM25\\100004_merged.java",
                "+eventsRepository . syncTagsFilter historyManagerParams . getFilterModel ",
                new StandardAnalyzer());



    }

}
