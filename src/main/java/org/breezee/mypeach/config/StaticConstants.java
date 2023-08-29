package org.breezee.mypeach.config;

/**
 * @objectName: SQL正则式
 * @description:
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/12 16:45
 * @history:
 *   2023/08/04 BreezeeHui 修改remarkPatter正则式，对/**\/中间应该可以包括换行符
 *   2023/08/14 BreezeeHui 增加MERGE INTO的正则式
 *   2023/08/20 BreezeeHui 进一步完善和优化键配置、动态参数的正则式
 *   2023/08/24 BreezeeHui 动态参数的正则式支持中间加空格
 */
public class StaticConstants {
    public static final String parenthesesRoundKey = "##";
    public static final String LEFT_BRACE = "{";
    public static final String HASH_LEFT_BRACE = "#{";
    public static final String RIGHT_BRACE = "}";
    public static final String HASH = "#";
    public static final String PERCENT = "%";
    /**
     * sql单行双减号注释的正则表达式：--注释内容
     */
    public static final String remarkPatterSingle2Reduce = "--.*";
    /**
     * sql单行#号注释的正则表达式：#注释内容
     */
    public static final String remarkPatterSingleHash = "#.*";
    /**
     * sql多行注释的正则表达式：/***\/
     */
    public static final String remarkPatterMultiLine = "/\\*|\\*/";
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
    public static final String joinPattern = "\\s*((((LEFT)|(RIGHT)|(FULL))\\s*(OUTER)?)|(INNER)|(CROSS))?\\s+JOIN\\s*";
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

    public static final String insertIntoPatternCommon = "INSERT\\s+INTO\\s+\\S+\\s*";
    /**
     * INSERT INTO正则式：INSERT INTO TABLE_NAME(，但括号部分已被替换，所以旧正则式已不适用："^INSERT\\s+INTO\\s+\\S+\\s*\\(\\s*"
     */
    public static final String insertIntoPattern = "^" + insertIntoPatternCommon;

    //public static final String insertSelectPattern = "\\s*\\)" + commonSelectPattern;
    public static final String updateSetPattern = "^UPDATE\\s*\\S*\\s*SET\\s*";//正则式：UPDATE TABLE_NAME SET
    public static final String deletePattern = "^DELETE\\s+FROM\\s+\\S+\\s+"; //正则式:DELETE FROM TABALE_NAME

    //MERGE INTO
    public static final String mergePatternHead = "^MERGE\\s+(INTO\\s+)*(\\w+|[.\\[\\]`])+(\\s+AS\\s+\\w+)*\\s+USING\\s+"; //有些表名要加[]`.
    public static final String mergePatternMatchOrNot = "WHEN\\s+(NOT\\s+)*MATCHED\\s+THEN\\s+";

    //动态参数（支持{}、[]与连接符(&-@|)任意组合）：示例：  /***@MP&DYN {[id=1]}& {[A.ID,B.ID]}  @MP&DYN****/
    public static final String dynConditionKeyPre = "@MP&DYN&KEY:";//动态参数的键前缀
    public static final String dynSqlSegmentConfigPatternCenter = "[\\]\\}\\s]+\\s*[&\\-@\\|]+\\s*[\\{\\[\\s]+\\s*";//动态条件SQL段配置正则式_中间段
    public static final String dynSqlSegmentConfigPatternLeft = "\\s*([\\[\\{]+\\s*)+";//动态条件SQL段配置正则式_左边
    public static final String dynSqlSegmentConfigPatternRight = "\\s*([\\]\\}]+\\s*)+";//动态条件SQL段配置正则式_右边
    //动态参数中的IN和NOT IN正则式
    public static final String dynSqlSegmentNotInPattern = "\\s+NOT\\s+IN\\s+\\(";
    public static final String dynSqlSegmentInPattern = "\\s+IN\\s+\\(";

    //键正则式（支持中间的空格）：针对#{}()-'都要加上转义符，否则会报错！！
    //键大类支持中英文冒号(:：)、分号(;；)分隔，小类支持横杆(-)、竖线(|)、与(&)、电邮字符（@）分隔
    //#{}参数形式：示例： ' % # { MDLIST ：  M ；R : LS : F : D - now() &  r  | n  @ ii : N } % '
    public static final String keyPatternHashLeftBrace = "'?(\\s*%)?\\s*\\#\\s*\\{\\s*\\w+\\s*([:;：；]+\\s*\\w+\\s*([\\-\\|&@]\\s*(\\(|\\)|\\w|,|\\')*\\s*)*)*\\s*\\}(\\s*%)?(\\s*')?";
    //##参数形式：示例： ' % # MDLIST ：  M ；R : LS : F : D - now() &  r  | n  @ ii : N # % '
    public static final String keyPatternHash = "'?(\\s*%)?\\s*\\#\\s*\\w+\\s*([:;：；]+\\s*\\w+\\s*([\\-\\|&@]\\s*(\\(|\\)|\\w|,|\\')*\\s*)*)*\\s*\\#(\\s*%)?(\\s*')?";

    public static final String keyBigTypeSpit = ":|：|;|；"; // new char[] { ':', '：', ';', '；' }; //键配置大类
    public static final String keySmallTypeSpit = "-|&|@|\\|";  //new char[] { '-', '&', '@', '|' }; //键配置小类
    //IN正则式：IN可以在WHERE后面，也可以在AND或者OR后面
    public static final String inPattern = "(WHERE|AND|OR|\\()\\s+.+\\s+IN\\s+";
    public static final String onlyInPattern = "\\s+IN\\s+"; //仅包含IN并且左右有空格的IN
}
