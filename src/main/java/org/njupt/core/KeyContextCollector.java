package org.njupt.core;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.njupt.util.DzyUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class KeyContextCollector {

    private static final String StartDiffMark = "<<<<<<<";

    private static final String EndDiffMark = ">>>>>>>";

    public static final String BM25INDEX = "G:/now/2024merge/backup/BM25Index";

    public static Directory createBM25(String javaFilePath, Analyzer analyzer) throws Exception {
        // 1.创建磁盘索引、配置索引解析器、应用到索引写入器
        Directory directory = FSDirectory.open(new File(BM25INDEX).toPath());
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        // 2.读取merged.java file中每一行代码内容，添加到到索引文件
        indexWriter.deleteAll();

        FileReader fileReader = new FileReader(javaFilePath);
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        String lineContent; int lineCount = 1; boolean key = true;
        while ((lineContent = bufferedReader.readLine()) != null) {

            //Conflicts block don't be added in document !
            if (lineContent.startsWith(StartDiffMark)){
                key = false;
                continue;
            }
            if (lineContent.startsWith(EndDiffMark)) {
                key = true;
                continue;
            }
            if (key){
                //System.out.println(lineContent);
                //addDocument(indexWriter, lineCount++, lineContent);
                addDocument(indexWriter, lineCount++, DzyUtils.newTokenizerToString(lineContent));//need to tokenize code
            }
        }
        // 3.数据流关闭
        indexWriter.close();
        return directory;
    }

    public static List<Map<String, String>> useBM25(Directory directory, List<Map<String, String>> keyInfoTuples, int queryNumber, Analyzer analyzer) throws IOException, ParseException {
        // 1.创建索引搜索器、设置模型为BM25算法
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        BM25Similarity similarity = new BM25Similarity();
        indexSearcher.setSimilarity(similarity);

        // 2.创建查询解析器
        QueryParser queryParser = new QueryParser("codeContent", analyzer);

        // 3.执行查询
        List<Map<String, String>> contextList = new ArrayList<>();
        for (Map<String, String> tuple : keyInfoTuples){
            Map<String, String> map = new HashMap<>();
            map.put("a_keyContext", queryBM25(indexSearcher, queryParser, tuple.get("a_tokens"), queryNumber));
            map.put("o_keyContext", queryBM25(indexSearcher, queryParser, tuple.get("o_tokens"), queryNumber));
            map.put("b_keyContext", queryBM25(indexSearcher, queryParser, tuple.get("b_tokens"), queryNumber));
            contextList.add(map);
        }

        // 4.关闭资源（directory在外面最后关闭）
        indexReader.close();
        return contextList;
    }

    private static String queryBM25(IndexSearcher indexSearcher, QueryParser queryParser , String queryKeyCode, int queryNumber) throws IOException {
//        System.out.println("QueryKeyCode: ------------------>\n" + queryKeyCode);
        //如果queryKeyCode为空 直接返回其关键上下文信息也为空！
        if (Objects.equals(queryKeyCode.replaceAll("\\s*",""), "")) return " ";

        //对query中每一个token在两边加上("),这个符号，解决了Lucene查询语法的错误问题！
        String[] queryArray = queryKeyCode.split(" ");
        StringBuilder code = new StringBuilder();
        for (String token : queryArray){
            code.append("\"").append(token).append("\"");
        }

        Query query = null;
        try {
            query = queryParser.parse(code.toString());
        } catch (Exception e) {
            System.out.println("Query code is wrong:--------------->\n" + code);
            return " ";
        }
        TopDocs topDocs = indexSearcher.search(query, queryNumber);

        StringBuilder queryResult = new StringBuilder();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document document = indexSearcher.doc(scoreDoc.doc);
            //这里其实还可以搞一个阈值！大于多少的我们认为是有用得！才会添加进去！
            queryResult.append(document.get("codeContent"));
//            System.out.println("lineId: " + document.get("lineId") + ", Score: " + scoreDoc.score + " codeContent: " + document.get("codeContent"));
        }
        return queryResult.toString();
    }

    private static void addDocument(IndexWriter indexWriter, int lineId, String codeContent) throws Exception {
        Document document = new Document();
        document.add(new TextField("lineId", String.valueOf(lineId), Field.Store.YES));
        document.add(new TextField("codeContent", codeContent, Field.Store.YES));
        indexWriter.addDocument(document);
    }

    public static void main(String[] args) throws Exception {
        //获取key Information
        List<Map<String, String>> keyInfo1 = KeyInformationCollector.extractTokenTuples("G:\\now\\2024merge\\MergeBERT_Data\\ChatGPTResearch\\exampleData\\acceptA\\100004_3_merged.txt");
        List<Map<String, String>> keyInfo2 = KeyInformationCollector.extractTokenTuples("G:\\now\\2024merge\\MergeBERT_Data\\ChatGPTResearch\\exampleData\\acceptA\\100004_4_merged.txt");

        String mergedJava = "G:\\now\\2024merge\\MergeBERT_Data\\ChatGPTResearch\\exampleData\\acceptA\\100004_merged.java";
        //创建索引文件
        Analyzer analyzer = new StandardAnalyzer();
        Directory directory = createBM25(mergedJava, analyzer);
        //使用索引查询
        System.out.println(useBM25(directory, keyInfo1, 1, analyzer));
        System.out.println("------------------------------------------------------------------------------------------------");
        System.out.println(useBM25(directory, keyInfo2, 1, analyzer));

        directory.close();

    }

}
