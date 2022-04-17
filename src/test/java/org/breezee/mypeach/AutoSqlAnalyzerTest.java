package org.breezee.mypeach;

import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.core.DeleteSqlParser;
import org.breezee.mypeach.core.InsertSqlParser;
import org.breezee.mypeach.core.SelectSqlParser;
import org.breezee.mypeach.core.UpdateSqlParser;
import org.breezee.mypeach.entity.ParserResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@SpringBootTest
public class AutoSqlAnalyzerTest {
    String testFilePrefix = "src/test/java/org/breezee/mypeach/";

    @Test
    void selecet() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "01_Select.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        dicQuery.put("PROVINCE_ID","张三");
        dicQuery.put("#PROVINCE_CODE#","BJ");
        dicQuery.put("#PROVINCE_NAME#","北京");
        dicQuery.put("#DATE#","20222-02-10");
        dicQuery.put("NAME",1);
        dicQuery.put("#REMARK#","测试");
        //dicQuery.put("BF","back");
        //dicQuery.put("MDLIST",new String[]{"SE","PA","FI"});//传入一个数组
//        List<String> list = new ArrayList<String>();
//        list.add("'SE'");
//        list.add("VE");
//        list.add("UC");

        List<Integer> list = new ArrayList<Integer>();
        list.addAll(Arrays.asList(2,3,4));
        dicQuery.put("MDLIST",list);//传入一个数组
        SelectSqlParser sqlAnalyzer = new SelectSqlParser(new MyPeachProperties());
        ParserResult result = sqlAnalyzer.parse(sSql, dicQuery);
        System.out.println(result.getMessage());
        System.out.println(result.getSql());
    }

    @Test
    void selecet2() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "01_Select2.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        dicQuery.put("PROVINCE_ID","张三");
        dicQuery.put("PROVINCE_CODE","BJ");
        dicQuery.put("PROVINCE_NAME","北京");
        dicQuery.put("DATE","20222-02-10");
        dicQuery.put("NAME",1);
        dicQuery.put("REMARK","测试");
        //dicQuery.put("BF","back");
        //dicQuery.put("MDLIST",new String[]{"SE","PA","FI"});//传入一个数组
//        List<String> list = new ArrayList<String>();
//        list.add("'SE'");
//        list.add("VE");
//        list.add("UC");

        List<Integer> list = new ArrayList<Integer>();
        list.addAll(Arrays.asList(2,3,4));
        dicQuery.put("MDLIST",list);//传入一个数组
        SelectSqlParser sqlAnalyzer = new SelectSqlParser(new MyPeachProperties());
        ParserResult result = sqlAnalyzer.parse(sSql, dicQuery);
        System.out.println(result.getMessage());
        System.out.println(result.getSql());
    }

    @Test
    void insert() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "02_Insert.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        //dicQuery.put("PROVINCE_ID","张三");
        dicQuery.put("#PROVINCE_CODE#","BJ");
        dicQuery.put("#PROVINCE_NAME#","北京");
        //dicQuery.put("#SORT_ID#",1);
        //dicQuery.put("#TFLAG#",1);
        InsertSqlParser sqlAnalyzer = new InsertSqlParser(new MyPeachProperties());
        ParserResult result = sqlAnalyzer.parse(sSql, dicQuery);
        System.out.println(result.getMessage());
        System.out.println(result.getSql());
    }

    @Test
    public void update() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "03_Update.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        dicQuery.put("PROVINCE_ID","张三");
        dicQuery.put("#PROVINCE_CODE#","BJ");
        //dicQuery.put("#PROVINCE_NAME#","北京");
        dicQuery.put("#TFLG#",1);
        UpdateSqlParser sqlAnalyzer = new UpdateSqlParser(new MyPeachProperties());
        ParserResult result = sqlAnalyzer.parse(sSql, dicQuery);
        System.out.println(result.getMessage());
        System.out.println(result.getSql());
    }

    @Test
    void delete() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "04_Delete.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        dicQuery.put("PROVINCE_ID","张三");
        dicQuery.put("#PROVINCE_CODE#","BJ");
        dicQuery.put("#PROVINCE_NAME#","北京");
        dicQuery.put("#SORT_ID#",1);
        DeleteSqlParser sqlAnalyzer = new DeleteSqlParser(new MyPeachProperties());
        ParserResult result = sqlAnalyzer.parse(sSql, dicQuery);
        System.out.println(result.getMessage());
        System.out.println(result.getSql());
    }

    @Test
    void insertSelect() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "05_InsertSelect.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        dicQuery.put("PROVINCE_ID","张三");
        //dicQuery.put("#PROVINCE_CODE#","BJ");
        //dicQuery.put("#PROVINCE_NAME#","北京");
        dicQuery.put("#SORT_ID#",1);//必须
        //dicQuery.put("#TFLAG#",1);
        InsertSqlParser sqlAnalyzer = new InsertSqlParser(new MyPeachProperties());
        ParserResult result = sqlAnalyzer.parse(sSql, dicQuery);
        System.out.println(result.getMessage());
        System.out.println(result.getSql());
    }

    @Test
    void oracleWithSelecet() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "06_OracleWithSelect.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        dicQuery.put("PROVINCE_ID","张三");
        dicQuery.put("#PROVINCE_CODE#","BJ");
        dicQuery.put("#PROVINCE_NAME#","北京");
        dicQuery.put("#DATE#","20222-02-10");
        dicQuery.put("NAME",1);
        dicQuery.put("#REMARK#","测试");
        //dicQuery.put("BF","back");
        SelectSqlParser sqlAnalyzer = new SelectSqlParser(new MyPeachProperties());
        ParserResult result = sqlAnalyzer.parse(sSql, dicQuery);
        System.out.println(result.getMessage());
        System.out.println(result.getSql());
    }
}
