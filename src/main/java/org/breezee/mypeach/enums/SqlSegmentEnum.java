package org.breezee.mypeach.enums;

/**
 * @objectName: SQL片段的枚举
 * @description: 定义拆分后的SQL类型
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/21 10:48
 */
public enum SqlSegmentEnum {
    /**
     * SELECT至FROM段
     */
    SELECT_ITEM,
    /**
     * FROM至WHERE段
     */
    FROM_TABLE,
    /**
     * WHERE下的条件
     */
    WHERE_CONDTION,
    GROUP_BY,
    HAVING,
    ORDER_BY,
    LIMIT,
    UPDATE_SET,
    INSERT_ITEM,
    INSER_VALUE,
    /**
     * 更多的子查询：需要再次分析
     */
    MORE_SELECT,
}
