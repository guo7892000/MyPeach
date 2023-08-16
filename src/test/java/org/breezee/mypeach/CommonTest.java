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
 * @date: 2022/4/20 22:45
 */
@SpringBootTest
public class CommonTest {
    String testFilePrefix = "src/test/Sql/Common/";
    @Autowired
    SqlParsers sqlParsers;

    @Test
    void mergeTest() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "01_MergeInto.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        dicQuery.put("USER_ID","张三");
        dicQuery.put("#AUTH_TYPE#","3");

        //sqlParsers.properties.setTargetSqlParamTypeEnum(TargetSqlParamTypeEnum.DIRECT_RUN);
        //sqlParsers.properties.setStopDeleteNoConditon(false);
        ParserResult result = sqlParsers.parse(SqlTypeEnum.CommonMerge,sSql, dicQuery);
        System.out.println(result.getCode().equals("0")?result.getSql():result.getMessage());//0转换成功，返回SQL；1转换失败，返回错误信息

    }

}
