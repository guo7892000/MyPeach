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
            case WITH_INSERT_SELECT:
            case INSERT_WITH_SELECT:
                return new InsertSqlParser(properties).parse(sSql,dic,paramTypeEnum);
            case UPDATE:
                return new UpdateSqlParser(properties).parse(sSql,dic,paramTypeEnum);
            case DELETE:
                return new DeleteSqlParser(properties).parse(sSql,dic,paramTypeEnum);
            case CommonMerge:
                return new CommonSqlParser(properties).parse(sSql, dic, paramTypeEnum);
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
    public ParserResult parse(String sSql, Map<String, Object> dic, TargetSqlParamTypeEnum paramTypeEnum) throws Exception
    {
        AbstractSqlParser sqlParser = GetParser(sSql, dic);
        return sqlParser.parse(sSql, dic, paramTypeEnum);//为方便调试，这里拆成两行
    }

    public ParserResult parse(SqlTypeEnum sqlType, String sSql, Map<String, Object> dic){
        return parse(sqlType,sSql,dic,TargetSqlParamTypeEnum.NameParam);
    }

    public Map<String, SqlKeyValueEntity> PreGetParam(String sSql, Map<String, Object> dic) throws Exception {
        AbstractSqlParser sqlParser = GetParser(sSql, dic);
        return sqlParser.PreGetParam(sSql, dic); //为方便调试，这里拆成两行
    }

    private AbstractSqlParser GetParser(String sSql, Map<String, Object> dic) throws Exception {
        AbstractSqlParser parser = new SelectSqlParser(properties);
        //去掉注释
        sSql = parser.RemoveSqlRemark(sSql, dic,false);
        //将SQL中的()替换为##序号##，方便从整体上分析SQL类型
        sSql = parser.generateParenthesesKey(sSql);
        //根据SQL的正则，再重新返回正确的SqlParser
        if (parser.isRightSqlType(sSql))
        {
            return parser;
        }
        parser = new UpdateSqlParser(properties);
        if (parser.isRightSqlType(sSql))
        {
            return parser;
        }
        parser = new DeleteSqlParser(properties);
        if (parser.isRightSqlType(sSql))
        {
            return parser;
        }
        parser = new CommonSqlParser(properties);
        if (parser.isRightSqlType(sSql))
        {
            return parser;
        }
        //Insert必须要放在Merge后面，因为两者都有Values，会误把Merge当作Insert
        parser = new InsertSqlParser(properties);
        if (parser.isRightSqlType(sSql))
        {
            return parser;
        }
        throw new Exception("不支持的SQL类型，请将SQL发给作者（guo7892000@126.com），后续版本增加支持！！");
    }
}
