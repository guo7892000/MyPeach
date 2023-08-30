package org.breezee.mypeach;

import org.breezee.mypeach.core.SqlParsers;
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

/**
 * @objectName:
 * @description:
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/20 22:44
 */
@SpringBootTest
public class SelectTest {
    String testFilePrefix = "src/test/Sql/Select/";
    @Autowired
    SqlParsers sqlParsers;

    @Test
    void selecet() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "01_Select.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        //dicQuery.put("PROVINCE_ID","张三");
        dicQuery.put("#PROVINCE_CODE#","BJ");
        dicQuery.put("#PROVINCE_NAME#","北京");
        dicQuery.put("#DATE#","20222-02-10");
        dicQuery.put("NAME",1);
        dicQuery.put("SORT_ID",20);
        dicQuery.put("#REMARK#","测试");
        dicQuery.put("PAGE_SIZE",8);
        //dicQuery.put("TNAME","TF");
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
        sqlParsers.properties.setShowDebugSql(true);
        sqlParsers.properties.setLogSqlPath("E:\\mypeach");
        ParserResult result = sqlParsers.parse(SqlTypeEnum.SELECT,sSql, dicQuery);
        //System.out.println(result.getMessage());
        //System.out.println(result.getSql());
    }

    @Test
    void withSelecet() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "02_WithSelect.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        dicQuery.put("#REMARK#","'测试'");
        dicQuery.put("PROVINCE_ID","张三");
        dicQuery.put("#PROVINCE_CODE#","BJ");
        dicQuery.put("#PROVINCE_NAME#","北京");
        dicQuery.put("#DATE#","20222-02-10");
        dicQuery.put("NAME",1);
        dicQuery.put("CREATOR","back");
        dicQuery.put("CREATOR1","back");
        dicQuery.put("GF","back");
        //SelectSqlParser sqlAnalyzer = new SelectSqlParser(new MyPeachProperties());
        ParserResult result = sqlParsers.parse(SqlTypeEnum.SELECT_WITH_AS,sSql, dicQuery);
        System.out.println(result.getCode().equals("0")?result.getSql():result.getMessage());//0转换成功，返回SQL；1转换失败，返回错误信息
    }

    @Test
    void selecetUnion() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "03_SelectUnion.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        dicQuery.put("PROVINCE_ID","张三");
        dicQuery.put("#PROVINCE_CODE#","BJ");
        dicQuery.put("#PROVINCE_NAME#","北京");
        dicQuery.put("#DATE#","20222-02-10");
        dicQuery.put("NAME",1);
        dicQuery.put("TFLG",1);
        dicQuery.put("#REMARK#","测试");
        dicQuery.put("TNAME","222");
        //dicQuery.put("BF","back");
        //dicQuery.put("MDLIST",new String[]{"SE","PA","FI"});//传入一个数组
//        List<String> list = new ArrayList<String>();
//        list.add("'SE'");
//        list.add("VE");
//        list.add("UC");

        List<Integer> list = new ArrayList<Integer>();
        list.addAll(Arrays.asList(2,3,4));
        dicQuery.put("MDLIST",list);//传入一个数组

        //sqlParsers.properties.setTargetSqlParamTypeEnum(TargetSqlParamTypeEnum.DIRECT_RUN);//改变SQL生成方式
        ParserResult result = sqlParsers.parse(SqlTypeEnum.SELECT,sSql, dicQuery);
        System.out.println(result.getMessage());
        System.out.println(result.getSql());
    }

    @Test
    void selecetParamType() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "01_Select.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        //dicQuery.put("PROVINCE_ID","张三");
        dicQuery.put("#PROVINCE_CODE#","BJ");
        dicQuery.put("#PROVINCE_NAME#","北京");
        dicQuery.put("#DATE#","20222-02-10");
        dicQuery.put("NAME",1);
        dicQuery.put("SORT_ID",20);
        dicQuery.put("#REMARK#","测试");
        dicQuery.put("PAGE_SIZE",8);

        sqlParsers.properties.setShowDebugSql(true);
        //sqlParsers.properties.setTargetSqlParamTypeEnum(TargetSqlParamTypeEnum.PostionParam);
        //sqlParsers.properties.setLogSqlPath("E:\\mypeach");
        ParserResult result = sqlParsers.parse(SqlTypeEnum.SELECT,sSql, dicQuery, TargetSqlParamTypeEnum.PostionParam);
        //System.out.println(result.getMessage());
        System.out.println(result.getSql());
    }

    @Test
    void InList() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "05_InList.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        //dicQuery.put("PROVINCE_ID","张三");
        dicQuery.put("#PROVINCE_CODE#","BJ");
        dicQuery.put("#PROVINCE_NAME#","北京");
        dicQuery.put("#DATE#","20222-02-10");
        dicQuery.put("NAME",1);
        dicQuery.put("SORT_ID",20);
        dicQuery.put("#REMARK#","测试");
        dicQuery.put("PAGE_SIZE",8);

        List<Integer> list = new ArrayList<>();
        List<Integer> list2 = new ArrayList<>();
        List<String> list3 = new ArrayList<>();
        for (int i = 0; i < 10; i++)
        {
            list.add(i);
            list2.add(i);
        }
        list3.add("10");
        list3.add("10000");
        list3.add("10001");
        list3.add("10400");

        dicQuery.put("PROVINCE_ID_LIST", list);//传入一个数组
        dicQuery.put("CITY_ID_LIST", list2);//传入一个数组
        dicQuery.put("PROVINCE_CODE_LIST", list3);//传入一个数组

        sqlParsers.properties.setShowDebugSql(true);
        //sqlParsers.properties.setTargetSqlParamTypeEnum(TargetSqlParamTypeEnum.PostionParam);
        //sqlParsers.properties.setLogSqlPath("E:\\mypeach");
        ParserResult result = sqlParsers.parse(SqlTypeEnum.SELECT,sSql, dicQuery);
        //System.out.println(result.getMessage());
        System.out.println(result.getSql());
    }

}
