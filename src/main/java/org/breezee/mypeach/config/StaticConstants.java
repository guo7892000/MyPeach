package org.breezee.mypeach.config;

/**
 * @objectName: SQL正则式
 * @description:
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/12 16:45
 */
public class StaticConstants {
    /**
     * sql备注的正则表达式：支持--和/***\/
     */
    public static final String remarkPatter = "--.*|(/\\*.*/*/)";
    /**
     * AND（或OR）的正则表达式
     */
    public static final String andOrPatter = "\\s+((AND)|(OR))\\s+";
    /**
     * WHERE的正则表达式
     */
    public static final String wherePattern= "\\s*WHERE\\s*";
    /**
     * FROM的正则表达式
     */
    public static final String fromPattern= "\\s*FROM\\s*";
    /**
     * SELECT的正则表达式
     */
    public static final String selectPattern = "^SELECT\\s+";

    public static final String LEFT_BRACE = "{";
    public static final String HASH_LEFT_BRACE = "#{";
    public static final String RIGHT_BRACE = "}";
    public static final String HASH = "#";
    public static final String PERCENT = "%";
}
