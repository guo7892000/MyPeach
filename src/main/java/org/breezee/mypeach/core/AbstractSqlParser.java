package org.breezee.mypeach.core;

import lombok.extern.slf4j.Slf4j;
import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.config.StaticConstants;
import org.breezee.mypeach.entity.*;
import org.breezee.mypeach.enums.SqlKeyStyleEnum;
import org.breezee.mypeach.enums.SqlTypeEnum;
import org.breezee.mypeach.enums.TargetSqlParamTypeEnum;
import org.breezee.mypeach.utils.ToolHelper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * @objectName: SQL分析抽象类
 * @description: 作为SELECT、INSERT、UPDATE、DELETE分析器的父类，包含SQL的前置处理，如SQL大定、去掉注释、提取键等。
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/12 21:52
 */
@Slf4j
public abstract class AbstractSqlParser {

    protected MyPeachProperties myPeachProp;
    protected String keyPrefix = "#";
    protected String keySuffix = "#";
    protected String keyPattern;//键正则式
    String sParenthesesPattern="\\(.+\\)";//left Parentheses patter

    /**
     * 优先处理的括号会被替换的两边字符加中间一个序号值，例如：##1##
     * 如其跟键前缀重复，则会在后面增加一个#号
     *
     */
    private String parenthesesRoundKey = "##";

    protected Map<String, SqlKeyValueEntity> mapSqlKey;//SQL中所有键
    protected Map<String, SqlKeyValueEntity> mapSqlKeyValid;//SQL中有传值的所有键

    Map<String,String> mapsParentheses;//优先处理的括号集合
    public Map<String, String> mapError;//错误信息Map

    protected StringBuilder sbHead = new StringBuilder();//头部字符构建者
    protected StringBuilder sbTail = new StringBuilder();//尾部字符构建者
    protected SqlTypeEnum sqlTypeEnum;
    ParserResult result;

    /***
     * 构造函数：初始化所有变量
     * @param prop 全局配置
     */
    public AbstractSqlParser(MyPeachProperties prop){
        myPeachProp = prop;

        if(prop.getKeyStyle()== SqlKeyStyleEnum.POUND_SIGN_BRACKETS){
            keyPrefix = StaticConstants.HASH_LEFT_BRACE;
            keySuffix = StaticConstants.RIGHT_BRACE;

            //还要支持类似：AND MODIFIER IN ('#MDLIST:N:LS:L-S#')的键
            keyPattern = "'?%?\\#\\{\\w+(:\\w+(-\\w+)?)*\\}%?'?";//键正则式，注这里针对#{}都要加上转义符，否则会报错！！
        }else {
            keyPrefix = StaticConstants.HASH;
            keySuffix = StaticConstants.HASH;
            //还要支持类似：AND MODIFIER IN ('#MDLIST:N:LS:L-S#')的键
            keyPattern = "'?%?"+ keyPrefix +"\\w+(:\\w+(-\\w+)?)*"+ keySuffix +"%?'?";//键正则式
        }
        if(parenthesesRoundKey.equals(keyPrefix)){
            parenthesesRoundKey+=StaticConstants.HASH;
        }

        mapsParentheses = new HashMap<>();
        mapSqlKey = new HashMap<>();
        mapSqlKeyValid = new HashMap<>();
        mapError = new HashMap<>();
        result = new ParserResult();
    }

