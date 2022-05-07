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
public class DeleteTest {
    String testFilePrefix = "src/test/Sql/Delete/";
    @Autowired
    SqlParsers sqlParsers;

    @Test
    void delete() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "01_Delete.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        dicQuery.put("PROVINCE_ID","张三");
        //dicQuery.put("#CREATOR#","BJ");
        dicQuery.put("#PROVINCE_NAME#","北京");
        dicQuery.put("#REMARK#","2'--2");
        //dicQuery.put("BF","BFFFF");

        //sqlParsers.properties.setTargetSqlParamTypeEnum(TargetSqlParamTypeEnum.DIRECT_RUN);
        //sqlParsers.properties.setStopDeleteNoConditon(false);
        ParserResult result = sqlParsers.parse(SqlTypeEnum.DELETE,sSql, dicQuery);
        System.out.println(result.getCode().equals("0")?result.getSql():result.getMessage());//0转换成功，返回SQL；1转换失败，返回错误信息
    }

}
