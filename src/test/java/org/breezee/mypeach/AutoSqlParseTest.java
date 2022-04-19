package org.breezee.mypeach;

import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.core.*;
import org.breezee.mypeach.entity.ParserResult;
import org.breezee.mypeach.enums.SqlTypeEnum;
import org.breezee.mypeach.enums.TargetSqlParamTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@SpringBootTest
public class AutoSqlParseTest {
    String testFilePrefix = "src/test/java/org/breezee/mypeach/";

    @Autowired
    SqlParsers sqlParsers;

    @Test
    void selecet() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "01_Select.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        dicQuery.put("PROVINCE_ID","张三");
        dicQuery.put("#PROVINCE_CODE#","BJ");
        dicQuery.put("#PROVINCE_NAME#","北京");
        dicQuery.put("#DATE#","20222-02-10");
        //dicQuery.put("NAME",1);
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
        //SelectSqlParser sqlAnalyzer = new SelectSqlParser(new MyPeachProperties());
        //sqlParsers.properties.setTargetSqlParamTypeEnum(TargetSqlParamTypeEnum.DIRECT_RUN);//改变SQL生成方式
        ParserResult result = sqlParsers.parse(SqlTypeEnum.SELECT,sSql, dicQuery);
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
        //SelectSqlParser sqlAnalyzer = new SelectSqlParser(new MyPeachProperties());
        ParserResult result = sqlParsers.parse(SqlTypeEnum.SELECT,sSql, dicQuery);

        System.out.println(result.getCode().equals("0")?result.getSql():result.getMessage());//0转换成功，返回SQL；1转换失败，返回错误信息
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
        //InsertSqlParser sqlAnalyzer = new InsertSqlParser(new MyPeachProperties());
        ParserResult result = sqlParsers.parse(SqlTypeEnum.INSERT_VALUES,sSql, dicQuery);
        System.out.println(result.getCode().equals("0")?result.getSql():result.getMessage());//0转换成功，返回SQL；1转换失败，返回错误信息
    }

    @Test
    public void update() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "03_Update.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        dicQuery.put("PROVINCE_ID","张三");
        dicQuery.put("#PROVINCE_CODE#","BJ");
        //dicQuery.put("#PROVINCE_NAME#","北京");
        dicQuery.put("#TFLG#",1);
        //UpdateSqlParser sqlAnalyzer = new UpdateSqlParser(new MyPeachProperties());
        ParserResult result = sqlParsers.parse(SqlTypeEnum.UPDATE,sSql, dicQuery);
        System.out.println(result.getCode().equals("0")?result.getSql():result.getMessage());//0转换成功，返回SQL；1转换失败，返回错误信息
    }

    @Test
    void delete() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "04_Delete.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        dicQuery.put("PROVINCE_ID","张三");
        dicQuery.put("#PROVINCE_CODE#","BJ");
        dicQuery.put("#PROVINCE_NAME#","北京");
        dicQuery.put("#SORT_ID#",1);
        //DeleteSqlParser sqlAnalyzer = new DeleteSqlParser(new MyPeachProperties());
        ParserResult result = sqlParsers.parse(SqlTypeEnum.DELETE,sSql, dicQuery);
        System.out.println(result.getCode().equals("0")?result.getSql():result.getMessage());//0转换成功，返回SQL；1转换失败，返回错误信息
    }

    @Test
    void insertSelect() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "05_InsertSelect.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        dicQuery.put("PROVINCE_ID","张三");
        //dicQuery.put("#PROVINCE_CODE#","BJ");
        //dicQuery.put("#PROVINCE_NAME#","北京");
        dicQuery.put("#SORT_ID#",1);//必须
        dicQuery.put("#TFLAG#",1);
        //InsertSqlParser sqlAnalyzer = new InsertSqlParser(new MyPeachProperties());
        ParserResult result = sqlParsers.parse(SqlTypeEnum.INSERT_SELECT,sSql, dicQuery);
        System.out.println(result.getCode().equals("0")?result.getSql():result.getMessage());//0转换成功，返回SQL；1转换失败，返回错误信息
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
        //SelectSqlParser sqlAnalyzer = new SelectSqlParser(new MyPeachProperties());
        ParserResult result = sqlParsers.parse(SqlTypeEnum.SELECT_WITH_AS,sSql, dicQuery);
        System.out.println(result.getCode().equals("0")?result.getSql():result.getMessage());//0转换成功，返回SQL；1转换失败，返回错误信息
    }
}
