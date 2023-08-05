package org.breezee.mypeach.config;

/**
 * @objectName: SQL键配置
 * @description: SQL中用到的键配置值说明
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/12 16:45
 */
public class SqlKeyConfig {
    /**
     * 键为非空值
     */
    public static final String NOT_NULL = "N";
    /**
     * 键是否必填
     */
    public static final String IS_MUST = "M";
    /**
     * 必须值替换，不使用参数
     */
    public static final String VALUE_REPLACE = "R";
    /**
     * 字符串清单
     */
    public static final String STRING_LIST = "LS";
    /**
     * 整型值清单
     */
    public static final String INTEGE_LIST = "LI";
    /**
     * 优先使用的配置：当同一个键出现多次时，会以F的配置为主
     */
    public static final String IS_FIRST = "F";
    /**
     * 动态SQL配置关键字：
     * 使用场景如根据不同键值使用不同分组方式，在SQL中以注释方式做配置
     */
    public static final String DYNAMIC_SQL = "DYN";
}
