package org.breezee.mypeach.core;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class SelectSqlParser extends AbstractSqlParser {

    public SelectSqlParser(MyPeachProperties properties) {
        super(properties);
        sqlTypeEnum = SqlTypeEnum.SELECT;
    }

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
     * 针对Oracle中以WITH开头的特殊查询的转换
     * @param sSql
     * @return
     */
    private String withSelectConvert(String sSql, StringBuilder sbHead) {
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.withSelectPartn);
        int iStart = 0;
        while (mc.find()) {
            sqlTypeEnum = SqlTypeEnum.SELECT_WITH_AS;
            String sOneSql = sSql.substring(iStart,mc.start()).trim();
            if(ToolHelper.IsNotNull(sOneSql)){
                //通用的以Select开头的处理
                sbHead.append(queryHeadSqlConvert(sOneSql,true));
            }
            sbHead.append(mc.group());
            iStart = mc.end();
        }
        if(iStart>0) {
            sSql = sSql.substring(iStart).trim();//去掉之前处理过的部分
            //匹配【)SELECT】部分
            mc = ToolHelper.getMatcher(sSql, StaticConstants.withSelectPartnToSelect);
            while (mc.find()) {
                String sOneSql = sSql.substring(0,mc.start()).trim();
                //通用的以Select开头的处理
                sbHead.append(queryHeadSqlConvert(sOneSql,true));

                sSql = sSql.substring(mc.end() - mc.group().length() + 1).trim();
                sbHead.append(")" + System.lineSeparator());
            }
        }
        return sSql;
    }

    @Override
    protected String beforeFromConvert(String sSql) {
        return queryBeforeFromConvert(sSql);
    }

}
