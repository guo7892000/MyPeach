package org.breezee.mypeach.core;

import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.config.StaticConstants;
import org.breezee.mypeach.enums.SqlTypeEnum;
import org.breezee.mypeach.utils.ToolHelper;

import java.util.regex.Matcher;

/**
 * @objectName: 新增SQL分析器（OK）
 * @description: 针对Insert into的SQL分析，思路：
 * 1.根据正则式：)VALUES(匹配，把数据库列与赋值分开，得到两个字符串。并且把匹配部分加到值字符构建器中
 * 2.
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/12 16:45
 */
public class InsertSqlParser extends AbstractSqlParser {
    /**
     * 构造函数
     * @param properties
     */
    public InsertSqlParser(MyPeachProperties properties) {
        super(properties);
        sqlTypeEnum = SqlTypeEnum.INSERT_VALUES;
    }

    /**
     * 头部SQL转换
     * @param sSql
     * @return
     */
    @Override
    public String headSqlConvert(String sSql){
        StringBuilder sbHead = new StringBuilder();
        //针对WITH...INSERT INTO...SELECT...的处理
        sSql = withInsertIntoSelect(sSql, sbHead);
        if (ToolHelper.IsNull(sSql))
        {
            return sbHead.toString();//当是WITH...INSERT INTO...SELECT...方式，则方法会返回空
        }

        //针对INSERT INTO...WITH...SELECT...的处理
        sSql = insertIntoWithSelect(sSql, sbHead);
        if (ToolHelper.IsNull(sSql))
        {
            return sbHead.toString();//当是INSERT INTO...WITH...SELECT...方式，则方法会返回空
        }

        //针对INSERT INTO...VALUES和INSERT INTO...SELECT的处理
        return insertValueOrSelectConvert(sSql,sbHead);
    }

    /**
     * FROM前段SQL处理
     * @param sSql
     * @return
     */
    @Override
    protected String beforeFromConvert(String sSql) {
        StringBuilder sbHead = new StringBuilder();
        String[] colArray = sSql.split(",");
        for (int i = 0; i < colArray.length; i++) {
            String sLastAndOr = i==0 ? "":",";
            String colString = complexParenthesesKeyConvert(colArray[i],sLastAndOr);

            if(sqlTypeEnum == SqlTypeEnum.INSERT_SELECT && ToolHelper.IsNull(colString)){
                String sKeyName = getFirstKeyName(colArray[i]);
                mapError.put(sKeyName,"SELECT中的查询项"+sKeyName+"，其值必须转入，不能为空！");
            }
            sbHead.append(colString);
        }
        return sbHead.toString();
    }

    /// <summary>
    /// 针对SqlServer的withInsertIntoSelect
    /// </summary>
    /// <param name="sSql"></param>
    /// <returns></returns>
    private String withInsertIntoSelect(String sSql,StringBuilder sbHead) {;
        Matcher mc = ToolHelper.getMatcher(sSql, withInsertIntoSelectPartn);//抽取出INSERT INTO TABLE_NAME(部分
        while (mc.find()) {
            sqlTypeEnum = SqlTypeEnum.WITH_INSERT_SELECT;
            String sInsert = sSql.substring(0, mc.start()) + mc.group();
            sInsert = complexParenthesesKeyConvert(sInsert, "");
            sbHead.append(sInsert + System.lineSeparator());
            sSql = sSql.substring(mc.end()).trim();
            //UNION 或 UNION ALL的处理
            sSql = unionOrUnionAllConvert(sSql, sbHead);
            if (ToolHelper.IsNull(sSql)) {
                return "";
            }
            //非UNION 且 非UNION ALL的处理
            //FROM段处理
            String sFinalSql = fromWhereSqlConvert(sSql, false);
            sbHead.append(sFinalSql);
            sSql = "";//处理完毕清空SQL
        }
        return sSql;
    }

