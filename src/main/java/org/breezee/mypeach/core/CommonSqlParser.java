package org.breezee.mypeach.core;

import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.config.StaticConstants;
import org.breezee.mypeach.enums.SqlTypeEnum;
import org.breezee.mypeach.utils.ToolHelper;

import java.util.regex.Matcher;

/**
 * @objectName:Delete Sql Analyzer(删除SQL分析器)
 * @description:
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/12 16:45
 */
public class CommonSqlParser extends AbstractSqlParser {

    /**
     * 构造函数
     * @param properties
     */
    public CommonSqlParser(MyPeachProperties properties) {
        super(properties);
        sqlTypeEnum = SqlTypeEnum.CommonMerge;
    }

    /**
     * 头部SQL转换
     * @param sSql
     * @return
     */
    @Override
    public String headSqlConvert(String sSql) {
        StringBuilder sbHead = new StringBuilder();
        StringBuilder sbTail = new StringBuilder();
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.mergePatternHead);
        if(mc.find()) {
            sqlTypeEnum = SqlTypeEnum.CommonMerge;
            sbHead.append(mc.group());//不变的部分先加入
            sSql = sSql.substring(mc.end());
        } else {
            sqlTypeEnum = SqlTypeEnum.Unknown;
            return sSql;
        }

        //
        String sMergeBefore = "";
        int i = 0;
        int iGroupStart = 0;
        mc = ToolHelper.getMatcher(sSql, StaticConstants.mergePatternMatchOrNot);
        while (mc.find()) {
            if (i == 0) {
                sMergeBefore = sSql.substring(0, mc.start());
            }
            if (iGroupStart > 0) {
                //得到要处理的内容
                String sNeedDeal = sSql.substring(iGroupStart, mc.start()).trim();
                sbTail.append(matchSqlDetal(sNeedDeal) + System.lineSeparator());
            }
            sbTail.append(mc.group());
            iGroupStart = mc.end();
            i++;
        }
        //最后部分的处理
        if (iGroupStart > 0) {
            //得到要处理的内容
            String sNeedDeal = sSql.substring(iGroupStart).trim();
            sbTail.append(matchSqlDetal(sNeedDeal));
        }
        //sMergeBefore中的处理
        mc = ToolHelper.getMatcher(sSql, "\\s+ON\\s+");
        if (mc.find()) {
            //只会有一个ON
            String sOnBefore = sMergeBefore.substring(0, mc.start());
            sOnBefore = complexParenthesesKeyConvert(sOnBefore,"");//ON前面先使用复杂##参数##解析
            sbHead.append(sOnBefore);
            sbHead.append(mc.group()); //ON部分字符加入
            String sOnAfter = sMergeBefore.substring(mc.end());
            sbHead.append(andOrConditionConvert(sOnAfter));//ON后面为AND条件转换
        }
        return sbHead.toString() + sbTail.toString();
    }

    private String matchSqlDetal(String sSql) {
        StringBuilder sbTail = new StringBuilder();
        Matcher mcUpdate = ToolHelper.getMatcher(sSql, "UPDATE\\s+SET\\s+");
        Matcher mcDelete = ToolHelper.getMatcher(sSql, "DELETE\\s+");
        Matcher mcInsert = ToolHelper.getMatcher(sSql, "INSERT\\s+");
        if (mcUpdate.find()) {
            sbTail.append(mcUpdate.group());
            sbTail.append(dealUpdateSetItem(sSql));
            return sbTail.toString();
        }
        else if (mcDelete.find()) {
            sbTail.append(mcDelete.group()); //未确定的TODO：删除还有其他条件吗？
            return sbTail.toString();
        }
        else if (mcInsert.find()) {
            sbTail.append(mcInsert.group());
            StringBuilder sbHeadNew = new StringBuilder();
            StringBuilder sbTailNew = new StringBuilder();
            sSql= sSql.substring(mcInsert.end());
            sSql = dealInsertItemAndValue(sSql, sbHeadNew, sbTailNew);
            sbTail.append(sbHeadNew);
            sbTail.append(sbTailNew);
            return sbTail.toString();
        }
        else {
            //throw new Exception("未处理的MatchOrNot类型！");
            return sSql;//找不到符合项
        }
    }

    /**
     * FROM前段SQL处理
     * @param sSql
     * @return
     */
    @Override
    protected String beforeFromConvert(String sSql) {
        return "";
    }

    /**
     * 是否正确SQL类型实现方法
     * @param sSql
     * @return
     */
    @Override
    public boolean isRightSqlType(String sSql)
    {
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.mergePatternHead);
        return mc.find();
    }
}
