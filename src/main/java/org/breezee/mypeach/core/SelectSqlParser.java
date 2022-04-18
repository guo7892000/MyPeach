package org.breezee.mypeach.core;

import lombok.extern.slf4j.Slf4j;
import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.enums.SqlTypeEnum;
import org.breezee.mypeach.utils.ToolHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

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
    //针对Oracle中以WITH开头的特殊查询，例如：with table_tmp as (),with table_tmp2 as () SELECT 。。。
    private final String sOracleWithSelectPartn = "\\)?\\s*,?\\s*WITH\\s+\\w+\\s+AS\\s*\\(";
    //【)SELECT】部分正则式，找出之后SELECT语句
    private final String sOracleWithSelectPartnToSelect = "\\)\\s*SELECT\\s+";

    public SelectSqlParser(MyPeachProperties properties) {
        super(properties);
        sqlTypeEnum = SqlTypeEnum.SELECT;
    }

    @Override
    protected String headSqlConvert(String sSql) {
        sSql = OracleWithSelectConvert(sSql);
        //通用的以Select开头的处理
        return queryHeadSqlConvert(sSql);
    }

    /**
     * 针对Oracle中以WITH开头的特殊查询的转换
     * @param sSql
     * @return
     */
    private String OracleWithSelectConvert(String sSql) {
        StringBuilder sbHead = new StringBuilder();
        Pattern regex = Pattern.compile(sOracleWithSelectPartn,CASE_INSENSITIVE);
        Matcher mc = regex.matcher(sSql);
        int iStart = 0;
        while (mc.find()) {
            sqlTypeEnum = SqlTypeEnum.SELECT_WITH_AS;
            String sOneSql = sSql.substring(iStart,mc.start()).trim();
            if(ToolHelper.IsNotNull(sOneSql)){
                //通用的以Select开头的处理
                sbHead.append(queryHeadSqlConvert(sOneSql));
            }
            sbHead.append(mc.group());
            iStart = mc.end();
        }
        if(iStart>0) {
            sSql = sSql.substring(iStart).trim();//去掉之前处理过的部分
            //匹配【)SELECT】部分
            regex = Pattern.compile(sOracleWithSelectPartnToSelect,CASE_INSENSITIVE);
            mc = regex.matcher(sSql);
            while (mc.find()) {
                String sOneSql = sSql.substring(0,mc.start()).trim();
                //通用的以Select开头的处理
                sbHead.append(queryHeadSqlConvert(sOneSql));

                sSql = sSql.substring(mc.end() - mc.group().length() + 1).trim();
                sbHead.append(")" + System.lineSeparator());
            }
            return sbHead.toString();
        }
        return sSql;
    }

    @Override
    protected String beforeFromConvert(String sSql) {
        return queryBeforeFromConvert(sSql);
    }


}
