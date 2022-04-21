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
import java.util.HashMap;
import java.util.Map;

/**
 * @objectName:
 * @description:
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/20 22:44
 */
@SpringBootTest
public class InsertTest {
    String testFilePrefix = "src/test/Sql/Insert/";
    @Autowired
    SqlParsers sqlParsers;

    @Test
    void insert() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "01_Insert.txt")));
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
    void insertSelect() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "02_InsertSelect.txt")));
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
}