    /**
     * 转换SQL（主入口方法）
     * @param sSql
     * @param dic
     * @return
     */
    public ParserResult parse(String sSql, Map<String, Object> dic){
        sbHead = new StringBuilder();//重新初始化sbHead
        sbTail = new StringBuilder();//重新初始化sbTail
        sSql = sSql.trim().toUpperCase();//将SQL转换为大写

        //1、删除所有注释，降低分析难度，提高准确性
        Pattern regex = Pattern.compile(StaticConstants.remarkPatter,CASE_INSENSITIVE);//Pattern：explanatory note
        Matcher mc = regex.matcher(sSql);
        while (mc.find()) {
            sSql = sSql.replace(mc.group(),"");//删除所有注释
            log.debug("Remove explanatory note:",sSql);
        }

        //2、获取SQL所有参数信息
        regex = Pattern.compile(keyPattern,CASE_INSENSITIVE);//Pattern：explanatory note
        mc = regex.matcher(sSql);
        while (mc.find()) {
            String sParamName = ToolHelper.getKeyName(mc.group(), myPeachProp);
            if(!mapSqlKey.containsKey(sParamName)){
                SqlKeyValueEntity param = SqlKeyValueEntity.build(mc.group(), dic, myPeachProp);
                mapSqlKey.put(sParamName,param);
                if(param.isHasValue()){
                    mapSqlKeyValid.put(sParamName,param);//有传值的键
                }
                if(ToolHelper.IsNotNull(param.getErrorMessage())){
                    mapError.put(sParamName,param.getErrorMessage());//错误列表
                }
            }
        }

        if(mapError.size()>0){
            ParserResult.fail("部分非空键（"+String.join(",",mapError.keySet())+"）没有传入值，已退出！",mapError);
            return result;
        }

        //3、得到符合左右括号正则式的内容，并替换为类似：##序号##格式，方便先从大方面分析结构，之后再取出括号里的内容来进一步分析
        regex = Pattern.compile(sParenthesesPattern,CASE_INSENSITIVE);//Pattern：Parentheses
        mc = regex.matcher(sSql);
        int iStart = 0;
        while (mc.find()) {
            String sKey = parenthesesRoundKey + String.valueOf(iStart) + parenthesesRoundKey;
            mapsParentheses.put(sKey,mc.group());
            sSql = sSql.replace(mc.group(),sKey);//符合左右括号正则式的内容，替换为：##序号##
            iStart++;
        }
        //4、头部处理：交给字类来做
        headSqlConvert(sSql);
        ParserResult result;
        String sFinalSql = sbHead.toString() + " " + sbTail.toString();
        //5、返回最终结果
        if(sFinalSql.isEmpty()){
            result = ParserResult.fail("转换失败，原因不明。",mapError);
        } else {
            result = ParserResult.success(sFinalSql, mapSqlKeyValid);
            result.setSql(sFinalSql);
            result.setMapQuery(mapSqlKeyValid);
        }
        return result;
    }

