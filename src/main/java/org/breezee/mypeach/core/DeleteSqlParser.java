package org.breezee.mypeach.core;

import lombok.extern.slf4j.Slf4j;
import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.config.StaticConstants;
import org.breezee.mypeach.entity.ParserResult;
import org.breezee.mypeach.entity.SqlSegment;
import org.breezee.mypeach.enums.SqlSegmentEnum;
import org.breezee.mypeach.enums.SqlTypeEnum;
import org.breezee.mypeach.utils.ToolHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * @objectName:Delete Sql Analyzer(删除SQL分析器)
 * @description:
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/12 16:45
 */
@Slf4j
public class DeleteSqlParser extends AbstractSqlParser {

    public DeleteSqlParser(MyPeachProperties properties) {
        super(properties);
        sqlTypeEnum = SqlTypeEnum.DELETE;
    }

    @Override
    public String headSqlConvert(String sSql) {
        StringBuilder sb = new StringBuilder();
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.deletePattern);//抽取出INSERT INTO TABLE_NAME(部分
        while (mc.find()){
            sb.append(mc.group());//不变的INSERT INTO TABLE_NAME(部分先加入
            //FROM部分SQL处理
            String sWhereSql = fromWhereSqlConvert(sSql.substring(mc.end()));
            if(ToolHelper.IsNull(sWhereSql)){
                mapError.put("严重错误","删除语句不能没有条件，那样会清除整张表数据！");//错误列表
            }
            sb.append(sWhereSql);
        }
        return sb.toString();
    }

    @Override
    protected String beforeFromConvert(String sSql) {
        return "";
    }

    @Override
    protected List<SqlSegment> split(String sSql) {
        List<SqlSegment> list = new ArrayList<>();
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.deletePattern);
        while (mc.find()){
            SqlSegment segment = new SqlSegment();
            segment.setHeadString(mc.group());//不变的DELETE FROM部分
            segment.setSql(sSql.substring(mc.end()));//FROM部分SQL处理
            segment.setSqlSegmentEnum(SqlSegmentEnum.WHERE_CONDTION);
            list.add(segment);
        }
        return list;
    }
}
