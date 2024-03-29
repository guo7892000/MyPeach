package org.breezee.mypeach.core;

import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.config.StaticConstants;
import org.breezee.mypeach.entity.ParserResult;
import org.breezee.mypeach.entity.SqlKeyValueEntity;
import org.breezee.mypeach.enums.SqlTypeEnum;
import org.breezee.mypeach.enums.TargetSqlParamTypeEnum;
import org.breezee.mypeach.utils.ToolHelper;
import org.breezee.mypeach.config.SqlKeyConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * @objectName: SQL分析抽象类
 * @description: 作为SELECT、INSERT、UPDATE、DELETE分析器的父类，包含SQL的前置处理，如SQL大写、去掉注释、提取键等。
 *      注：在匹配的SQL中，不能修改原字符，不然根据mc.start()或mc.end()取出的子字符会不对!!
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/12 21:52
 * @history:
 *   2023/07/20 BreezeeHui 增加非空参数无值时抛错。
 *   2023/07/29 BreezeeHui 增加WITH INSERT INTO SELECT 和INSERT INTO WITH SELECT的支持。
 *   2023/08/04 BreezeeHui 键设置增加优先使用配置项（F）的支持，即当一个键出现多次时，优先使用带有F配置的内容。
 *   2023/08/05 BreezeeHui 增加#号注释支持；修正/**\/注释的匹配与移除。
 *   2023/08/11 BreezeeHui 将移除注释抽成一个独立方法RemoveSqlRemark；增加SQL类型是否正确的抽象方法isRightSqlType。
 *   2023/08/18 BreezeeHui 针对注释中动态SQL的条件拼接，在预获取条件参数时，把动态SQL中的键也加进去！
 *   2023/08/19 BreezeeHui 只有在非预获取条件参数，且传入条件为空时，才把默认值赋给传入条件值！
 *   2023/08/24 BreezeeHui 修正子查询或之后中有多个()转换错误问题；修正SELECT有#参数#时转换错误问题；修正WITH正则式。
 *   2023/08/25 BreezeeHui 将unionOrUnionAllConvert抽取到父类中，方便针对所有SELECT语句先做union或Union All分析。
 *   2023/08/30 BreezeeHui 增加对IN配置多少项（默认1000）后分拆成AND (XX IN ('值1','值2') OR XX IN ('值N1','值N2'))。
 *   2024/01/24 BreezeeHui 修正当没有WHERE条件时无法转换问题。
 */
public abstract class AbstractSqlParser {

    protected MyPeachProperties myPeachProp;

    /**
     * 优先处理的括号会被替换的两边字符加中间一个序号值，例如：##1##
     * 如其跟键前缀重复，则会在后面增加一个#号
     */
    private String parenthesesRoundKey = StaticConstants.parenthesesRoundKey;
    private String parenthesesRoundKeyPattern;
    protected String withSelectPartn;

    protected Map<String, SqlKeyValueEntity> mapSqlKey;//SQL中所有键
    protected Map<String, SqlKeyValueEntity> mapSqlKeyValid;//SQL中有传值的所有键
    protected ArrayList positionParamConditonList;//位置参数用到的参数值

    Map<String,String> mapsParentheses;//优先处理的括号集合
    public Map<String, String> mapError;//错误信息Map
    Map<String, Object> mapObject;
    Map<String, String> mapString;
    Map<String, String> mapReplaceOrInCondition; //被替换的值或IN清单，这些将在返回的条件中剔除，因为他们不需要被参数化
    Map<String, String> dynamicString;
    protected SqlTypeEnum sqlTypeEnum;
    /*针对SqlServer的WITH... INSERT INTO... SELECT...，示例
     * with TMP_A AS(select 3 as id,'zhanshan3' as name)
     * INSERT INTO TEST_TABLE(ID,CNAME)
     * select * from TMP_A
     */
    protected String withInsertIntoSelectPartn;

    /*针对MySql、Oracle、SQLite、PostgerSQL的的INSERT INTO... WITH.. SELECT...，示例
     * with TMP_A AS(select 3 as id,'zhanshan3' as name)
     * INSERT INTO TEST_TABLE(ID,CNAME)
     * select * from TMP_A
     */
    protected String insertIntoWithSelectPartn;
    protected String insertIntoWithSelectPartnCommon;

    /***
     * 构造函数：初始化所有变量
     * @param prop 全局配置
     */
    public AbstractSqlParser(MyPeachProperties prop){
        myPeachProp = prop;

        if(parenthesesRoundKey.equals(StaticConstants.HASH)){
            parenthesesRoundKey+=StaticConstants.HASH;
        }
        parenthesesRoundKeyPattern = parenthesesRoundKey + "\\d+" + parenthesesRoundKey;
        //因为括号已被替换为##序号##，所以原正则式已不能使用："\\)?\\s*,?\\s*WITH\\s+\\w+\\s+AS\\s*\\("+commonSelectPattern;
        insertIntoWithSelectPartnCommon = "\\s*,?\\s*(WITH)*\\s+\\w+\\s+AS\\s"; //注：这里的WITH只是第一个临时表有，后面的是没有的
        withSelectPartn = insertIntoWithSelectPartnCommon + "+" + parenthesesRoundKeyPattern;
        /*最终正则式：^\s*,?\s*WITH\s+\w+\s+AS\s*##\d+##\s*INSERT\s+INTO\s+\S+\s*##\d+##*/
        withInsertIntoSelectPartn = "^" + insertIntoWithSelectPartnCommon+"*" + parenthesesRoundKeyPattern +"\\s*"
                + StaticConstants.insertIntoPatternCommon + parenthesesRoundKeyPattern; //SqlServer使用
        /*最终正则式：^INSERT\s+INTO\s+\S+\s*\s*,?\s*WITH\s+\w+\s+AS\s*##\d+##*/
        insertIntoWithSelectPartn = StaticConstants.insertIntoPattern + insertIntoWithSelectPartnCommon + "*" + parenthesesRoundKeyPattern;

        mapsParentheses = new HashMap<>();
        mapSqlKey = new HashMap<>();
        mapSqlKeyValid = new HashMap<>();
        mapError = new ConcurrentHashMap<>();//并发容器-错误信息
        mapObject = new HashMap<>();
        mapString = new HashMap<>();
        mapReplaceOrInCondition = new HashMap<>();
        dynamicString = new HashMap<>();
        positionParamConditonList = new ArrayList();
    }

    /// <summary>
    /// 预获取SQL参数（方便给参数赋值用于测试）
    /// </summary>
    /// <param name="sSql"></param>
    /// <returns></returns>
    public Map<String, SqlKeyValueEntity> PreGetParam(String sSql,Map<String, Object> dic) {
        Map<String, SqlKeyValueEntity> dicReturn = new HashMap<String, SqlKeyValueEntity>();
        //条件键优化
        Map<String, Object> dicNew = conditionKeyOptimize(dic);
        //1、移除所有注释
        String sSqlNew = RemoveSqlRemark(sSql, dicNew,true);
        //2、获取SQL中的#参数#
        Matcher mc = ToolHelper.getMatcher(sSqlNew, StaticConstants.keyPatternHash);
        while (mc.find()) {
            String sParamName = ToolHelper.getKeyName(mc.group(), myPeachProp);
            SqlKeyValueEntity param = SqlKeyValueEntity.build(mc.group(), new HashMap<>(), myPeachProp,true);
            if (!dicReturn.containsKey(sParamName)) {
                dicReturn.put(sParamName,param);
            }
        }
        //将动态条件拼接SQL段的键加入，方便测试
        for (String sKey:dicNew.keySet()) {
            if (sKey.startsWith(StaticConstants.dynConditionKeyPre)) {
                String sRealKey = sKey.replace(StaticConstants.dynConditionKeyPre, "");
                if(!sRealKey.isEmpty() && !dicReturn.containsKey(sRealKey)) {
                    SqlKeyValueEntity entity = new SqlKeyValueEntity();
                    entity.setKeyName(sRealKey); //目前外部只用到一个键名
                    entity.setKeyValue(dicNew.get(sKey));
                    entity.setHasValue(true);
                    dicReturn.put(sRealKey, entity);
                }
            }
        }
        return dicReturn;
    }

