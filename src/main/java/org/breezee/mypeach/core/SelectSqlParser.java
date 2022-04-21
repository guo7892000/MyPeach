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
            sbHead.append(queryHeadSqlConvert(sOne));
            iStart = mc.end();
            sbHead.append(mc.group());
        }
        if(iStart>0){
            String sOne = sSql.substring(iStart);
            sbHead.append(queryHeadSqlConvert(sOne));
        }else {
            sbHead.append(queryHeadSqlConvert(sSql));//通用的以Select开头的处理
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
                sbHead.append(queryHeadSqlConvert(sOneSql));
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
                sbHead.append(queryHeadSqlConvert(sOneSql));

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

    @Override
    protected List<SqlSegment> split(String sSql) {
        List<SqlSegment> list = new ArrayList<>();

        //1.With AS的处理
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.withSelectPartn);
        int indexWith = 0;
        while (mc.find()) {
            SqlSegment segment = new SqlSegment();
            sqlTypeEnum = SqlTypeEnum.SELECT_WITH_AS;
            String sOneSql = sSql.substring(indexWith,mc.start()).trim();
            if(ToolHelper.IsNotNull(sOneSql)){
                segment.setSql(sOneSql);
            }
            segment.setSqlSegmentEnum(SqlSegmentEnum.MORE_SELECT);
            segment.setTailString(mc.group());//设置尾部SQL
            indexWith = mc.end();
            list.add(segment);
        }
        if(indexWith>0) {
            sSql = sSql.substring(indexWith).trim();//去掉之前处理过的部分
            //匹配【)SELECT】部分
            mc = ToolHelper.getMatcher(sSql, StaticConstants.withSelectPartnToSelect);
            //这里只能是一个匹配
            if (mc.find()) {
                sSql = segmentConvert(sSql,SqlSegmentEnum.MORE_SELECT, list,mc);
            }
        }

        //2.UNION和UNION ALL处理
        mc = ToolHelper.getMatcher(sSql, StaticConstants.unionAllPartner);
        int indexUnion=0;
        while(mc.find()){
            SqlSegment segment = new SqlSegment();
            segment.setTailString(mc.group());
            segment.setSql(sSql.substring(indexUnion, mc.start()));
            segment.setSqlSegmentEnum(SqlSegmentEnum.MORE_SELECT);
            list.add(segment);

            indexUnion = mc.end();
        }
        if(indexUnion>0){
            String sOne = sSql.substring(indexUnion);
            SqlSegment segment = new SqlSegment();
            segment.setSqlSegmentEnum(SqlSegmentEnum.MORE_SELECT);
            segment.setSql(sOne);
            list.add(segment);
            return list;//有UNION或UNION ALL，这里SQL就全了，可以返回了
        }

        //3.没有UNION或UNION ALL
        SqlSegment segment = new SqlSegment();
        if(indexWith == 0){
            mc = ToolHelper.getMatcher(sSql, StaticConstants.selectPattern);//抽取出SELECT部分
            if (mc.find()) {
                segment.setHeadString(mc.group());
                sSql = sSql.substring(mc.end());
            }
        }

        //4.拆出SELET ITEM片段
        mc = ToolHelper.getMatcher(sSql, StaticConstants.fromPattern);//抽取出From部分
        if (mc.find()) {
            sSql = segmentConvert(sSql,SqlSegmentEnum.SELECT_ITEM, list,mc);
        }

        //5、拆出FROM至WHERE之间的片段
        mc = ToolHelper.getMatcher(sSql, StaticConstants.wherePattern);
        if (mc.find()) {
            sSql = segmentConvert(sSql,SqlSegmentEnum.FROM_TABLE, list,mc);//WHERE确定FROM部分
        }

        //6、拆出WHERE至GROUP BY之间的片段
        boolean needWhereSplit = true;//是否需要做WHERE分拆
        boolean needGroupBySplit = false;//是否需要做GroupBy分拆
        boolean needHavingSplit = false;//是否需要做GroupBy分拆
        mc = ToolHelper.getMatcher(sSql, StaticConstants.groupByPattern);
        if (mc.find()) {
            needGroupBySplit = true;
            sSql = segmentConvert(sSql,SqlSegmentEnum.WHERE_CONDTION, list,mc);//GROUP BY确定WHERE部分
            if(!hasKey(sSql)){
                //之后都没有key配置，那么直接将字符加到尾部，然后返回
                SqlSegment last = list.get(list.size()-1);
                last.setTailString(last.getTailString() + sSql.trim());
                return list;
            }

            needWhereSplit = false;
            Matcher mcHaving = ToolHelper.getMatcher(sSql, StaticConstants.havingPattern);
            if (mcHaving.find()) {
                needGroupBySplit = false;
                sSql = segmentConvert(sSql,SqlSegmentEnum.GROUP_BY, list,mcHaving);
                needHavingSplit = true;
            }
        }

        //7、拆出ORDER片段
        boolean needOrderSplit = false;
        mc = ToolHelper.getMatcher(sSql, StaticConstants.orderByPattern);
        if (mc.find()) {
            if(needWhereSplit){
                sSql = segmentConvert(sSql,SqlSegmentEnum.WHERE_CONDTION, list,mc);//GROUP BY确定WHERE部分
                needWhereSplit = false;
                if(!hasKey(sSql)){
                    //之后都没有key配置，那么直接将字符加到尾部，然后返回
                    SqlSegment last = list.get(list.size()-1);
                    last.setTailString(last.getTailString() + sSql.trim());
                    return list;
                }
            }
            if(needGroupBySplit){
                sSql = segmentConvert(sSql,SqlSegmentEnum.GROUP_BY, list,mc);//GROUP BY确定WHERE部分
                needGroupBySplit = false;
            }
            if(needHavingSplit){
                sSql = segmentConvert(sSql,SqlSegmentEnum.HAVING, list,mc);//GROUP BY确定WHERE部分
                needHavingSplit = false;
            }
            needOrderSplit = true;
        }

        //8、拆出LIMIT段
        mc = ToolHelper.getMatcher(sSql, StaticConstants.limitPattern);
        if (mc.find()) {
            if(needWhereSplit){
                sSql = segmentConvert(sSql,SqlSegmentEnum.WHERE_CONDTION, list,mc);//GROUP BY确定WHERE部分
                needWhereSplit = false;
            }
            if(needGroupBySplit){
                sSql = segmentConvert(sSql,SqlSegmentEnum.GROUP_BY, list,mc);//GROUP BY确定WHERE部分
                needGroupBySplit = false;
            }
            if(needHavingSplit){
                sSql = segmentConvert(sSql,SqlSegmentEnum.HAVING, list,mc);//GROUP BY确定WHERE部分
                needHavingSplit = false;
            }
            if(needOrderSplit){
                sSql = segmentConvert(sSql,SqlSegmentEnum.ORDER_BY, list,mc);//GROUP BY确定WHERE部分
                needHavingSplit = false;
            }
            //最后一段LIMIT处理
            SqlSegment segmentLimit = new SqlSegment();
            segmentLimit.setSql(sSql);
            segmentLimit.setSqlSegmentEnum(SqlSegmentEnum.LIMIT);
            list.add(segmentLimit);
        }

        //9、最后一段字符的处理
        if(ToolHelper.IsNotNull(sSql.trim())){
            SqlSegment last = list.get(list.size()-1);
            last.setTailString(last.getTailString() + sSql.trim());
        }

        return list;
    }

    private String segmentConvert(String sSql, SqlSegmentEnum segmentEnum,List<SqlSegment> list,Matcher mc) {
        SqlSegment segment = new SqlSegment();
        segment.setTailString(mc.group());
        segment.setSql(sSql.substring(0, mc.start()));
        segment.setSqlSegmentEnum(segmentEnum);
        list.add(segment);
        sSql = sSql.substring(mc.end());
        return sSql;
    }
}
