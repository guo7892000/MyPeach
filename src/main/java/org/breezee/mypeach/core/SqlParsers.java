package org.breezee.mypeach.core;

import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.entity.ParserResult;
import org.breezee.mypeach.enums.SqlTypeEnum;
import org.breezee.mypeach.enums.TargetSqlParamTypeEnum;

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

    public MyPeachProperties properties;

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
    public ParserResult parse(SqlTypeEnum sqlType, String sSql, Map<String, Object> dic, TargetSqlParamTypeEnum paramTypeEnum){
        switch (sqlType){
            case INSERT_VALUES:
            case INSERT_SELECT:
                return new InsertSqlParser(properties).parse(sSql,dic,paramTypeEnum);
            case UPDATE:
                return new UpdateSqlParser(properties).parse(sSql,dic,paramTypeEnum);
            case DELETE:
                return new DeleteSqlParser(properties).parse(sSql,dic,paramTypeEnum);
            case SELECT:
            case SELECT_WITH_AS:
            default:
                return new SelectSqlParser(properties).parse(sSql,dic,paramTypeEnum);
        }
    }

    public ParserResult parse(SqlTypeEnum sqlType, String sSql, Map<String, Object> dic){
        return parse(sqlType,sSql,dic,TargetSqlParamTypeEnum.NameParam);
    }

}
