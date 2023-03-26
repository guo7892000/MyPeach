package org.breezee.mypeach;

import org.breezee.mypeach.core.SqlParsers;
import org.breezee.mypeach.entity.ParserResult;
import org.breezee.mypeach.enums.SqlTypeEnum;
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
 * @date: 2022/4/20 22:45
 */
@SpringBootTest
public class SelectDynamicTest {
    String testFilePrefix = "src/test/Sql/SelectDynamic/";
    @Autowired
    SqlParsers sqlParsers;

    @Test
    void selecet1() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "01_Select.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        dicQuery.put("PROVINCE_ID","张三");
        dicQuery.put("PROVINCE_CODE","BJ");
        dicQuery.put("PROVINCE_NAME","北京");
        dicQuery.put("DATE","20222-02-10");
        dicQuery.put("ID",1);
        dicQuery.put("REMARK","测试");
        //dicQuery.put("BF","back");
        dicQuery.put("MDLIST",new String[]{"SE","PA","FI"});//传入一个数组
//        List<String> list = new ArrayList<String>();
//        list.add("'SE'");
//        list.add("VE");
//        list.add("UC");

        //List<Integer> list = new ArrayList<Integer>();
        //list.addAll(Arrays.asList(2,3,4));
//        dicQuery.put("MDLIST",list);//传入一个数组
        //SelectSqlParser sqlAnalyzer = new SelectSqlParser(new MyPeachProperties());
        //sqlParsers.properties.setKeyStyle(SqlKeyStyleEnum.POUND_SIGN_BRACKETS);//设置为#{}模式
        ParserResult result = sqlParsers.parse(SqlTypeEnum.SELECT,sSql, dicQuery);

        System.out.println(result.getCode().equals("0")?result.getSql():result.getMessage());//0转换成功，返回SQL；1转换失败，返回错误信息
    }

    @Test
    void Select2()
    {
        Map<String, Object> dicQuery = new HashMap<>();
        String sSql = "SELECT * FROM BAS_CITY T WHERE T.CITY_ID = #CITY_ID#";
        dicQuery.put("PROVINCE_ID", "张三");
        dicQuery.put("PROVINCE_CODE", "BJ");
        dicQuery.put("PROVINCE_NAME", "北京");
        ParserResult result = sqlParsers.parse(SqlTypeEnum.SELECT, sSql, dicQuery);
        System.out.println(result.getCode().equals("0")?result.getSql():result.getMessage());//0转换成功，返回SQL；1转换失败，返回错误信息
    }
}
