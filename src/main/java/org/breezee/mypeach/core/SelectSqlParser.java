package org.breezee.mypeach.core;

import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.config.StaticConstants;
import org.breezee.mypeach.enums.SqlTypeEnum;
import org.breezee.mypeach.utils.ToolHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * @objectName:
 * @description:
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/12 16:45
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
        sSql = withSelectConvert(sSql,sbHead);
        //UNION和UNION ALL处理
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.unionAllPartner);
        int iStart=0;
        while(mc.find()){
            String sOne = sSql.substring(iStart,mc.start());
            String sConvertSql = queryHeadSqlConvert(sOne,false);
            sbHead.append(sConvertSql);
            iStart = mc.end();
            sbHead.append(mc.group());
        }
        if(iStart>0){
            String sOne = sSql.substring(iStart);
            String sConvertSql = queryHeadSqlConvert(sOne,false);
            sbHead.append(sConvertSql);
        }else {
            String sConvertSql = queryHeadSqlConvert(sSql,false);
            sbHead.append(sConvertSql);//通用的以Select开头的处理
        }
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
        if(mc.find()) {
            sqlTypeEnum = SqlTypeEnum.SELECT_WITH_AS;
            String sOneSql = complexParenthesesKeyConvert(mc.group(),"");//##序号##处理
            sbHead.append(sOneSql);
            iStart = mc.end();
        }else{
            sqlTypeEnum = SqlTypeEnum.Unknown;
        }
        if(iStart>0) {
            sbHead.append(System.lineSeparator());
            sSql = sSql.substring(iStart).trim();//去掉之前处理过的部分
            sSql = queryHeadSqlConvert(sSql,true);//通用的以Select开头的处理
        }
        return sSql;//还需要处理的SQL
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
        return false;
    }
}
