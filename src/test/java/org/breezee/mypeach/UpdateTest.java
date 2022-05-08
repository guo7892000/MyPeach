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
public class UpdateTest {
    String testFilePrefix = "src/test/Sql/Update/";
    @Autowired
    SqlParsers sqlParsers;

    @Test
    public void update() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "01_Update.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        dicQuery.put("PROVINCE_ID","张三");
        dicQuery.put("#PROVINCE_CODE#","BJ");
        //dicQuery.put("#PROVINCE_NAME#","北京");
        dicQuery.put("#TFLG#",1);
        dicQuery.put("MODIFIER","lisi");
        //UpdateSqlParser sqlAnalyzer = new UpdateSqlParser(new MyPeachProperties());
        ParserResult result = sqlParsers.parse(SqlTypeEnum.UPDATE,sSql, dicQuery);
        System.out.println(result.getCode().equals("0")?result.getSql():result.getMessage());//0转换成功，返回SQL；1转换失败，返回错误信息
    }
}
