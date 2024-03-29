package org.breezee.mypeach.enums;

/**
 * @objectName:
 * @description:
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/16 10:34
 */
public enum TargetSqlParamTypeEnum {
    /**
     * 名称：命名参数化的SQL（默认，参数例如:ID）
     * 描述：在执行SQL时，需要根据有效的参数Map<String,SqlParam>来构造传入的参数值。参数格式见：SqlParamConfig的
     */
    NameParam,
    /**
     * 名称：位置参数化的SQL（参数为？）
     * 描述：在执行SQL时，需要根据有效的参数
     */
    PostionParam,
    /**
     * 名称：得到可以直接运行的SQL
     * 描述：已将SQL中的键替换为键值，可以直接运行。有SQL注入风险
     */
    DIRECT_RUN
}