    /// <summary>
    /// 条件键优化
    /// </summary>
    /// <param name="dic"></param>
    /// <returns></returns>
    private static Map<String, Object> conditionKeyOptimize(Map<String, Object> dic)
    {
        //1、对传入的条件集合中的KEY进行优化：如去掉#号，如有：分隔，那么取第一个值作为键
        Map<String, Object> dicNew = new HashMap<String, Object>();
        for (String key:dic.keySet())
        {
            String sKeyNew = key.replace("#", "").replace("{", "").replace("}", "");
            sKeyNew = sKeyNew.split(StaticConstants.keyBigTypeSpit)[0];
            dicNew.put(sKeyNew, dic.get(key));
        }
        return dicNew;
    }

    /// <summary>
    /// 获取所有参数键
    /// </summary>
    /// <param name="sSql"></param>
    /// <param name="dicNew"></param>
    private void getAllParamKey(String sSql, Map<String, Object> dicNew)
    {
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.keyPatternHash);
        int iLastEnd = 0;
        while (mc.find()) {
            String sParamName = ToolHelper.getKeyName(mc.group(), myPeachProp);
            SqlKeyValueEntity param = SqlKeyValueEntity.build(mc.group(), dicNew, myPeachProp,false);
            //确定IN列字符
            fixedInColumnName(sSql, mc, iLastEnd, param);

            if (!mapSqlKey.containsKey(sParamName)) {
                mapSqlKey.put(sParamName, param);//参数不存在，直接添加
            } else {
                if (param.getKeyMoreInfo().isFirst()) {
                    mapSqlKey.put(sParamName,param); //如是优先配置，那么替换原存在的配置对象
                }
            }

            if (!mapSqlKeyValid.containsKey(sParamName) && param.isHasValue()) {
                mapSqlKeyValid.put(sParamName, param);//有传值的键
                mapObject.put(sParamName, param.getKeyValue());
                mapString.put(sParamName,param.getKeyValue().toString());
            }
            if (!param.isHasValue() && param.getKeyMoreInfo().isMust()) {
                mapError.put(sParamName, sParamName + "参数非空，但未传值！");//非空参数空值报错
            }

            if (ToolHelper.IsNotNull(param.getErrorMessage())) {
                mapError.put(sParamName, param.getErrorMessage());//错误列表
            }

            if (param.getKeyMoreInfo().isMustValueReplace()) {
                mapReplaceOrInCondition.put(sParamName, sParamName); //要被替换或IN清单的条件键
            }

            //位置参数的条件值数组
            if (param.isHasValue() && !param.getKeyMoreInfo().isMustValueReplace()) {
                positionParamConditonList.add(param.getKeyValue());
            }
            iLastEnd = mc.end(); //上次参数位置
        }
    }

    /// <summary>
    /// 确实In列字符
    /// </summary>
    /// <param name="sSql"></param>
    /// <param name="mc"></param>
    /// <param name="iLastEnd"></param>
    /// <param name="param"></param>
    private static void fixedInColumnName(String sSql, Matcher mc, int iLastEnd, SqlKeyValueEntity param)
    {
        //确定InColumnName:20230829
        if (ToolHelper.IsNotNull(param.getKeyMoreInfo().getInString()))
        {
            String sBeforeSql = sSql.substring(iLastEnd, mc.end());
            Matcher mcIn = ToolHelper.getMatcher(sBeforeSql, StaticConstants.inPattern);
            while (mcIn.find())
            {
                String sIncudeColumnName = mcIn.group().trim();
                Matcher mcColunIn = ToolHelper.getMatcher(sIncudeColumnName, "(WHERE|AND|OR)\\s*");
                if (mcColunIn.find()) {
                    sIncudeColumnName = sIncudeColumnName.replace(mcColunIn.group(), "").trim();
                }
                mcColunIn = ToolHelper.getMatcher(sIncudeColumnName, "\\s+IN");
                if (mcColunIn.find()) {
                    sIncudeColumnName = sIncudeColumnName.replace(mcColunIn.group(), "").trim();
                }

                int iLeft = 0;
                int iRight = 0;
                while (sIncudeColumnName.startsWith("(")) {
                    for (char oneChar:sIncudeColumnName.toCharArray()) {
                        if (oneChar == '(' ) iLeft++;
                        if (oneChar== ')') iRight++;
                    }
                    if (iLeft > iRight) {
                        sIncudeColumnName = sIncudeColumnName.substring(1);
                        iLeft--;
                    }
                }
                param.getKeyMoreInfo().setInColumnName(sIncudeColumnName);
            }
        }
    }

    /// <summary>
    /// SQL预优化
    /// </summary>
    /// <param name="sSql"></param>
    /// <returns></returns>
    private String sqlPreOptimize(String sSql)
    {
        //去掉前后空格
        String sNoConditionSql = sSql;
        //将#{}的参数，转换为##形式，方便后面统一处理
        Matcher mc = ToolHelper.getMatcher(sNoConditionSql, StaticConstants.keyPatternHashLeftBrace);
        while (mc.find()) {
            String sNewParam = mc.group().replace("#{", "#").replace("}", "#");
            sSql = sSql.replace(mc.group(), sNewParam);
        }
        return sSql;
    }


    /**
     * 移除SQL注释方法
     * @param sSql
     * @param dic
     * @param isPreGetCondition
     * @return
     */
    public String RemoveSqlRemark(String sSql,Map<String, Object> dic,boolean isPreGetCondition)
    {
        //1、预处理
        //1.1 去掉前后空字符：注这里不要转换为大写，因为有些条件里有字母值，如转换为大写，则会使条件失效！！
        sSql = sSql.trim(); //.toUpperCase();//将SQL转换为大写

        //1.2 将参数中的#{}，转换为##，方便后续统一处理
        sSql = sqlPreOptimize(sSql);

        //2、删除所有注释，降低分析难度，提高准确性
        //2.1 先去掉--的单行注释
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.remarkPatterSingle2Reduce);//Pattern：explanatory note
        while (mc.find()) {
            sSql = sSql.replace(mc.group(), "");//删除所有注释
        }

        //2.2 先去掉/***\/的多行注释：因为多行注释不好用正则匹配，所以其就要像左右括号一样，单独分析匹配
        sSql = removeMultiLineRemark(sSql, dic,isPreGetCondition);
        //参数#改为*后的SQL
        String sNoConditionSql = sSql;
        mc = ToolHelper.getMatcher(sSql, StaticConstants.keyPatternHash);
        while (mc.find()) {
            //先将#号替换为*，防止跟原注释冲突。注：字符数量还是跟原SQL一样！
            String sNewParam = mc.group().replace("#", "*");
            sNoConditionSql = sNoConditionSql.replace(mc.group(), sNewParam); //将参数替换为新字符
        }

        //2.3、移除#开头的单行注释
        mc = ToolHelper.getMatcher(sNoConditionSql, StaticConstants.remarkPatterSingleHash);
        StringBuilder sbNoRemark = new StringBuilder();
        int iGroupStart = 0;//组开始的位置
        boolean isHasHashRemark = false;
        while (mc.find()) {
            sbNoRemark.append(sSql.substring(iGroupStart, mc.start()));
            iGroupStart = mc.end();
            isHasHashRemark = true;
        }
        if (iGroupStart > 0) {
            sbNoRemark.append(sSql.substring(iGroupStart)); //最后的字符
        }
        if (isHasHashRemark) {
            sSql = sbNoRemark.toString();
        }
        return sSql;
    }

    /**
     * 转换SQL（主入口方法）
     * @param sSql 要转换的SQL
     * @param dic SQL键配置的值
     * @return 返回转换结果
     */
    public ParserResult parse(String sSql, Map<String, Object> dic){
        //1、 条件键优化
        Map<String, Object> dicNew = conditionKeyOptimize(dic);

        //2、移除所有注释
        sSql = RemoveSqlRemark(sSql, dicNew,false);

        //3、获取SQL所有参数信息
        getAllParamKey(sSql, dicNew);

        //3.1、当传入参数不符合，则直接返回退出
        ParserResult result;
        if (mapSqlKey.size() == 0) {
            result = ParserResult.success(sSql, mapSqlKey, mapObject, mapString, positionParamConditonList);

            result.setMessage("SQL中没有发现键(键配置样式为：" + StaticConstants.HASH + "key" + StaticConstants.HASH + "或"
                    + StaticConstants.HASH_LEFT_BRACE + "key" + StaticConstants.RIGHT_BRACE + ")，已直接返回原SQL！");
            return result;
        }

        if (mapError.size() > 0) {
            return ParserResult.fail("部分非空键（" + String.join(",", mapError.keySet()) + "）没有传入值，已退出！", mapError);
        }

        //4、得到符合左右括号正则式的内容，并替换为类似：##序号##格式，方便先从大方面分析结构，之后再取出括号里的内容来进一步分析
        String sNewSql = generateParenthesesKey(sSql);
        if (ToolHelper.IsNotNull(sNewSql)) {
            sSql = sNewSql;
        }

        //5、转换处理：边拆边处理
        String sFinalSql = headSqlConvert(sSql);

        //在处理过程中，也会往mapError写入错误信息，所以这里如有错误，也返回出错信息
        if (mapError.size() > 0) {
            return ParserResult.fail("部分非空键没有传入值或其他错误，关联信息：" + String.join(",", mapError.keySet()) + "，已退出！", mapError);
        }
        //6、返回最终结果
        if (sFinalSql.isEmpty()) {
            return ParserResult.fail("转换失败，原因不明。", mapError);
        }

        //6.1、针对值替换以及IN清单，要从条件中移除，防止参数化报错
        for (String sKey: mapReplaceOrInCondition.keySet()) {
            mapSqlKeyValid.remove(sKey);
            mapObject.remove(sKey);
            mapString.remove(sKey);
        }

        result = ParserResult.success(sFinalSql, mapSqlKeyValid, mapObject, mapString, positionParamConditonList);
        result.setSql(sFinalSql);
        result.setEntityQuery(mapSqlKeyValid);
        //6.2、输出SQL到控制台
        if (myPeachProp.isShowDebugSql()) {
            System.out.println(sFinalSql);
        }
        //6.3、如有设置SQL输出路径，那么也记录SQL到日志文件中。
        String sPath = myPeachProp.getLogSqlPath();
        if(!sPath.isEmpty()){
            Calendar calendar= Calendar.getInstance();
            SimpleDateFormat dateFormat= new SimpleDateFormat("yyyy-MM-dd");
            String sLogFileName = "/sql."+dateFormat.format(calendar.getTime())+".txt";
            if (!sPath.startsWith("/") && sPath.indexOf(":") == 0) {
                sPath = System.getProperty("user.dir")+"/" + sPath;
            }
            Path path = Paths.get(sPath);
            if(!Files.exists(path)){
                try {
                    Files.createDirectories(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                path = Paths.get(sPath + "/" + sLogFileName);
                StringBuilder sb = new StringBuilder();
                sb.append(sFinalSql+System.lineSeparator());
                dateFormat= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                sb.append("*******************【" + dateFormat.format(calendar.getTime()) + "】**************************");
                sb.append(System.lineSeparator());
                sb.append(System.lineSeparator());
                Files.write(path, sb.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 转换重载方法
     * @param sSql
     * @param dic
     * @param targetSqlParamTypeEnum
     * @return
     */
    public ParserResult parse(String sSql, Map<String, Object> dic,TargetSqlParamTypeEnum targetSqlParamTypeEnum){
        TargetSqlParamTypeEnum oldParamTypeEnum = myPeachProp.getTargetSqlParamTypeEnum();
        if(targetSqlParamTypeEnum == oldParamTypeEnum){
            return parse(sSql,dic);
        }
        myPeachProp.setTargetSqlParamTypeEnum(targetSqlParamTypeEnum);//设置为新的目标SQL类型
        ParserResult result = parse(sSql,dic);
        myPeachProp.setTargetSqlParamTypeEnum(oldParamTypeEnum);//还原为旧的目标SQL类型
        return result;
    }

    /**
     * 移除多行注释
     * 目的：为了简化SQL分析
     * @param sSql
     * @return
     */
    protected String removeMultiLineRemark(String sSql, Map<String, Object> dic,boolean isPreGetCondition) {
        Matcher mc;
        StringBuilder sb = new StringBuilder();
        mc = ToolHelper.getMatcher(sSql, StaticConstants.remarkPatterMultiLine);
        //int iGroup = 0;//第几组括号
        int iLeft = 0;//左注释数
        int iRight = 0;//右注释数
        int iGroupStart = 0;//组开始的位置

        int iRemarkBegin = 0;//注释开始的地方
        String sOneRemarkSql = "";

        //增加动态SQl语句的处理

        while (mc.find()) {
            if ("/*".equals(mc.group())) {
                iLeft++;
                if (iLeft == 1) {
                    String sNowStr = sSql.substring(iGroupStart, mc.start());
                    if (!sNowStr.isEmpty()) {
                        sb.append(sNowStr);//注：不要包括左括号
                    }
                    iGroupStart = mc.end();
                    iRemarkBegin=mc.start();
                }
            } else {
                iRight++;
            }
            //判断是否是一组数据
            if (iLeft == iRight) {
                iGroupStart = mc.end();//下一个语句的开始
                sOneRemarkSql = sSql.substring(iRemarkBegin, iGroupStart).trim();
                int iLen = SqlKeyConfig.dynamicSqlRemarkFlagString.length();
                int iStart = sOneRemarkSql.indexOf(SqlKeyConfig.dynamicSqlRemarkFlagString);
                int iEnd = sOneRemarkSql.lastIndexOf(SqlKeyConfig.dynamicSqlRemarkFlagString);
                if (iStart > -1 && iEnd>-1) {
                    //包含动态SQL标志
                    sOneRemarkSql = sOneRemarkSql.substring(iStart+ iLen, iEnd).trim();
                    sOneRemarkSql = getDynamicSql(dic, sOneRemarkSql,isPreGetCondition);
                    sb.append(sOneRemarkSql.trim());//加入动态部分的SQL
                }

                iLeft = 0;
                iRight = 0;
            }
        }
        //最后的字符
        if (iGroupStart > 0) {
            sb.append(sSql.substring(iGroupStart).trim());
            return sb.toString().trim();
        }
        //没有注释时，直接返回原SQL
        return sSql;
    }

    /// <summary>
    /// 获取动态SQL
    /// </summary>
    /// <param name="dic"></param>
    /// <param name="sOneRemarkSql"></param>
    /// <returns></returns>
    /// <exception cref="Exception"></exception>
    private String getDynamicSql(Map<String, Object> dic, String sOneRemarkSql,boolean isPreGetCondition) {
        try
        {
            Matcher mc = ToolHelper.getMatcher(sOneRemarkSql, StaticConstants.dynSqlSegmentConfigPatternCenter);
            if (mc.find()) {
                String sCond = sOneRemarkSql.substring(0,mc.start());
                String sDynSql = sOneRemarkSql.substring(mc.end());

                mc = ToolHelper.getMatcher(sCond, StaticConstants.dynSqlSegmentConfigPatternLeft);
                if (mc.find())
                {
                    sCond = sCond.substring(mc.end()).trim();
                }

                mc = ToolHelper.getMatcher(sDynSql, StaticConstants.dynSqlSegmentConfigPatternRight);
                if (mc.find())
                {
                    sDynSql = sDynSql.substring(0,mc.start()).trim();
                }

                int iFinStart = -1;
                String sOperateStr = "";
                //增加IN和NOT IN 支持
                mc = ToolHelper.getMatcher(sCond, StaticConstants.dynSqlSegmentNotInPattern);
                if (mc.find())
                {
                    return dynSqlSegmentInOrNotConditionEqual(dic, isPreGetCondition, mc, sCond, sDynSql,true);
                }
                mc = ToolHelper.getMatcher(sCond, StaticConstants.dynSqlSegmentInPattern);
                if (mc.find())
                {
                    return dynSqlSegmentInOrNotConditionEqual(dic, isPreGetCondition, mc, sCond, sDynSql, false);
                }

                if (sCond.indexOf(">=") > 0) {
                    //大于等于：使用整型比较
                    sOperateStr = ">=";
                    iFinStart = sCond.indexOf(sOperateStr);
                    String sKey = sCond.substring(0, iFinStart).trim();
                    String sValue = sCond.substring(iFinStart + sOperateStr.length()).trim();
                    if (dic.containsKey(sKey)) {
                        Integer iCondValue = Integer.parseInt(dic.get(sKey).toString());
                        Integer iSqlValue = Integer.parseInt(sValue);
                        return (iCondValue.compareTo(iSqlValue) >= 0) ? sDynSql : "";
                    }
                    if (isPreGetCondition && !dic.containsKey(sKey)) {
                        dic.put(StaticConstants.dynConditionKeyPre + sKey, sValue);//加上前缀，是为了更好区分这是注释里的动态键
                    }
                } else if (sCond.indexOf("<=") > 0) {
                    //小于等于：使用整型比较
                    sOperateStr = "<=";
                    iFinStart = sCond.indexOf(sOperateStr);
                    String sKey = sCond.substring(0, iFinStart).trim();
                    String sValue = sCond.substring(iFinStart + sOperateStr.length()).trim();
                    if (dic.containsKey(sKey)) {
                        Integer iCondValue = Integer.parseInt(dic.get(sKey).toString());
                        Integer iSqlValue = Integer.parseInt(sValue);
                        return (iCondValue.compareTo(iSqlValue) <= 0) ? sDynSql : "";
                    }
                    if (isPreGetCondition && !dic.containsKey(sKey)) {
                        dic.put(StaticConstants.dynConditionKeyPre + sKey, sValue);//加上前缀，是为了更好区分这是注释里的动态键
                    }
                } else if (sCond.indexOf("<") > 0){
                    //小于：使用整型比较
                    sOperateStr = "<";
                    iFinStart = sCond.indexOf(sOperateStr);
                    String sKey = sCond.substring(0, iFinStart).trim();
                    String sValue = sCond.substring(iFinStart + sOperateStr.length()).trim();
                    if (dic.containsKey(sKey)) {
                        Integer iCondValue = Integer.parseInt(dic.get(sKey).toString());
                        Integer iSqlValue = Integer.parseInt(sValue);
                        return (iCondValue.compareTo(iSqlValue) < 0) ? sDynSql : "";
                    }
                    if (isPreGetCondition && !dic.containsKey(sKey)) {
                        dic.put(StaticConstants.dynConditionKeyPre + sKey, sValue);//加上前缀，是为了更好区分这是注释里的动态键
                    }
                } else if (sCond.indexOf(">") > 0) {
                    //大于：使用整型比较
                    sOperateStr = ">";
                    iFinStart = sCond.indexOf(sOperateStr);
                    String sKey = sCond.substring(0, iFinStart).trim();
                    String sValue = sCond.substring(iFinStart + sOperateStr.length()).trim();
                    if (dic.containsKey(sKey)) {
                        Integer iCondValue = Integer.parseInt(dic.get(sKey).toString());
                        Integer iSqlValue = Integer.parseInt(sValue);
                        return (iCondValue.compareTo(iSqlValue) > 0) ? sDynSql : "";
                    }
                    if (isPreGetCondition && !dic.containsKey(sKey)) {
                        dic.put(StaticConstants.dynConditionKeyPre + sKey, sValue);//加上前缀，是为了更好区分这是注释里的动态键
                    }
                } else if (sCond.indexOf("=") > 0) {
                    //等于：使用字符比较
                    sOperateStr = "=";
                    iFinStart = sCond.indexOf(sOperateStr);
                    String sKey = sCond.substring(0, iFinStart).trim();
                    String sValue = sCond.substring(iFinStart + sOperateStr.length()).trim();
                    if (dic.containsKey(sKey)) {
                        return sValue.equals(dic.get(sKey).toString()) ? sDynSql : "";
                    }
                    if (isPreGetCondition && !dic.containsKey(sKey)) {
                        dic.put(StaticConstants.dynConditionKeyPre + sKey, sValue);//加上前缀，是为了更好区分这是注释里的动态键
                    }
                } else if (sCond.indexOf("!=") > 0) {
                    //不等于：使用字符比较
                    sOperateStr = "!=";
                    iFinStart = sCond.indexOf(sOperateStr);
                    String sKey = sCond.substring(0, iFinStart).trim();
                    String sValue = sCond.substring(iFinStart + sOperateStr.length()).trim();
                    if (dic.containsKey(sKey)) {
                        return sValue.equals(dic.get(sKey).toString()) ? "" : sDynSql;
                    }
                    if (isPreGetCondition && !dic.containsKey(sKey)) {
                        dic.put(StaticConstants.dynConditionKeyPre + sKey, sValue);//加上前缀，是为了更好区分这是注释里的动态键
                    }
                } else if (sCond.indexOf("<>") > 0) {
                    //不等于：使用字符比较
                    sOperateStr = "<>";
                    iFinStart = sCond.indexOf(sOperateStr);
                    String sKey = sCond.substring(0, iFinStart).trim();
                    String sValue = sCond.substring(iFinStart + sOperateStr.length()).trim();
                    if (dic.containsKey(sKey))
                    {
                        return sValue.equals(dic.get(sKey).toString()) ? "" : sDynSql;
                    }
                    if (isPreGetCondition && !dic.containsKey(sKey)) {
                        dic.put(StaticConstants.dynConditionKeyPre + sKey, sValue);//加上前缀，是为了更好区分这是注释里的动态键
                    }
                } else {
                    throw new Exception("不支持的动态SQl操作符，只能使用>=、>、<=、<、=、！=、<>这几种单值比较符！原始字符：" + sCond);
                }
            }
            throw new Exception("动态SQl配置错误！！" );
        }
        catch(Exception e) {
            return "";
        }
    }

    /// <summary>
    /// 动态SQL段的In或Not IN判断
    /// </summary>
    /// <param name="dic"></param>
    /// <param name="isPreGetCondition"></param>
    /// <param name="mc"></param>
    /// <param name="sCond"></param>
    /// <param name="sDynSql"></param>
    /// <param name="isNotIn"></param>
    /// <returns></returns>
    private String dynSqlSegmentInOrNotConditionEqual(Map<String, Object> dic, boolean isPreGetCondition, Matcher mc, String sCond, String sDynSql,boolean isNotIn)
    {
        String sKey = sCond.substring(0, mc.start()).trim();
        String sValue = sCond.substring(mc.end()).replace("(", "").replace(")", "").replace("'", "");
        if (dic.containsKey(sKey)) {
            String[] arrNotIn = sValue.split(",");
            for (String item:arrNotIn) {
                if (item.equals(dic.get(sKey).toString())) {
                    return isNotIn ? "":sDynSql; //找到了：针对NOT IN返回空；针对IN返回动态SQL
                }
            }
            return isNotIn ? sDynSql: ""; //没找到：针对NOT IN返回动态SQL；针对IN返回空
        }
        if (isPreGetCondition && !dic.containsKey(sKey)) {
            dic.put(StaticConstants.dynConditionKeyPre + sKey, sValue);//加上前缀，是为了更好区分这是注释里的动态键
        }
        return "";
    }

    /**
     * 生成包括##序号##键方法
     * 目的：为了简化大方面的分析
     * @param sSql
     * @return
     */
    public String generateParenthesesKey(String sSql) {
        Matcher mc;
        StringBuilder sb = new StringBuilder();
        mc = ToolHelper.getMatcher(sSql, StaticConstants.parenthesesPattern);
        //int iGroup = 0;//第几组括号
        int iLeft = 0;//左括号数
        int iRight = 0;//右括号数
        int iGroupStart = 0;//组开始的位置

        while (mc.find()) {
            if("(".equals(mc.group())){
                iLeft++;
                if(iLeft==1){
                    sb.append(sSql.substring(iGroupStart,mc.start()));//注：不要包括左括号
                    iGroupStart = mc.end();
                }
            }else{
                iRight++;
            }
            //判断是否是一组数据
            if(iLeft == iRight){
                String sKey = parenthesesRoundKey + String.valueOf(mapsParentheses.size()) + parenthesesRoundKey;
                //符合左右括号正则式的内容，替换为：##序号##。把最外面的左右括号也放进去
                String sParenthesesSql = "(" + sSql.substring(iGroupStart,mc.start()) + mc.group();
                mapsParentheses.put(sKey,sParenthesesSql);
                sb.append(sKey);//注：不要包括右括号
                iGroupStart = mc.end();//下一个语句的开始
                //iGroup++;
                iLeft = 0;
                iRight = 0;
            }
        }
        //最后的字符
        if(iGroupStart>0){
            sb.append(sSql.substring(iGroupStart));
        }

        String sNewSql = sb.toString();
        if(ToolHelper.IsNull(sNewSql)){
            //没有括号时，调用子查询方法
            return queryHeadSqlConvert(sSql,true);
        }
        return sNewSql;
    }

    /***
     * FROM段SQL的转换（包括WHERE部分）
     * @param sSql
     * @param childQuery 是否子查询
     */
    protected String fromWhereSqlConvert(String sSql,boolean childQuery) {
        StringBuilder sb = new StringBuilder();
        String sSet = "";
        String sFromWhere = "";

        //分隔FROM段
        Matcher mcFrom = ToolHelper.getMatcher(sSql, StaticConstants.fromPattern);
        boolean isDealWhere = false;
        //因为只会有一个FROM，所以这里不用WHILE，而使用if
        if(!mcFrom.find()){
            //1。没有FROM语句
            String sFinalWhere = whereConvert(sSql);
            sb.append(sFinalWhere);
            return sb.toString();
        }

        //2.有FROM，及之后可能存在的WHERE段处理
        sSet = sSql.substring(0, mcFrom.start()).trim();
        sFromWhere = sSql.substring(mcFrom.end()).trim();

        //1、查询语句中查询的字段，或更新语句中的更新项
        if(childQuery){
            String sFinalBeforeFrom = queryBeforeFromConvert(sSet);
            sb.append(sFinalBeforeFrom);//由子类来处理
        }else {
            String sFinalBeforeFrom = beforeFromConvert(sSet);
            sb.append(sFinalBeforeFrom);//由子类来处理
        }

        sb.append(mcFrom.group());//sbHead添加FROM字符

        //2、WHERE段分隔
        Matcher mcWhere = ToolHelper.getMatcher(sFromWhere, StaticConstants.wherePattern);
        String sFrom = "";//
        String sWhere = "";
        //因为只会有一个FROM，所以这里不用WHILE，而使用if
        if (!mcWhere.find()) {
            //没有WHERE段：但后面可能有GROUP BY或ORDER BY或LIMI等项，需要进一步匹配，从而确定FROM段和FROM段字符
            //GROUP BY的处理
            Matcher mcGroupBy = ToolHelper.getMatcher(sSql, StaticConstants.groupByPattern);
            if (mcGroupBy.find()) {
                sFrom = sSql.substring(mcFrom.end(), mcGroupBy.start());
                sWhere = mcGroupBy.group() + sSql.substring(mcGroupBy.end());
            } else {
                //ORDER BY的处理
                Matcher mcOrder = ToolHelper.getMatcher(sSql, StaticConstants.orderByPattern);
                if (mcOrder.find()) {
                    sFrom = sSql.substring(mcFrom.end(), mcOrder.start());
                    sWhere = mcOrder.group() + sSql.substring(mcOrder.end());
                } else {
                    //LIMIT的处理
                    Matcher mcLimit = ToolHelper.getMatcher(sSql, StaticConstants.limitPattern);
                    if (mcLimit.find()) {
                        sFrom = sSql.substring(mcFrom.end(), mcLimit.start());
                        sWhere = mcLimit.group() + sSql.substring(mcLimit.end());
                    } else {
                        //没有GROUP BY、ORDER BY、LIMIT，那么就相当于只有FROM段的内容
                        sFrom = sSql.substring(mcFrom.end());
                        sWhere = "";
                    }
                }
            }
        }
        else
        {
            sFrom = sFromWhere.substring(0, mcWhere.start());//
            sWhere = sFromWhere.substring(mcWhere.end() - mcWhere.group().length());
        }

        //3、FROM段的处理
        if(!hasKey(sFrom)){
            //FROM段没有参数时，直接拼接
            sb.append(sFrom);
            String sFinalWhere = whereConvert(sWhere);
            sb.append(sFinalWhere);

            return sb.toString();
        }

        //4 通过各种Join正则式分解语句
        Matcher mc2 = ToolHelper.getMatcher(sFrom, StaticConstants.joinPattern);
        int iStart2=0;
        int iCount = 0;
        String lastJoin = "";//最后一次JOIN语句的字符，这个在while循环外处理最后一段字符时用到
        while (mc2.find()) {
            String oneJoin = sFrom.substring(iStart2,mc2.start());//第一条JOIN语句
            if(iCount > 0){
                sb.append(lastJoin);
            }
            lastJoin = mc2.group();
            iCount++;
            if(!hasKey(oneJoin)){
                //没有参数，直接拼接
                sb.append(oneJoin);
                //sbHead.append(mc2.group());
                iStart2 = mc2.end();
                continue;//继续下一段处理
            }
            //AND和OR的条件转换
            String sAndOr = andOrConditionConvert(oneJoin);
            sb.append(sAndOr);
            iStart2 = mc2.end();

        }
        sb.append(lastJoin);
        //5 之前正则式中最后一段SQL的AND和OR的条件转换
        String sLastFrom = andOrConditionConvert(sFrom.substring(iStart2));
        sb.append(sLastFrom);

        //6.WHERE段的SQL处理
        String sConvertWhere = whereConvert(sWhere);
        sb.append(sConvertWhere);

        return sb.toString();
    }

    /**
     * where语句的转换
     * @param sSql
     * @return
     */
    private String whereConvert(String sSql) {
        StringBuilder sb = new StringBuilder();
        //二、 如果语句中没有FROM语句，那会直接进入
        Matcher mcWhere = ToolHelper.getMatcher(sSql, StaticConstants.wherePattern);
        if (!mcWhere.find()) {
            //没有Where，那就是直接SELECT部分
            if (!hasKey(sSql)) {
                return sSql; //没有参数时直接返回
            }
            //有键
            String[] keyList = sSql.split(",");
            int iCount = 0;
            for (String item:keyList) {
                String sValue = iCount == 0 ? "" : ",";
                sb.append(sValue + singleKeyConvert(item));
                iCount++;
            }
            return sb.toString();
        }

        //sb.append(sSql.substring(0,mcWhere.start()));//确定FROM部分
        String sWhereString = mcWhere.group();

        //7.GROUP BY的处理
        //6、拆出WHERE至GROUP BY之间的片段
        boolean needWhereSplit = true;//是否需要做WHERE分拆
        boolean needGroupBySplit = false;//是否需要做GroupBy分拆
        boolean needHavingSplit = false;//是否需要做GroupBy分拆
        Matcher mcGroupBy = ToolHelper.getMatcher(sSql, StaticConstants.groupByPattern);
        if (mcGroupBy.find()) {
            needGroupBySplit = true;
            //2.1 AND和OR的条件转换
            String OneSql = sSql.substring(mcWhere.end(),mcGroupBy.start());
            String sConvertWHere = andOrConditionConvert(OneSql);
            if(ToolHelper.IsNotNull(sConvertWHere)){
                sb.append(sWhereString + sConvertWHere);
            }

            sb.append(mcGroupBy.group());
            sSql = sSql.substring(mcGroupBy.end());
            if(!hasKey(sSql)){
                //之后都没有key配置，那么直接将字符加到尾部，然后返回
                sb.append(sSql);
                return sb.toString();
            }

            needWhereSplit = false;
            Matcher mcHaving = ToolHelper.getMatcher(sSql, StaticConstants.havingPattern);
            if (mcHaving.find()) {
                needGroupBySplit = false;
                String sOne = sSql.substring(0,mcHaving.start());
                sOne = andOrConditionConvert(sOne);
                sb.append(sOne);
                sb.append(mcHaving.group());

                sSql = sSql.substring(mcHaving.end());
                needHavingSplit = true;
            }
        }

        //7、拆出ORDER片段
        boolean needOrderSplit = false;
        Matcher mcOrder = ToolHelper.getMatcher(sSql, StaticConstants.orderByPattern);
        if (mcOrder.find()) {
            if(needWhereSplit){
                String sConvertWHere = andOrConditionConvert(sSql.substring(mcWhere.end(),mcOrder.start()));
                if(ToolHelper.IsNotNull(sConvertWHere)){
                    sb.append(sWhereString + sConvertWHere);
                }
                sb.append(mcOrder.group());
                sSql = sSql.substring(mcOrder.end());
                needWhereSplit = false;
                if(!hasKey(sSql)){
                    //之后都没有key配置，那么直接将字符加到尾部，然后返回
                    sb.append(sSql);
                    return sb.toString();
                }
            }
            if(needGroupBySplit){
                String sAndOr = andOrConditionConvert(sSql.substring(0,mcOrder.start()));
                sb.append(sAndOr);
                needGroupBySplit = false;
            }
            if(needHavingSplit){
                String sAndOr = andOrConditionConvert(sSql.substring(0,mcOrder.start()));
                sb.append(sAndOr);
                needHavingSplit = false;
            }
            sb.append(mcOrder.group());
            sSql = sSql.substring(mcOrder.end());
            needOrderSplit = true;
        }

        //8、拆出LIMIT段
        Matcher mcLimit = ToolHelper.getMatcher(sSql, StaticConstants.limitPattern);
        if (mcLimit.find()) {
            if(needWhereSplit){
                String sConvertWHere = andOrConditionConvert(sSql.substring(0,mcLimit.start()));
                if(ToolHelper.IsNotNull(sConvertWHere)){
                    sb.append(sWhereString + sConvertWHere);
                }
            }
            if(needGroupBySplit || needHavingSplit || needOrderSplit){
                String sAndOr = andOrConditionConvert(sSql.substring(0,mcLimit.start()));
                sb.append(sAndOr);
            }

            sb.append(mcLimit.group());
            sSql = sSql.substring(mcLimit.end());
        }

        //9、最后一段字符的处理
        if(ToolHelper.IsNotNull(sSql.trim())){
            String sWhere = "";
            if(needWhereSplit){
                sWhere = sWhereString;//有可能WHERE还未处理
                sSql = sSql.substring(mcWhere.end());
            }

            String sSqlFinal = andOrConditionConvert(sSql);
            if(ToolHelper.IsNotNull(sSqlFinal)){
                sb.append(sWhere + sSqlFinal);
            }
        }
        return sb.toString();
    }

    /**
     * AND和OR的条件转换处理
     * @param sCond 例如：PROVINCE_ID = '#PROVINCE_ID#' AND UPDATE_CONTROL_ID= '#UPDATE_CONTROL_ID#'
     */
    protected String andOrConditionConvert(String sCond) {
        StringBuilder sb = new StringBuilder();
        //1、按AND（OR）正则式匹配
        Matcher mc = ToolHelper.getMatcher(sCond, StaticConstants.andOrPatter);
        int iStart = 0;
        String sBeforeAndOr = "";
        boolean hasGoodCondition = false;
        while (mc.find()) {
            if(!hasGoodCondition){
                sBeforeAndOr = ""; //只要没有一个条件时，前面的AND或OR为空
            }
            //2、得到一个AND或OR段
            String oneSql = sCond.substring(iStart,mc.start());
            //查看是否有：##序号##
            boolean parenthesesRounFlag = false;//没有
            Matcher mc2 = ToolHelper.getMatcher(oneSql, parenthesesRoundKeyPattern);
            while (mc2.find()){
                parenthesesRounFlag = true;
            }
            if(hasKey(oneSql) || parenthesesRounFlag) {
                //2.1、当键存在，或存在：##序号##时，调用括号键转换处理方法
                String sFinalSql = complexParenthesesKeyConvert(oneSql, sBeforeAndOr);
                if(ToolHelper.IsNotNull(sFinalSql)){
                    sb.append(sFinalSql);
                    sBeforeAndOr = mc.group();
                    hasGoodCondition = true;
                }
            }else {
                //2.2、当键存在时，调用括号键转换处理方法
                sb.append(sBeforeAndOr + oneSql);
                sBeforeAndOr = mc.group();
                hasGoodCondition = true;
            }
            iStart = mc.end();
        }
        //最后一个AND或OR之后的的SQL字符串处理，也是调用括号键转换处理方法
        String sComplexSql = complexParenthesesKeyConvert(sCond.substring(iStart),sBeforeAndOr);
        sb.append(sComplexSql);

        return sb.toString();
    }

    protected String complexParenthesesKeyConvert(String sSql, String sLastAndOr){
        return complexParenthesesKeyConvert(sSql,sLastAndOr,false);
    }
    /**
     * 复杂的括号键转换处理：
     *  之前为了降低复杂度，将包含()的子查询或函数替换为##序号##，这里需要取出来分析
     * @param sSql 包含##序号##的SQL
     * @param sLastAndOr 上次处理中最后的那个AND或OR字符
     * @param isSingleColumnDeal 是否单个字段处理，如查询列中也可能包含参数，无没有则直接跳过
     */
    protected String complexParenthesesKeyConvert(String sSql, String sLastAndOr,boolean isSingleColumnDeal){
        StringBuilder sb = new StringBuilder();
        String sValue = "";
        //1、分析是否有包含 ##序号## 正则式的字符
        Matcher mc = ToolHelper.getMatcher(sSql, parenthesesRoundKeyPattern);
        Boolean hasFirstMatcher = mc.find();
        if(!hasFirstMatcher){
            //没有双括号，但可能存在单括号，如是要修改为1=1或AND 1=1 的形式
            return parenthesesConvert(sSql, sLastAndOr);
        }
        String sSqlNew = sSql; //注：在匹配的SQL中，不能修改原字符，不然根据mc.start()或mc.end()取出的子字符会不对!!
        Map<String,String> dicReplace = new HashMap<>();
        //2、有 ##序号## 字符的语句分析：可能会有多个,需要针对每一个##序号##作详细分析
        //比如WITH...INSERT INTO...SELECT和INSERT INTO...WITH...INSERT INTO...
        String sSource = "";
        String sReturn = "";
        int iLastStart = 0;
        while (hasFirstMatcher) {
            sSource = mapsParentheses.get(mc.group());//取出 ##序号## 内容

            if (!hasKey(sSource)) {
                sSqlNew = sSqlNew.replace(mc.group(), sSource);
                dicReplace.put(mc.group(), sSource);  //没有键的字符，先加到集合中。在返回前替换
                //取出下个匹配##序号##的键，如果有，那么继续下个循环去替换##序号##
                hasFirstMatcher = mc.find();
                if (hasFirstMatcher) {
                    //继续取出##序号##键的值来替换：WITH...INSERT INTO...SELECT和INSERT INTO...WITH...INSERT INTO...这两种情况会进入本段代码
                    continue;
                }

                //2.1 如没有##序号##键，那么得到替换并合并之前的AND或OR字符
                String sConnect = sLastAndOr + sSqlNew;
                if (!hasKey(sConnect)) {
                    //2.2 合并后也没有键，则直接追加到头部字符构建器
                    sb.append(sConnect);
                    return sb.toString();
                }
                //2.3 如果有键传入，那么进行单个键转换
                sb.append(singleKeyConvert(sConnect));
                return sb.toString();
            }

            //判断是否所有键为空
            Boolean allKeyNull = true;
            Matcher mc1 = ToolHelper.getMatcher(sSource, StaticConstants.keyPatternHash);
            while (mc1.find()) {
                if (ToolHelper.IsNotNull(singleKeyConvert(mc1.group()))) {
                    allKeyNull = false;
                    break;
                }
            }

            String sPre = sSql.substring(iLastStart, mc.start());
            iLastStart = mc.end();
            String sEnd = sSql.substring(iLastStart); //注：后续部分还可能用##序号##

            //查询单列的动态处理
            if (isSingleColumnDeal)
            {
                if (!hasKey(sSource))
                {
                    return sPre + sSource + sEnd; //无键时直接返回
                }

                String sSingleKeyName = getFirstKeyName(sSource);
                if (!mapSqlKeyValid.containsKey(sSingleKeyName))
                {
                    return ""; //没有值传入，直接返回空
                }
                return sPre + singleKeyConvert(sSource) + sEnd;
            }

            //3、子查询处理
            String sChildQuery = childQueryConvert(sLastAndOr + sPre, "", sSource); //这里先不把结束字符加上
            sb.append(sChildQuery);//加上子查询
            if (allKeyNull || ToolHelper.IsNotNull(sChildQuery)) {
                //取出下个匹配##序号##的键，如果有，那么继续下个循环去替换##序号##
                hasFirstMatcher = mc.find();
                if (hasFirstMatcher)
                {
                    //继续取出##序号##键的值来替换
                    sSqlNew = sEnd;//剩余部分将要被处理
                    continue;
                }
                else
                {
                    sb.append(sEnd);//这里把结束字符加上
                }
                sReturn = sb.toString();
                for(String sKey : dicReplace.keySet()) {
                    sReturn = sReturn.replace(sKey, dicReplace.get(sKey)); //在返回前替换不包含参数的##序号##字符
                }
                return sReturn;//如果全部参数为空，或者子查询已处理，直接返回
            }

            //4、非子查询的处理
            sb.append(sEnd);//这里把结束字符加上
            //判断是否IN表达式
            Matcher mcOnlyIn = ToolHelper.getMatcher(sSql, StaticConstants.onlyInPattern);
            String sInAnd = "";
            String sInColumn = "";
            if (mcOnlyIn.find()) {
                sInAnd = sLastAndOr;
                sInColumn =  sPre; //把列名 IN ()这一段完整加上
            }
            else
            {
                //有键值传入，并且非子查询，做AND或OR正则匹配分拆字符
                sb.append(sLastAndOr + sPre);//因为不能移除"()"，所以这里先拼接收"AND"或"OR"，记得加上头部字符
            }

            //AND或OR正则匹配处理
            // 注：此处虽然与【andOrConditionConvert】有点类似，但有不同，不能将以下代码替换为andOrConditionConvert方法调用
            Matcher mc2 = ToolHelper.getMatcher(sSource, StaticConstants.andOrPatter);
            int iStart = 0;
            String beforeAndOr = "";
            while (mc2.find()) {
                //4.1 存在AND或OR
                String sOne = sSource.substring(iStart, mc2.start()).trim();
                //【括号SQL段转换方法】
                sValue = parenthesesConvert(sOne, beforeAndOr);
                sb.append(sValue);
                iStart = mc2.end();
                beforeAndOr = mc2.group();
            }
            //4.2 最后一个AND或OR之后的的SQL字符串处理，也是调用【括号SQL段转换方法】
            String sEndSql = sInColumn + sSource.substring(iStart);
            sValue = parenthesesConvert(sEndSql, beforeAndOr); //TODO:IN
            sb.append(sInAnd + sValue + sEnd);//加上尾部字符

            hasFirstMatcher = mc.find();//注：这里也要重新给hasFirstMatcher赋值，要不会有死循环
        }

        sReturn = sb.toString();
        for(String sKey:dicReplace.keySet()) {
            sReturn = sReturn.replace(sKey, dicReplace.get(sKey)); //在返回前替换不包含参数的##序号##字符
        }
        return sReturn;

    }

    /**
     * 含括号的SQL段转换
     *   注：已经过AND或OR拆分，只含一个键，并且字符前有左括号，或者字符后有右括号
     *  例如 ( ( CREATOR = '#CREATOR#'、CREATOR_ID = #CREATOR_ID# ) 、 TFLG = '#TFLG#')
     * @param sSql 只有一个key的字符（即已经过AND或OR的正则表达式匹配后分拆出来的部分字符）
     * @param sLastAndOr 前一个拼接的AND或OR字符
     */
    private String parenthesesConvert(String sSql, String sLastAndOr) {
        //1、剔除开头的一个或多个左括号，并且把这些左括号记录到变量中，方便后面拼接
        String sOne = sSql;
        String sStartsParentheses="";
        while (sOne.startsWith("(")){
            sStartsParentheses += "(";
            sOne = sOne.substring(1).trim(); //remvoe the start position of String "("
        }

        //2、剔除结尾处的一个或多个括号，并将它记录到变量中，方便后面拼接
        String sEndRight = "";
        int leftCount = sOne.length() - sOne.replace("(","").length();//left Parentheses count
        long rightCount = sOne.length() - sOne.replace(")","").length();//right Parentheses count

        if(leftCount != rightCount){
            while (rightCount-leftCount>0){
                sEndRight+=")";
                sOne = sOne.substring(0,sOne.length()-1).trim();
                rightCount--;
            }
        }

        String sParmFinal = singleKeyConvert(sOne);//有括号也一并去掉了
        if(ToolHelper.IsNull(sParmFinal)){
            //没有键值传入
            if(ToolHelper.IsNotNull(sStartsParentheses) || ToolHelper.IsNotNull(sEndRight)){
                //有左或右括号时，就替换为AND 1=1
                sLastAndOr = sLastAndOr.replace("OR","AND");
                return sLastAndOr + sStartsParentheses + " 1=1 " + sEndRight;
            }
            return "";//没有括号时返回空，即可以直接去掉
        }
        else {
            //IN清单的括号在里边已组装
            return sLastAndOr + sStartsParentheses + sParmFinal + sEndRight;//有键值传入
        }

    }

    /**
     * 子查询转换
     * @param sPre 前缀
     * @param sEnd 后缀
     * @param sSource ##序号##的具体内容
     * @return
     */
    private String childQueryConvert(String sPre, String sEnd, String sSource) {
        StringBuilder sb = new StringBuilder();
        //1、判断是否有子查询:抽取出子查询的 (SELECT 部分
        Matcher mcChild = ToolHelper.getMatcher(sSource, StaticConstants.childSelectPattern);
        if (!mcChild.find()) {
            return "";//没有子查询，返回空
        }

        //2、有子查询，将开头的一个或多个 ( 追加到头部字符构造器，这样剥开才能找到真正的参数控制部分的字符串
        sb.append(sPre);//拼接子查询前缀 (SELECT
        while(sSource.startsWith("(")) {
            sb.append("(");
            sSource = sSource.substring(1).trim();
        }
        //3、结束位置 ) 的处理：如右括号数与左括数不相等，那么将右括号超过左括号的数量追加到尾部构造器。这样对于里边有方法的()能轻松处理！！
        String sEndRight = "";
        int leftCount = sSource.length() - sSource.replace("(","").length();//左括号数
        long rightCount = sSource.length() - sSource.replace(")","").length();//右括号数
        if(leftCount != rightCount){
            //二者不等时，再根据右括号超过左括号的差值，递减到0为空。即左右括号数相等
            while (rightCount-leftCount>0){
                sEndRight+=")"; //追加右括号到尾部构造器
                sSource = sSource.substring(0, sSource.length()-1).trim();//去掉尾部的右括号
                rightCount--;
            }
        }

        //子查询中又可能存在子查询，所以这里还要对括号进一步分析
        sSource = generateParenthesesKey(sSource);
        if(!hasKey(sSource)){
            sb.append(sSource);//这里有可能已处理完子查询
        } else {
            /** 4、子查询又相当于一个SELECT语句，这里又存在FROM和WHERE处理，所以这部分是根据SELECT模式，再解析一次。
             *   这就是为何将queryHeadSqlConvert和queryBeforeFromConvert放在本抽象父类的缘故。
             */
            mcChild = ToolHelper.getMatcher(sSource, StaticConstants.selectPattern);//抽取出SELECT部分
            while (mcChild.find()) {
                //4.1 调用查询头部转换方法
                String sSqlChild = queryHeadSqlConvert(sSource,true);
                sb.append(sSqlChild);
            }
        }
        sb.append(sEndRight);//追加右括号
        sb.append(sEnd);//追加 ##序号## 之后部分字符
        return sb.toString(); //返回子查询已处理
    }

    /****
     * 单个键SQL转换：一般在对AND（OR）分隔后调用本方法
     * @param sSql: 例如："[PROVINCE_CODE] = '#PROVINCE_CODE#'" 或 ",[PROVINCE_NAME] = '#PROVINCE_NAME#'"
     * @return
     */
    protected String singleKeyConvert(String sSql){
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.keyPatternHash);
        while (mc.find()){
            String sKey = ToolHelper.getKeyName(mc.group(), myPeachProp);
            if(!mapSqlKeyValid.containsKey(sKey)){
                return ""; //1、没有值传入，直接返回空
            }
            SqlKeyValueEntity entity = mapSqlKeyValid.get(sKey);
            String sList = entity.getKeyMoreInfo().getInString();
            //最终值处理标志
            if(ToolHelper.IsNotNull(sList)){
                String[] sInArr = sList.split(",");
                int iMaxIn = entity.getKeyMoreInfo().getPerInListMax() > 0 ? entity.getKeyMoreInfo().getPerInListMax() : myPeachProp.inMax;
                double dCount = sInArr.length * 1.0 / iMaxIn;
                int iCount = (int)Math.ceil(dCount);
                if (iCount <= 1) {
                    return sSql.replace(mc.group(), sList);//替换IN的字符串
                }
                else
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("(");
                    for (int i = 0; i < iCount; i++) {
                        List list = Arrays.stream(sInArr).skip((long)(i * iMaxIn)).limit((long)iMaxIn).collect(Collectors.toList());
                        String sOne = String.join(",", list);
                        if (i == 0)
                        {
                            String sOneIn = sSql.replace(mc.group(), sOne);
                            sb.append(sOneIn + " ");
                        }
                        else
                        {
                            String sOneIn = "OR " + entity.getKeyMoreInfo().getInColumnName() + " IN (" + sOne + ") ";
                            sb.append(sOneIn + " ");
                        }
                    }
                    sb.append(")");
                    return sb.toString();
                }
            }
            if(entity.getKeyMoreInfo().isMustValueReplace() || myPeachProp.getTargetSqlParamTypeEnum() == TargetSqlParamTypeEnum.DIRECT_RUN){
                //2、返回替换键后只有值的SQL语句
                return sSql.replace(mc.group(), String.valueOf(entity.getReplaceKeyWithValue()));
            }
            //3、返回参数化的SQL语句：LIKE的问题是在值的前或后或两边加上%解决
            if(myPeachProp.getTargetSqlParamTypeEnum() == TargetSqlParamTypeEnum.NameParam){
                return sSql.replace(mc.group(), myPeachProp.getParamPrefix()+sKey+ myPeachProp.getParamSuffix());
            }
            return sSql.replace(mc.group(), "?");
        }
        return sSql;//4、没有键时，直接返回原语句
    }

    /**
     *获取第一个键的字符串
     * @param sSql
     * @return 例如：'%#CITY_NAME#%'
     */
    protected String getFirstKeyString(String sSql){
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.keyPatternHash);
        if (mc.find()) {
            return mc.group();
        }else {
            return "";
        }
    }

    /**
     *获取第一个键的键名
     * @param sSql
     * @return 例如：CITY_NAME
     */
    protected String getFirstKeyName(String sSql){
        String sParamString = getFirstKeyString(sSql);
        return ToolHelper.getKeyName(sParamString, myPeachProp);
    }

    /**
     * 判断SQL是否有键
     * @param sSql
     * @return
     */
    protected boolean hasKey(String sSql){
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.keyPatternHash);
        boolean hasPara = false;
        while (mc.find()) {
            hasPara = true;
            break;
        }
        return hasPara;
    }

    /***
     * 查询的头部处理
     * 注：放这里的原因是INSERT INTO ... SELECT 语句也用到该方法
     * @param sSql
     */
    protected String queryHeadSqlConvert(String sSql,boolean childQuery) {
        StringBuilder sb = new StringBuilder();
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.selectPattern);//抽取出SELECT部分
        if (mc.find()){
            sb.append(mc.group());//不变的SELECT部分先加入
            sSql = sSql.substring(mc.end()).trim();
            //UNION 或 UNION ALL的处理
            sSql = unionOrUnionAllConvert(sSql, sb);
            if (ToolHelper.IsNull(sSql)) {
                return sb.toString();
            }
            //非UNION 且 非UNION ALL的处理
            String sFinalSql = fromWhereSqlConvert(sSql,childQuery);
            sb.append(sFinalSql);
        } else {
            //传过来的SQL有可能去掉了SELECT部分
            //UNION 或 UNION ALL的处理
            sSql = unionOrUnionAllConvert(sSql, sb);
            if (ToolHelper.IsNull(sSql)) {
                return sb.toString();
            }
            String sFinalSql = fromWhereSqlConvert(sSql,childQuery);
            sb.append(sFinalSql);
        }
        return  sb.toString();
    }

    /***
     * 查询的FROM前段SQL处理
     * 注：放这里的原因是INSERT INTO ... SELECT 语句也用到该方法
     * @param sSql
     */
    protected String queryBeforeFromConvert(String sSql) {
        StringBuilder sb = new StringBuilder();
        String[] sSelectItemArray = sSql.split(",");
        String sComma="";
        for (String col:sSelectItemArray) {
            //查看是否有：##序号##
            boolean parenthesesRounFlag = false;//没有
            Matcher mc = ToolHelper.getMatcher(col, parenthesesRoundKeyPattern);
            if(mc.find()){
                parenthesesRounFlag = true;
            }
            if(!hasKey(col) && !parenthesesRounFlag){
                sb.append(sComma + col);
                sComma = ",";
                continue;
            }
            //括号转换处理
            String colString = complexParenthesesKeyConvert(sComma + col, "", true);
            if(!hasKey(colString))
            {
                sb.append(colString);
                sComma = ",";
                continue;
            }

            String sKey = getFirstKeyName(colString);
            if (mapSqlKeyValid.containsKey(sKey))
            {
                sb.append(singleKeyConvert(colString));
                sComma = ",";
            }

            if (sComma.isEmpty())
            {
                sComma = ",";
            }
        }

        return sb.toString();
    }

    protected String dealUpdateSetItem(String sSql)
    {
        StringBuilder sb = new StringBuilder();
        String[] sSetArray = sSql.split(",");
        String sComma = "";
        for (String col:sSetArray) {
            if (!hasKey(col)) {
                sb.append(sComma + col);
                sComma = ",";
                continue;
            }

            sb.append(complexParenthesesKeyConvert(sComma + col, ""));

            if (sComma.isEmpty()) {
                String sKey = getFirstKeyName(col);
                if (mapSqlKeyValid.containsKey(sKey)) {
                    sComma = ",";
                }
            }
        }
        return sb.toString();
    }

    protected String dealInsertItemAndValue(String sSql, StringBuilder sbHead, StringBuilder sbTail)
    {
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.valuesPattern);//先根据VALUES关键字将字符分隔为两部分
        if (!mc.find()) {
            return sSql;
        }
        String sInsert = "";
        String sPara = "";

        String sInsertKey = sSql.substring(0, mc.start()).trim();
        String sParaKey = sSql.substring(mc.end()).trim();

        sInsert = ToolHelper.removeBeginEndParentheses(mapsParentheses.get(sInsertKey));
        sPara = ToolHelper.removeBeginEndParentheses(mapsParentheses.get(sParaKey));
        sPara = generateParenthesesKey(sPara);//针对有括号的部分先替换为##序号##

        sbHead.append("(");//加入(
        sbTail.append(mc.group() + "(");//加入VALUES(

        //3、 insert into ... values形式
        String[] colArray = sInsert.split(",");
        String[] paramArray = sPara.split(",");

        int iGood = 0;
        for (int i = 0; i < colArray.length; i++) {
            String sOneParam = paramArray[i];
            String sParamSql = complexParenthesesKeyConvert(sOneParam, "");
            if (ToolHelper.IsNotNull(sParamSql)) {
                if (iGood == 0) {
                    sbHead.append(colArray[i]);
                    sbTail.append(sParamSql);
                } else {
                    sbHead.append("," + colArray[i]);
                    sbTail.append("," + sParamSql);
                }
                iGood++;
            }
        }
        sbHead.append(")");
        sbTail.append(")");
        sSql = "";//处理完毕清空SQL
        return sSql;
    }

    /// <summary>
    /// UNION 或 UNION ALL 或 其他处理
    /// </summary>
    /// <param name="sSql">处理前SQL</param>
    /// <param name="sbHead">处理后的拼接SQL</param>
    /// <returns></returns>
    protected String unionOrUnionAllConvert(String sSql, StringBuilder sbHead)
    {
        //UNION和UNION ALL处理
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.unionAllPartner);
        int iStart = 0;
        while (mc.find())
        {
            String sOne = sSql.substring(iStart, mc.start());
            String sConvertSql = queryHeadSqlConvert(sOne, false);
            sbHead.append(sConvertSql);
            iStart = mc.end();
            sbHead.append(mc.group());
        }

        if (iStart > 0)
        {
            //UNION或UNION ALL处理剩下部分的处理
            String sOne = sSql.substring(iStart);
            String sConvertSql = queryHeadSqlConvert(sOne, false);
            sbHead.append(sConvertSql);
            return "";
        }
        return sSql;
    }

    /**
     * 头部SQL转换：子类实现
     * @param sSql
     */
    protected abstract String headSqlConvert(String sSql);

    /**
     * FROM前段的SQL转换：子类实现
     * @param sSql
     */
    protected abstract String beforeFromConvert(String sSql);

    /**
     * 是否正确SQL类型抽象方法
     * @param sSql
     */
    public abstract boolean isRightSqlType(String sSql);
}