    /// <summary>
    /// 针对MySql、Oracle、PostgreSQL、SQLite的insertIntoWithSelect
    /// </summary>
    /// <param name="sSql"></param>
    /// <returns></returns>
    private String insertIntoWithSelect(String sSql, StringBuilder sbHead) {
        Matcher mc = ToolHelper.getMatcher(sSql, insertIntoWithSelectPartn);
        while (mc.find()) {
            sqlTypeEnum = SqlTypeEnum.INSERT_WITH_SELECT;
            String sInsert = sSql.substring(0, mc.start()) + mc.group();
            sInsert = complexParenthesesKeyConvert(sInsert, "");
            sbHead.append(sInsert + System.lineSeparator());
            sSql = sSql.substring(mc.end()).trim();
            //UNION 或 UNION ALL的处理
            sSql = unionOrUnionAllConvert(sSql, sbHead);
            if (ToolHelper.IsNull(sSql)) {
                return sbHead.toString();
            }
            //非UNION 且 非UNION ALL的处理
            //FROM段处理
            String sFinalSql = fromWhereSqlConvert(sSql, false);
            sbHead.append(sFinalSql);
            sSql = "";//处理完毕清空SQL
        }
        return sSql;
    }

    /**
     * INSERT INTO VALUES和INSERT INTO SELECT处理
     * @param sSql
     * @param sb
     * @return
     */
    private String insertValueOrSelectConvert(String sSql, StringBuilder sb){
        StringBuilder sbHead = new StringBuilder();
        StringBuilder sbTail = new StringBuilder();
        //1、抽取出INSERT INTO TABLE_NAME部分
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.insertIntoPattern);
        if (mc.find()){
            sbHead.append(mc.group());//加入INSERT INTO TABLE_NAME
            sSql = sSql.substring(mc.end()).trim();
        }

        //2、判断是否insert into ... values形式
        sSql = dealInsertItemAndValue(sSql, sbHead, sbTail);
        if(sSql.isEmpty())
        {
            sqlTypeEnum = SqlTypeEnum.INSERT_VALUES;
        }else{
            //4、INSERT INTO TABLE_NAME 。。 SELECT形式
            mc = ToolHelper.getMatcher(sSql, StaticConstants.commonSelectPattern);
            if (mc.find())
            {
                sqlTypeEnum = SqlTypeEnum.INSERT_SELECT;
                String sInsert = sSql.substring(0, mc.start());
                sInsert = complexParenthesesKeyConvert(sInsert, "");
                sbHead.append(sInsert);
                sSql = mc.group() + sSql.substring(mc.end()).trim();
                //UNION 或 UNION ALL的处理
                sSql = unionOrUnionAllConvert(sSql, sbHead);
                if (ToolHelper.IsNull(sSql))
                {
                    return sbHead.toString();
                }
                //非UNION 且 非UNION ALL的处理
                //FROM段处理
                String sFinalSql = fromWhereSqlConvert(sSql, false);
                sbHead.append(sFinalSql);
            }
            else
            {
                sqlTypeEnum = SqlTypeEnum.Unknown;
            }
            return sbHead.toString();
        }
        sb.append(sbHead.toString()+sbTail.toString());
        return sb.toString();
    }

    /**
     * 是否正确SQL类型实现方法
     * @param sSql
     * @return
     */
    @Override
    public  boolean isRightSqlType(String sSql)
    {
        Matcher mc = ToolHelper.getMatcher(sSql, withInsertIntoSelectPartn);
        if (mc.find())
        {
            return true;
        }
        mc = ToolHelper.getMatcher(sSql, insertIntoWithSelectPartn);
        if (mc.find())
        {
            return true;
        }
        //Insert into...
        mc = ToolHelper.getMatcher(sSql, StaticConstants.insertIntoPattern);
        if (mc.find()){
            mc = ToolHelper.getMatcher(sSql, StaticConstants.valuesPattern);
            if (mc.find())
            {
                return true;
            }
            mc = ToolHelper.getMatcher(sSql, StaticConstants.commonSelectPattern);
            if (mc.find())
            {
                return true;
            }
        }
        return false;
    }
}
