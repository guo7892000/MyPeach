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
    public static final String parenthesesRoundKey = "##";
    public static final String LEFT_BRACE = "{";
    public static final String HASH_LEFT_BRACE = "#{";
    public static final String RIGHT_BRACE = "}";
    public static final String HASH = "#";
    public static final String PERCENT = "%";
    /**
     * sql备注的正则表达式：支持--和/***\/
     */
    public static final String remarkPatter = "--.*|(/\\*.*/*/)";
    /**
     * 左括号或右括号的正则式
     */
    public static final String parenthesesPattern="\\(|\\)";
    /**
     * AND（或OR）的正则表达式
     */
    public static final String andOrPatter = "\\s+((AND)|(OR))\\s+";
    /**
     * WHERE的正则表达式
     */
    public static final String wherePattern= "\\s*WHERE\\s+";
    /**
     * FROM的正则表达式
     */
    public static final String fromPattern= "\\s*FROM\\s+";//前面为*，是因为有可能在拆分时，去掉了前面的空格

    /**
     * 各种JOIN的正则式
     */
    public static final String joinPattern = "\\s*((LEFT)|(RIGHT)|(FULL)|(INNER))?\\s+JOIN\\s*";
    /**
     * SELECT的正则表达式：增加DISTINCT、TOP N的支持
     */
    public static final String selectPattern = "^SELECT\\s+(DISTINCT|TOP\\s+\\d+\\s+)?\\s*";

    /**
     * SELECT查询的通用正则表达式：增加DISTINCT、TOP N的支持
     */
    public static final String commonSelectPattern = "\\s*SELECT\\s+(DISTINCT|TOP\\s+\\d+\\s+)?";

    /**
     * SELECT子查询的正则表达式：增加DISTINCT、TOP N的支持
     */
    public static final String childSelectPattern = "\\(" + commonSelectPattern;
    /**
     * 【withSelect最后的字符)SELECT】正则式，即真正开始查询的语句开始
     */
    public static final String withSelectPartnToSelect = "\\)" + commonSelectPattern;
    /**
     * UNION和UNION ALL的正则式
     */
    public static final String unionAllPartner = "\\s+UNION\\s+(ALL\\s+)?";

    /**
     * GROUP BY的正则表达式
     */
    public static final String groupByPattern= "\\s+GROUP\\s+BY\\s+";
    /**
     * HAVING的正则表达式
     */
    public static final String havingPattern= "\\s+HAVING\\s+";
    /**
     * ORDER BY的正则表达式
     */
    public static final String orderByPattern= "\\s+ORDER\\s+BY\\s+";

    /**
     * LIMIT的正则表达式
     */
    public static final String limitPattern= "\\s+LIMIT\\s+";
    /**
     * VALUES正则式：)VALUES(，但括号部分已被替换，所以旧正则式已不适用："\\)\\s*VALUES\\s*\\(\\s*"
     */
    public static final String valuesPattern = "\\s*VALUES\\s*"; //正则式：)VALUES(
    /**
     * INSERT INTO正则式：INSERT INTO TABLE_NAME(，但括号部分已被替换，所以旧正则式已不适用："^INSERT\\s+INTO\\s+\\S+\\s*\\(\\s*"
     */
    public static final String insertIntoPattern = "^INSERT\\s+INTO\\s+\\S+\\s*";

    //public static final String insertSelectPattern = "\\s*\\)" + commonSelectPattern;
    public static final String updateSetPattern = "^UPDATE\\s*\\S*\\s*SET\\s*";//正则式：UPDATE TABLE_NAME SET
    public static final String deletePattern = "^DELETE\\s+FROM\\s+\\S+\\s+"; //正则式:DELETE FROM TABALE_NAME

}
