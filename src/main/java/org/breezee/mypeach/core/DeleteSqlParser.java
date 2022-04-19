package org.breezee.mypeach.core;

import lombok.extern.slf4j.Slf4j;
import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.enums.SqlTypeEnum;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @objectName:Delete Sql Analyzer(删除SQL分析器)
 * @description:
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/12 16:45
 */
@Slf4j
public class DeleteSqlParser extends AbstractSqlParser {
    String sDeletePattern = "^DELETE\\s+FROM\\s+\\S+\\s+"; //正则式:DELETE FROM TABALE_NAME

    public DeleteSqlParser(MyPeachProperties properties) {
        super(properties);
        sqlTypeEnum = SqlTypeEnum.DELETE;
    }

    @Override
    public String headSqlConvert(String sSql) {
        StringBuilder sb = new StringBuilder();
        Pattern regex = Pattern.compile(sDeletePattern);//抽取出INSERT INTO TABLE_NAME(部分
        Matcher mc = regex.matcher(sSql);
        while (mc.find()){
            sb.append(mc.group());//不变的INSERT INTO TABLE_NAME(部分先加入
            //FROM部分SQL处理
            sb.append(fromSqlConvert(sSql.substring(mc.end())));
        }
        return sb.toString();
    }

    @Override
    protected String beforeFromConvert(String sSql) {
        return "";
    }

}