    /***
     * FROM段SQL的转换（包括WHERE部分）
     * @param sSql
     */
    protected void fromSqlConvert(String sSql) {
        String sSet = "";
        String sFromWhere = "";

        //分隔FROM段
        Pattern regex = Pattern.compile(StaticConstants.fromPattern, CASE_INSENSITIVE);//根据FROM关键字将字符分隔为两部分
        Matcher mc = regex.matcher(sSql);
        boolean isDealWhere = false;//是否处理过WHERE语句
        while (mc.find()) {
            //一、FROM及之后WHERE端的处理
            sSet = sSql.substring(0, mc.start()).trim();
            sFromWhere = sSql.substring(mc.end()).trim();

            //1、查询语句中查询的字段，或更新语句中的更新项
            beforeFromConvert(sSet);//由子类来处理
            sbHead.append(mc.group());//sbHead添加FROM字符

            //2、WHERE段分隔
            Pattern regexInner = Pattern.compile(StaticConstants.wherePattern,CASE_INSENSITIVE);//先根据WHERE关键字将字符分隔为两部分
            Matcher mcWhere = regexInner.matcher(sFromWhere);
            while (mcWhere.find()) {
                //3、FROM段的处理
                String sFrom = sFromWhere.substring(0,mcWhere.start());
                if(!hasKey(sFrom)){
                    //FROM段没有参数时，直接拼接
                    sbHead.append(sFrom);
                    sbHead.append(mcWhere.group());
                    //WHERE条件的处理
                    String sCondition = sFromWhere.substring(mcWhere.end());
                    //AND和OR的条件转换
                    andOrConditionConvert(sCondition);
                    break;//中断本次处理
                }

                //4 通过各种Join正则式分解语句
                Pattern regex2 = Pattern.compile("\\s*((LEFT)|(RIGHT)|(FULL)|(INNER))?\\s+JOIN\\s*",CASE_INSENSITIVE);
                Matcher mc2 = regex2.matcher(sFrom);
                int iStart2=0;
                String lastJoin = "";//最后一次JOIN语句的字符，这个在while循环外处理最后一段字符时用到
                while (mc2.find()) {
                    String oneJoin = sFrom.substring(iStart2,mc2.start());//第一条JOIN语句
                    lastJoin = mc2.group();
                    if(!hasKey(oneJoin)){
                        //没有参数，直接拼接
                        sbHead.append(oneJoin);
                        //sbHead.append(mc2.group());
                        iStart2 = mc2.end();
                        continue;//继续下一段处理
                    }
                    //AND和OR的条件转换
                    andOrConditionConvert(oneJoin);
                    iStart2 = mc2.end();
                }
                sbHead.append(lastJoin);
                //5 之前正则式中最后一段SQL的AND和OR的条件转换
                andOrConditionConvert(sFrom.substring(iStart2));

                //6.WHERE段的SQL处理
                String sWhereString = mcWhere.group();
                sbHead.append(sWhereString);//添加上WHERE
                int iLength = sbHead.length();
                //6.1 AND和OR的条件转换
                andOrConditionConvert(sFromWhere.substring(mcWhere.end()));
                //6.2、如果所有条件都为空，即sbHead的长度没变
                if(iLength == sbHead.length()){
                    sbHead.delete(iLength-sWhereString.length(),iLength);//移除多余的WHER字符，因为WHERE后面没有条件，不过一般这种情况很少见
                }

            }
            isDealWhere = true;
        }

        if(!isDealWhere){
            //二、 如果语句中没有FROM语句，那会直接进入
            Pattern regexInner = Pattern.compile(StaticConstants.wherePattern,CASE_INSENSITIVE);//先根据WHERE关键字将字符分隔为两部分
            Matcher mcWhere = regexInner.matcher(sSql);
            while (mcWhere.find()) {
                String sWhereString = mcWhere.group();
                sbHead.append(sWhereString);
                int iLength = sbHead.length();
                //2.1 AND和OR的条件转换
                andOrConditionConvert(sSql.substring(mcWhere.end()));
                //2.2、如果所有条件都为空，即sbHead的长度没变
                if(iLength == sbHead.length()){
                    sbHead.delete(iLength-sWhereString.length(),iLength);//移除多余的WHER字符，因为WHERE后面没有条件，不过一般这种情况很少见
                }
            }
        }
    }

    /**
     * AND和OR的条件转换处理
     * @param sCond 例如：PROVINCE_ID = '#PROVINCE_ID#' AND UPDATE_CONTROL_ID= '#UPDATE_CONTROL_ID#'
     */
    protected void andOrConditionConvert(String sCond) {
        //1、按AND（OR）正则式匹配
        Pattern regex = Pattern.compile(StaticConstants.andOrPatter, CASE_INSENSITIVE);
        Matcher mc = regex.matcher(sCond);
        int iStart = 0;
        String sBeforeAndOr = "";
        while (mc.find()) {
            //2、得到一个AND或OR段
            String oneSql = sCond.substring(iStart,mc.start());
            //查看是否有：##序号##
            boolean parenthesesRounFlag = false;//没有
            Pattern regex2 = Pattern.compile(parenthesesRoundKey + "\\d+" + parenthesesRoundKey,CASE_INSENSITIVE);
            Matcher mc2 = regex2.matcher(oneSql);
            if(mc2.find()){
                parenthesesRounFlag = true;
            }
            if(hasKey(oneSql) || parenthesesRounFlag) {
                //2.1、当键存在，或存在：##序号##时，调用括号键转换处理方法
                parenthesesKeyConvert(oneSql, sBeforeAndOr);
            }else {
                //2.2、当键存在时，调用括号键转换处理方法
                sbHead.append(sBeforeAndOr + oneSql);
            }
            sBeforeAndOr = mc.group();
            iStart = mc.end();
        }
        //最后一个AND或OR之后的的SQL字符串处理，也是调用括号键转换处理方法
        parenthesesKeyConvert(sCond.substring(iStart),sBeforeAndOr);
    }

