package org.breezee.mypeach.enums;

/**
 * @objectName: SQL类型枚举
 * @description:
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/12 16:45
 */
public enum SqlTypeEnum {
    /**
     * 查询语句
     */
    SELECT,
    /**
     * WITH AS SELECT查询语句
     */
    SELECT_WITH_AS,
    /**
     * 新增语句(INSERT INTO... VALUES...)
     */
    INSERT_VALUES,
    /**
     * 新增语句(INSERT INTO... SELECT...)
     */
    INSERT_SELECT,
    /**
     * 新增语句(WITH...INSERT INTO... SELECT...)，主要是SqlServer使用
     */
    WITH_INSERT_SELECT,
    /**
     * 新增语句(INSERT INTO...WITH...SELECT...)
     */
    INSERT_WITH_SELECT,
    /**
     * 更新语句
     */
    UPDATE,
    /**
     * 删除语句
     */
    DELETE,
    /**
     * 通用语句-MERGE
     */
    CommonMerge,
    /**
     *未知语句
     */
    Unknown

}
