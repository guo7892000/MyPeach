package org.breezee.mypeach.core;

import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.config.StaticConstants;
import org.breezee.mypeach.entity.ParserResult;
import org.breezee.mypeach.entity.SqlKeyValueEntity;
import org.breezee.mypeach.enums.SqlTypeEnum;
import org.breezee.mypeach.enums.TargetSqlParamTypeEnum;
import org.breezee.mypeach.utils.ToolHelper;

import java.util.Map;
import java.util.regex.Matcher;

/**
 * @objectName:
 * @description:
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/16 22:54
 * @history:
 *   2023/07/20 BreezeeHui 增加预获取参数方法，方便测试参数赋值。
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

    /***
     * @param sSql  需要自动化转换的SQL
     * @param dic   SQL语句中键的值
     * @return 根据传入的动态条件转换为动态的SQL
     */
    public ParserResult parse(String sSql, Map<String, Object> dic, TargetSqlParamTypeEnum paramTypeEnum)
    {
        return GetParser(sSql).parse(sSql, dic, paramTypeEnum);
    }

    public ParserResult parse(SqlTypeEnum sqlType, String sSql, Map<String, Object> dic){
        return parse(sqlType,sSql,dic,TargetSqlParamTypeEnum.NameParam);
    }

    public Map<String, SqlKeyValueEntity> PreGetParam(String sSql)
    {
        return GetParser(sSql).PreGetParam(sSql);
    }

    private AbstractSqlParser GetParser(String sSql)
    {
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.insertIntoPattern);
        if (mc.find())
        {
            return new InsertSqlParser(properties);
        }
        mc = ToolHelper.getMatcher(sSql, StaticConstants.updateSetPattern);//先截取UPDATE SET部分
        if (mc.find())
        {
            return new UpdateSqlParser(properties);
        }
        mc = ToolHelper.getMatcher(sSql, StaticConstants.deletePattern);//抽取出INSERT INTO TABLE_NAME(部分
        if (mc.find())
        {
            return new DeleteSqlParser(properties);
        }
        return new SelectSqlParser(properties);
    }
}
