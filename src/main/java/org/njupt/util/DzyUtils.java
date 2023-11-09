package org.njupt.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.similarity.JaccardSimilarity;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;


import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;


public class DzyUtils {

    public static final Logger logger = LoggerFactory.getLogger(DzyUtils.class);

    /**
     * According to String, write in file
     * @param filePath file's path
     * @param context file's context
     */
    public static void stringToBuildFile(String filePath, String context){
        Path path = Paths.get(filePath);
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
        //Use Apache Commons Text(Jaccard Similarity)
        JaccardSimilarity jaccardSimilarity = new JaccardSimilarity();
        double similarity = jaccardSimilarity.apply(ours, target);

        DecimalFormat decimalFormat = new DecimalFormat("#0.00");
        logger.info("Code's similarity is : {}%", decimalFormat.format(similarity * 100));
        return Double.valueOf(decimalFormat.format(similarity * 100));
    }

    private static void addCodeLine(IndexWriter writer, String codeLine, int line) throws Exception {
        Document doc = new Document();
        doc.add(new TextField("code", codeLine, Field.Store.YES));
        doc.add(new TextField("line", String.valueOf(line), Field.Store.YES));
        writer.addDocument(doc);
    }
    public void getBM25 () throws Exception {
        // 创建内存索引 G:\now\2024merge\ChatGPTResearch\exampleData\BM25
        Directory index = FSDirectory.open(new File("G:\\now\\2024merge\\ChatGPTResearch\\exampleData\\BM25").toPath());

        // 创建分析器
        //Analyzer analyzer = new IKAnalyzer();//中文
        Analyzer analyzer = new SimpleAnalyzer();

        // 创建索引写入器配置
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        // 创建索引写入器
        IndexWriter writer =  new IndexWriter(index, config);


        // 添加示例代码行到索引中
        int count = 0;
        addCodeLine(writer, "int a = 10;中国人才是牛逼", count++);
        addCodeLine(writer, "int b = 20;我们大家都是中国人", count++);
        addCodeLine(writer, "int c = a + b;中华人民共和国", count++);
        addCodeLine(writer, "System.out.println(c);这里是中国", count++);

        // 提交写入器并关闭
        writer.close();

        //----------------------------------------------------------------------------------
        // 创建查询解析器
        IndexReader indexReader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(indexReader);

        // 创建查询对象
        Query query = new TermQuery(new Term("code", "b"));

        // 执行查询
        TopDocs topDocs = searcher.search(query, 3);

        // 获取查询结果
        ScoreDoc[] hits = topDocs.scoreDocs;
        System.out.println(hits.length);
        for (ScoreDoc hit : hits){
            System.out.println(hit.score);
        }

        // 输出最相似的代码行
        if (hits.length > 0) {
            int docId = hits[0].doc;
            Document doc = searcher.doc(docId);
            System.out.println("Most similar code line: " + doc.get("code"));
        } else {
            System.out.println("No similar code line found.");
        }

        // 关闭读取器
        indexReader.close();
    }

    private static void addDocument(IndexWriter indexWriter, String id, String content) throws Exception {
        Document document = new Document();
        document.add(new StringField("id", id, Field.Store.YES));
        document.add(new StringField("content", content, Field.Store.YES));
        indexWriter.addDocument(document);
    }
    public void getBM25New() throws Exception {
        // 创建内存索引
        Directory directory = FSDirectory.open(new File("G:\\now\\2024merge\\ChatGPTResearch\\exampleData\\BM25New").toPath());
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        IndexWriter indexWriter = new IndexWriter(directory, config);

        // 添加文献内容到索引
        addDocument(indexWriter, "1", "This is the first document");
        addDocument(indexWriter, "2", "This document is the second document");
        addDocument(indexWriter, "3", "And this is the third one");
        indexWriter.close();

        // 创建BM25模型
        BM25Similarity similarity = new BM25Similarity();

        // 创建索引搜索器
        DirectoryReader directoryReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(directoryReader);
        indexSearcher.setSimilarity(similarity);

        // 创建查询解析器
        QueryParser queryParser = new QueryParser("content", new StandardAnalyzer());

        // 输入的英文字符串
        String queryStr = "This is the first document";

        // 解析查询字符串
        Query query = queryParser.parse(queryStr);

        // 执行查询
        TopDocs topDocs = indexSearcher.search(query, 10);
        System.out.println(topDocs.scoreDocs.length);
        // 输出查询结果
        System.out.println("Query: " + queryStr);
        System.out.println("Results:");
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document document = indexSearcher.doc(scoreDoc.doc);
            System.out.println("Document ID: " + document.get("id") + ", Score: " + scoreDoc.score);
        }