    /**
     * 括号键转换处理：
     *  之前为了降低复杂度，将包含()的子查询或函数替换为##序号##，这里需要取出来分析
     * @param sSql 包含##序号##的SQL
     * @param sLastAndOr 上次处理中最后的那个AND或OR字符
     */
    protected void parenthesesKeyConvert(String sSql, String sLastAndOr){
        //1、分析是否有包含 ##序号## 正则式的字符
        Pattern regex = Pattern.compile(parenthesesRoundKey + "\\d+" + parenthesesRoundKey,CASE_INSENSITIVE);
        Matcher mc = regex.matcher(sSql);
        if(!mc.find()){
            //没找到时，直接调用单个键转换
            sbHead.append(singleKeyConvert(sLastAndOr + sSql));//may be has param
            return;//退出本次处理
        }

        //2、有 ##序号## 字符的语句分析
        String sSource = mapsParentheses.get(mc.group());//取出 ##序号## 内容
        if(!hasKey(sSource)){
            //2.1 没有键，得到替换并合并之前的AND或OR字符
            String sConnect = sLastAndOr + sSql.replace(mc.group(),sSource);
            if(!hasKey(sConnect)){
                //2.2 合并后也没有键，则直接追加到头部字符构建器
                sbHead.append(sConnect);
                return;
            }
            //2.3 如果有键传入，那么进行单个键转换
            sbHead.append(singleKeyConvert(sConnect));
            return;
        }

        //判断是否所有键为空
        boolean allKeyNull = true;
        Pattern regex1 = Pattern.compile(keyPattern,CASE_INSENSITIVE);
        Matcher mc1 = regex1.matcher(sSource);
        while (mc1.find()){
            if(ToolHelper.IsNotNull(singleKeyConvert(mc1.group()))){
                allKeyNull =false;
            }
        }

        String sPre = sSql.substring(0, mc.start());
        String sEnd = sSql.substring(mc.end());
        //3、子查询处理
        boolean hasChildQuery = childQueryConvert(sLastAndOr + sPre, sEnd, sSource,allKeyNull);
        if(allKeyNull || hasChildQuery){
            return;//如果全部参数为空，或者子查询已处理，直接返回
        }
        //4、有键值传入，并且非子查询，做AND或OR正则匹配分拆字符
        sbHead.append(sLastAndOr + sSql.replace(mc.group(),""));//因为不能移除"()"，所以这里先拼接收"AND"或"OR"
        //AND或OR正则匹配处理
        // 注：此处虽然与【andOrConditionConvert】有点类似，但有不同，不能将以下代码替换为andOrConditionConvert方法调用
        Pattern regex2 = Pattern.compile(StaticConstants.andOrPatter,CASE_INSENSITIVE);
        Matcher mc2 = regex2.matcher(sSource);
        int iStart = 0;
        String beforeAndOr = "";
        boolean bFirst = true;
        while (mc2.find()){
            //4.1 存在AND或OR
            String sOne = sSource.substring(iStart,mc2.start()).trim();
            //复杂的包含左右括号的SQL段转换（非子查询）
            conplexParenthesesConvert(sOne,beforeAndOr,bFirst);
            iStart = mc2.end();
            beforeAndOr = mc2.group();
            bFirst = false;
        }
        //4.2 最后一个AND或OR之后的的SQL字符串处理，也是调用【复杂的包含左右括号的SQL段转换（非子查询）】方法
        conplexParenthesesConvert(beforeAndOr + sSource.substring(iStart),"",bFirst);
    }

