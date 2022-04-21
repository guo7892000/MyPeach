package org.breezee.mypeach.entity;

import lombok.Data;
import org.breezee.mypeach.enums.SqlSegmentEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * @objectName: SQL片段
 * @description: 拆分后的SQL片段
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/21 10:45
 */
@Data
public class SqlSegment {
    /**
     * SQL片段类型
     */
    private SqlSegmentEnum sqlSegmentEnum;
    /**
     * 是否需要转换
     */
    boolean needParse = true;
    /**
     *头部SQL
     */
    private String headString;
    /**
     *尾部SQL
     */
    private String tailString;
    /**
     *需要转换的SQL（只有一个SQL时）
     */
    private String sql;
    /**
     *需要转换的子SQL片段集合
     */
    private List<SqlSegment> childSql = new ArrayList<>();
    /**
     * 最终的SQL：如其有值，说明该片段已全部转换完成
     */
    private String finalSql;

    public static SqlSegment build(String sSql){
        return build(sSql,"","");
    }

    public static SqlSegment build(String sSql,String sHead){
        return build(sSql,sHead,"");
    }

    public static SqlSegment build(String sSql,String sHead,String tailString){
        SqlSegment segment = new SqlSegment();
        segment.setSql(sSql);
        segment.setHeadString(sHead);
        segment.setTailString(tailString);
        return segment;
    }
}