        // 关闭资源
        directoryReader.close();
        directory.close();
    }

    private static void createIndexAndAddDocuments(Path indexPath) throws IOException {
        // create documents
        Document doc1 = new Document();
        doc1.add(new TextField("Title", "Apparatus for manufacturing green bricks for the brick manufacturing industry",
                Field.Store.YES));
        doc1.add(new TextField("Abstract",
                "The invention relates to an apparatus (1) for manufacturing green bricks from clay for the brick manufacturing industry, comprising a circulating conveyor (3) carrying mould containers combined to mould container parts (4), a reservoir (5) for clay arranged above the mould containers, means for carrying clay out of the reservoir (5) into the mould containers, means (9) for pressing and trimming clay in the mould containers, means (11) for supplying and placing take-off plates for the green bricks (13) and means for discharging green bricks released from the mould containers, characterized in that the apparatus further comprises means (22) for moving the mould container parts (4) filled with green bricks such that a protruding edge is formed on at least one side of the green bricks",
                Field.Store.YES));

        Document doc2 = new Document();
        doc2.add(new TextField("Title",
                "Some other title, for example: Apparatus for manufacturing green bricks for the brick manufacturing industry",
                Field.Store.YES));
        doc2.add(new TextField("Abstract",
                "Some other abstract, for example: The invention relates to an apparatus (1) for manufacturing green bricks from clay for the brick manufacturing industry, comprising a circulating conveyor (3) carrying mould containers combined to mould container parts (4), a reservoir (5) for clay arranged above the mould containers, means for carrying clay out of the reservoir (5) into the mould containers, means (9) for pressing and trimming clay in the mould containers, means (11) for supplying and placing take-off plates for the green bricks (13) and means for discharging green bricks released from the mould containers, characterized in that the apparatus further comprises means (22) for moving the mould container parts (4) filled with green bricks such that a protruding edge is formed on at least one side of the green bricks",
                Field.Store.YES));

        Document doc3 = new Document();
        doc3.add(new TextField("Title", "A document with a competely different title", Field.Store.YES));
        doc3.add(new TextField("Abstract",
                "This document also has a completely different abstract which is in no way similar to the abstract of the previous documents.",
                Field.Store.YES));

        IndexWriter iw = new IndexWriter(FSDirectory.open(indexPath), new IndexWriterConfig(new StandardAnalyzer()));
        iw.deleteAll();
        iw.addDocument(doc1);
        iw.addDocument(doc2);
        iw.addDocument(doc3);
        iw.close();
    }


    public static void main(String[] args) throws Exception {
        DzyUtils dzyUtils = new DzyUtils();
//        dzyUtils.getBM25New();


        Path path = Paths.get("G:\\now\\2024merge\\ChatGPTResearch\\exampleData\\BM3");

        // create index
        createIndexAndAddDocuments(path);

        // open index reader and create index searcher
        IndexReader ir = DirectoryReader.open(FSDirectory.open(path));
        IndexSearcher is = new IndexSearcher(ir);
        is.setSimilarity(new BM25Similarity());

        // document which is used to create the query
//        Document doc = ir.document(1);

        // create query parser
        QueryParser queryParser = new QueryParser("Abstract", new StandardAnalyzer());


        // create query
        Query query = queryParser.parse("This document also has a completely different abstract which is in no way similar to the abstract of the previous documents.");

        // search
        for (ScoreDoc result : is.search(query, Integer.MAX_VALUE).scoreDocs) {
            System.out.println(result.doc + "\t" + result.score);
        }


    }

}