    /**
     * 子查询转换
     * @param sPre 前缀
     * @param sEnd 后缀
     * @param sSource ##序号##的具体内容
     * @param allParamEmpty 所有键是否为空
     * @return
     */
    private boolean childQueryConvert(String sPre, String sEnd, String sSource,boolean allParamEmpty) {
        //1、判断是否有子查询
        Pattern regexChild = Pattern.compile("\\(SELECT\\s+");//抽取出子查询的 (SELECT 部分
        Matcher mcChild = regexChild.matcher(sSource);
        if (!mcChild.find()) {
            return false;//没有子查询，返回false
        }

        //2、有子查询，将开头的一个或多个 ( 追加到头部字符构造器，这样剥开才能找到真正的参数控制部分的字符串
        sbHead.append(sPre);//拼接子查询前缀 (SELECT
        while(sSource.startsWith("(")) {
            sbHead.append("(");
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

        /** 4、子查询又相当于一个SELECT语句，这里又存在FROM和WHERE处理，所以这部分是根据SELECT模式，再解析一次。
        *   这就是为何将queryHeadSqlConvert和queryBeforeFromConvert放在本抽象父类的缘故。
        */
        regexChild = Pattern.compile(StaticConstants.selectPattern);//抽取出SELECT部分
        mcChild = regexChild.matcher(sSource);
        while (mcChild.find()) {
            //4.1 调用查询头部转换方法
            queryHeadSqlConvert(sSource);
        }
        sbHead.append(sEndRight);//追加右括号
        sbHead.append(sEnd);//追加 ##序号## 之后部分字符
        return true; //返回子查询已处理
    }

    /**
     * 复杂的包含左右括号的SQL段转换（非子查询），例如( ( CREATOR = '#CREATOR#' OR CREATOR_ID = #CREATOR_ID# ) AND TFLG = '#TFLG#')
     * @param sOne 只有一个key的字符（即已经过AND或OR的正则表达式匹配后分拆出来的部分字符）
     * @param beforeAndOr 前一个拼接的AND或OR字符
     * @param bFirst 是否第一次接拼
     */
    private void conplexParenthesesConvert(String sOne,String beforeAndOr,boolean bFirst) {
        //1、剔除开头的一个或多个左括号，并且把这些左括号记录到变量中，方便后面拼接
        String sStartsParentheses="";
        while (sOne.startsWith("(")){ //remvoe the start position of string "("
            sStartsParentheses += "(";
            sOne = sOne.substring(1).trim();
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

        //3、判断键是否有传值
        String keyString = getFirstKeyString(sOne);
        String keyName = ToolHelper.getKeyName(keyString, myPeachProp);//根据键字符得到键名
        if(!mapSqlKeyValid.containsKey(keyName)){
            //3.1 没有传值
            String sCon = "";//
            if(bFirst){
                //3.1.1 第一次拼妆：当没有左括号时，直接取 1=1 ，否则在 1=1 前面还要加上左括号变量(包含一个或多个左括号)
                sCon = sStartsParentheses.isEmpty()? " 1=1 ": " " + sStartsParentheses + " 1=1 ";//
            } else {
                //3.1.2 非第一次拼妆：当没有左括号时，直接取 AND 1=1 ，否则在AND 与 1=1 之间要加上左括号变量(包含一个或多个左括号)
                //注：对于没有键值传进来的参数会统一修改为：AND 1=1，即使之前为OR连接条件。因为OR 1=1会查询全部数据，这样就会导致很多错误的数据被更新！！这里很好避免了这个问题^_^
                sCon = sStartsParentheses.isEmpty()? " AND 1=1 ": " AND " + sStartsParentheses + " 1=1 ";//
            }
            sbHead.append(sCon + sEndRight);
        }else {
            //3.2 有传值
            SqlKeyValueEntity entity = mapSqlKeyValid.get(keyName);
            String sList = entity.getKeyMoreInfo().getStringList();
            String sKeyValue;
            if(ToolHelper.IsNotNull(sList)){
                //3.2.1、替换IN的字符串
                sKeyValue = sOne.replace(keyString, sList);
            } else if(myPeachProp.getTargetSqlParamTypeEnum() == TargetSqlParamTypeEnum.Param){
                //3.2.2、得到参数化的SQL语句
                sKeyValue = sOne.replace(keyString,ToolHelper.getTargetParamName(keyName, myPeachProp));
            }else {
                //3.2.3、得到替换键后只有值的SQL语句
                String sValue = String.valueOf(entity.getReplaceKeyWithValue());
                sKeyValue = sOne.replace(keyString,sValue);
            }
            String sAndOr = beforeAndOr + sStartsParentheses + sKeyValue + sEndRight;
            sbHead.append(sAndOr);
        }
    }

    /****
     * 单个键SQL转换：一般在对AND（OR）分隔后调用本方法
     * @param sSql: 例如："[PROVINCE_CODE] = '#PROVINCE_CODE#'" 或 ",[PROVINCE_NAME] = '#PROVINCE_NAME#'"
     * @return
     */
    protected String singleKeyConvert(String sSql){
        Pattern regex = Pattern.compile(keyPattern,CASE_INSENSITIVE);//AND条件处理
        Matcher mc = regex.matcher(sSql);
        while (mc.find()){
            String sKey = ToolHelper.getKeyName(mc.group(), myPeachProp);
            if(!mapSqlKeyValid.containsKey(sKey)){
                return ""; //1、没有值传入，直接返回空
            }
            SqlKeyValueEntity entity = mapSqlKeyValid.get(sKey);
            String sList = entity.getKeyMoreInfo().getStringList();
            //最终值处理标志
            if(ToolHelper.IsNotNull(sList)){
                return sSql.replace(mc.group(), sList);//替换IN的字符串
            }

            if(myPeachProp.getTargetSqlParamTypeEnum() == TargetSqlParamTypeEnum.Param ){
                //2、返回参数化的SQL语句
                return sSql.replace(mc.group(), myPeachProp.getParamPrefix()+sKey+ myPeachProp.getParamSuffix());
            }
            //3、返回替换键后只有值的SQL语句
            return sSql.replace(mc.group(), String.valueOf(entity.getReplaceKeyWithValue()));
        }
        return sSql;//4、没有键时，直接返回原语句
    }

    /**
     *获取第一个键的字符串
     * @param sSql
     * @return 例如：'%#CITY_NAME#%'
     */
    protected String getFirstKeyString(String sSql){
        Pattern regex = Pattern.compile(keyPattern,CASE_INSENSITIVE);
        Matcher mc = regex.matcher(sSql);
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
        Pattern regex = Pattern.compile(keyPattern,CASE_INSENSITIVE);//AND条件处理
        Matcher mc = regex.matcher(sSql);
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
    protected void queryHeadSqlConvert(String sSql) {
        Pattern regex = Pattern.compile(StaticConstants.selectPattern);//抽取出SELECT部分
        Matcher mc = regex.matcher(sSql);
        while (mc.find()){
            sbHead.append(mc.group());//不变的SELECT部分先加入
            sSql = sSql.substring(mc.end()).trim();
            fromSqlConvert(sSql);
        }

    }

    /***
     * 查询的FROM前段SQL处理
     * 注：放这里的原因是INSERT INTO ... SELECT 语句也用到该方法
     * @param sSql
     */
    protected void queryBeforeFromConvert(String sSql) {
        String[] sSelectItemArray = sSql.split(",");
        String sComma="";
        for (String col:sSelectItemArray) {
            //查看是否有：##序号##
            boolean parenthesesRounFlag = false;//没有
            Pattern regex = Pattern.compile(parenthesesRoundKey + "\\d+" + parenthesesRoundKey,CASE_INSENSITIVE);
            Matcher mc = regex.matcher(col);
            if(mc.find()){
                parenthesesRounFlag = true;
            }
            if(!hasKey(col) && !parenthesesRounFlag){
                sbHead.append(sComma + col);
                sComma = ",";
                continue;
            }
            //括号转换处理
            parenthesesKeyConvert(sComma + col,"");
            //第一个有效元素后的元素前要加逗号：查询的字段应该是不能去掉的，回头这再看看？？？
            if(sComma.isEmpty()){
                String sKey = getFirstKeyName(col);
                if(mapSqlKeyValid.containsKey(sKey)){
                    sComma = ",";
                }
            }
        }
    }

    /**
     * 头部SQL转换：子类实现
     * @param sSql
     */
    protected abstract void headSqlConvert(String sSql);

    /**
     * FROM前段的SQL转换：子类实现
     * @param sSql
     */
    protected abstract void beforeFromConvert(String sSql);

}
