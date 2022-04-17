package org.breezee.mypeach.core;

import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.entity.ParserResult;
import org.breezee.mypeach.enums.SqlTypeEnum;

import java.util.Map;

/**
 * @objectName:
 * @description:
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/16 22:54
 */
public class SqlParsers {

    private MyPeachProperties properties;

    //构造函数
    public SqlParsers(MyPeachProperties prop){
        properties = prop;
    }

    /**
     * 获取SQL
     * @param sqlType SQL语句类型
     * @param sSql  需要自动化转换的SQL
     * @param dic   SQL语句中键的值
     * @return 根据传入的动态条件转换为动态的SQL
     */
    public ParserResult getSql(SqlTypeEnum sqlType, String sSql, Map<String, Object> dic){
        switch (sqlType){
            case INSERT:
                return new InsertSqlParser(properties).parse(sSql,dic);
            case UPDATE:
                return new UpdateSqlParser(properties).parse(sSql,dic);
            case DELETE:
                return new DeleteSqlParser(properties).parse(sSql,dic);
            case SELECT:
            default:
                return new SelectSqlParser(properties).parse(sSql,dic);
        }
    }
}
