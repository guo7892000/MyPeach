package org.breezee.mypeach.core;

import lombok.extern.slf4j.Slf4j;
import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.config.StaticConstants;
import org.breezee.mypeach.entity.SqlSegment;
import org.breezee.mypeach.enums.SqlSegmentEnum;
import org.breezee.mypeach.enums.SqlTypeEnum;
import org.breezee.mypeach.utils.ToolHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * @objectName: 更新SQL分析器
 * @description: 针对UPDATE SET的SQL分析，思路：
 * 1.根据正则式：)VALUES(匹配，把数据库列与赋值分开，得到两个字符串。并且把匹配部分加到值字符构建器中
 * 2.
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/12 16:45
 */
public class UpdateSqlParser extends AbstractSqlParser {
    public UpdateSqlParser(MyPeachProperties properties) {
        super(properties);
        sqlTypeEnum = SqlTypeEnum.UPDATE;
    }

    @Override
    public String headSqlConvert(String sSql) {
        StringBuilder sb = new StringBuilder();
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.updateSetPattern);//先截取UPDATE SET部分
        while (mc.find()){
            sb.append(mc.group());//不变的UPDATE SET部分先加入
            sSql = sSql.substring(mc.end()).trim();
            //调用From方法
            sb.append(fromWhereSqlConvert(sSql));
        }
        return sb.toString();
    }

    protected String beforeFromConvert(String sSql){
        StringBuilder sb = new StringBuilder();
        String[] sSetArray = sSql.split(",");
        String sComma="";
        for (String col:sSetArray) {
            if(!hasKey(col)){
                sb.append(sComma + col);
                sComma = ",";
                continue;
            }

            sb.append(complexParenthesesKeyConvert(sComma + col,""));

            if(sComma.isEmpty()){
                String sKey = getFirstKeyName(col);
                if(mapSqlKeyValid.containsKey(sKey)){
                    sComma = ",";
                }
            }
        }
        return sb.toString();
    }

    @Override
    protected List<SqlSegment> split(String sSql) {
        List<SqlSegment> list = new ArrayList<>();
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.updateSetPattern);//先截取UPDATE SET部分
        if (mc.find()){
            SqlSegment segment = new SqlSegment();
            segment.setHeadString(mc.group());//不变的UPDATE SET部分
            sSql = sSql.substring(mc.end()).trim();
            String sSet = "";
            //1、拆出至FROM之间的片段:UPDATE是可以有FROM的
            Matcher mcFrom = ToolHelper.getMatcher(sSql, StaticConstants.fromPattern);
            if (mcFrom.find()) {
                //1.1 有FROM
                sSet = sSql.substring(0,mcFrom.start());
                String sConvertSql = beforeFromConvert(sSet);
                //1.2 UPDATE部分
                segment.setFinalSql(sConvertSql);
                segment.setNeedParse(false);
                segment.setSqlSegmentEnum(SqlSegmentEnum.UPDATE_SET);
                list.add(segment);
                sSql = sSql.substring(mcFrom.end());
            }

            Matcher mcWhere = ToolHelper.getMatcher(sSql, StaticConstants.wherePattern);
            if (mcWhere.find()) {
                if(ToolHelper.IsNull(sSet)) {
                    //UPDATE SET如果未处理，那么这里再处理
                    sSet = sSql.substring(0, mcWhere.start());
                    String sConvertSql = beforeFromConvert(sSet);
                    //UPDATE部分
                    segment.setFinalSql(sConvertSql);
                    segment.setSqlSegmentEnum(SqlSegmentEnum.UPDATE_SET);
                    list.add(segment);
                }
                //WHER片段部分
                segment = new SqlSegment();
                segment.setHeadString(mcWhere.group());//不变的UPDATE SET部分
                sSql = sSql.substring(mcWhere.end()).trim();
                segment.setSql(sSql);
                segment.setSqlSegmentEnum(SqlSegmentEnum.WHERE_CONDTION);
                list.add(segment);
            }
        }
        return list;
    }

}
