package org.breezee.mypeach.core;

import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.config.StaticConstants;
import org.breezee.mypeach.enums.SqlTypeEnum;
import org.breezee.mypeach.utils.ToolHelper;

import java.util.regex.Matcher;

/**
 * @objectName:
 * @description:
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/12 16:45
 * @history:
 *   2023/08/25 BreezeeHui 抽取UnionOrUnionAllConvert方法；在WithSelectConvert也要增加UnionOrUnionAllConvert处理；修正With临时表的正则中WITH为(WITH)*，
 *      因为后面的临时表是不用加WITH的；匹配UNION或UNION ALL时使用while，而不是if，因为会有多个UNION或UNION ALL。
 */
public class SelectSqlParser extends AbstractSqlParser {
    /**
     * 构造函数
     * @param properties
     */
    public SelectSqlParser(MyPeachProperties properties) {
        super(properties);
        sqlTypeEnum = SqlTypeEnum.SELECT;
    }

    /**
     * 头部SQL转换
     * @param sSql
     * @return
     */
    @Override
    protected String headSqlConvert(String sSql) {
        StringBuilder sbHead = new StringBuilder();
        //with...as...select的处理
        sSql = withSelectConvert(sSql, sbHead);
        if (ToolHelper.IsNull(sSql))
        {
            return sbHead.toString();//当是WITH...INSERT INTO...SELECT...方式且已处理，则返回处理过的SQL
        }
        //UNION 或 UNION ALL的处理
        sSql = unionOrUnionAllConvert(sSql, sbHead);
        if (ToolHelper.IsNull(sSql))
        {
            return sbHead.toString();
        }
        //正常的SELECT处理
        String sConvertSql = queryHeadSqlConvert(sSql, false);
        sbHead.append(sConvertSql);//通用的以Select开头的处理
        return sbHead.toString();
    }

    /**
     * 以WITH开头的特殊查询的转换
     * @param sSql
     * @param sbHead
     * @return
     */
    private String withSelectConvert(String sSql, StringBuilder sbHead) {
        Matcher mc = ToolHelper.getMatcher(sSql, withSelectPartn);
        int iStart = 0;
        while (mc.find()) {
            //因为会存在多个临时表，所以这里必须用while
            sqlTypeEnum = SqlTypeEnum.SELECT_WITH_AS;
            String sOneSql = complexParenthesesKeyConvert(mc.group(), "");//##序号##处理
            sbHead.append(sOneSql);
            iStart = mc.end();
        }
        if (iStart > 0) {
            //处理with...select剩余部分SQL
            sbHead.append(System.lineSeparator());
            sSql = sSql.substring(iStart).trim();//去掉之前处理过的部分
            //with...select...也存在UNION或UNION ALL的情况，所以这里要调用UNION或UNION ALL处理
            sSql = unionOrUnionAllConvert(sSql, sbHead);
            if (ToolHelper.IsNull(sSql)) {
                return "";
            }
            else {
                //非UNION且非UNION ALL的处理
                String sConvertSql = queryHeadSqlConvert(sSql, false);
                sbHead.append(sConvertSql);//通用的以Select开头的处理
                return "";
            }
        } else {
            return sSql;//返回未处理的SQL
        }
    }

    /**
     * FROM前段SQL处理
     * @param sSql
     * @return
     */
    @Override
    protected String beforeFromConvert(String sSql) {
        return queryBeforeFromConvert(sSql);
    }

    /**
     * 是否正确SQL类型实现方法
     * @param sSql
     * @return
     */
    @Override
    public  boolean isRightSqlType(String sSql)
    {
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.unionAllPartner);
        if (mc.find())
        {
            return true;
        }
        mc = ToolHelper.getMatcher(sSql, withSelectPartn);
        if (mc.find())
        {
            return true;
        }
        mc = ToolHelper.getMatcher(sSql, StaticConstants.selectPattern);
        if (mc.find())
        {
            return true;
        }
        return false;
    }
}
