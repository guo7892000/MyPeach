package org.breezee.mypeach.core;

import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.config.StaticConstants;
import org.breezee.mypeach.entity.ParserResult;
import org.breezee.mypeach.entity.SqlKeyValueEntity;
import org.breezee.mypeach.enums.SqlKeyStyleEnum;
import org.breezee.mypeach.enums.SqlTypeEnum;
import org.breezee.mypeach.enums.TargetSqlParamTypeEnum;
import org.breezee.mypeach.utils.ToolHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private String parenthesesRoundKeyPattern = parenthesesRoundKey + "\\d+" + parenthesesRoundKey;

    protected Map<String, SqlKeyValueEntity> mapSqlKey;//SQL中所有键
    protected Map<String, SqlKeyValueEntity> mapSqlKeyValid;//SQL中有传值的所有键

    Map<String,String> mapsParentheses;//优先处理的括号集合
    public Map<String, String> mapError;//错误信息Map

    protected SqlTypeEnum sqlTypeEnum;

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
        mapError = new ConcurrentHashMap<>();//并发容器-错误信息
    }

    /**
     * 转换SQL（主入口方法）
     * @param sSql
     * @param dic
     * @return
     */
    public ParserResult parse(String sSql, Map<String, Object> dic){

        sSql = sSql.trim().toUpperCase();//将SQL转换为大写

        //1、删除所有注释，降低分析难度，提高准确性
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.remarkPatter);
        Pattern regex;
        while (mc.find()) {
            sSql = sSql.replace(mc.group(),"");//删除所有注释
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

        if(mapSqlKey.size()==0){
            return ParserResult.fail("SQL中没有发现键，当前键配置样式为："+keyPrefix +"key"+ keySuffix+"，请修改配置或SQL。已退出！",mapError);
        }

        if(mapError.size()>0){
            return ParserResult.fail("部分非空键（"+String.join(",",mapError.keySet())+"）没有传入值，已退出！",mapError);
        }

        //3、得到符合左右括号正则式的内容，并替换为类似：##序号##格式，方便先从大方面分析结构，之后再取出括号里的内容来进一步分析
        mc = ToolHelper.getMatcher(sSql, sParenthesesPattern);
        int iStart = 0;
        while (mc.find()) {
            String sKey = parenthesesRoundKey + String.valueOf(iStart) + parenthesesRoundKey;
            mapsParentheses.put(sKey,mc.group());
            sSql = sSql.replace(mc.group(),sKey);//符合左右括号正则式的内容，替换为：##序号##
            iStart++;
        }

        //4、头部处理：交给字类来做
        String sFinalSql = headSqlConvert(sSql);
        //在处理过程中，也会往mapError写入错误信息，所以这里如有错误，也返回出错信息
        if(mapError.size()>0){
            return ParserResult.fail("部分非空键（"+String.join(",",mapError.keySet())+"）没有传入值，已退出！",mapError);
        }
        ParserResult result;
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
    protected String fromSqlConvert(String sSql) {
        StringBuilder sb = new StringBuilder();
        String sSet = "";
        String sFromWhere = "";

        //分隔FROM段
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.fromPattern);
        boolean isDealWhere = false;//是否处理过WHERE语句
        while (mc.find()) {
            //一、FROM及之后WHERE端的处理
            sSet = sSql.substring(0, mc.start()).trim();
            sFromWhere = sSql.substring(mc.end()).trim();

            //1、查询语句中查询的字段，或更新语句中的更新项
            sb.append(beforeFromConvert(sSet));//由子类来处理
            sb.append(mc.group());//sbHead添加FROM字符

            //2、WHERE段分隔
            Matcher mcWhere = ToolHelper.getMatcher(sFromWhere, StaticConstants.wherePattern);
            while (mcWhere.find()) {
                //3、FROM段的处理
                String sFrom = sFromWhere.substring(0,mcWhere.start());
                if(!hasKey(sFrom)){
                    //FROM段没有参数时，直接拼接
                    sb.append(sFrom);
                    sb.append(mcWhere.group());
                    //WHERE条件的处理
                    String sCondition = sFromWhere.substring(mcWhere.end());
                    //AND和OR的条件转换
                    sb.append(andOrConditionConvert(sCondition));
                    break;//中断本次处理
                }

                //4 通过各种Join正则式分解语句
                Matcher mc2 = ToolHelper.getMatcher(sFrom, "\\s*((LEFT)|(RIGHT)|(FULL)|(INNER))?\\s+JOIN\\s*");
                int iStart2=0;
                String lastJoin = "";//最后一次JOIN语句的字符，这个在while循环外处理最后一段字符时用到
                while (mc2.find()) {
                    String oneJoin = sFrom.substring(iStart2,mc2.start());//第一条JOIN语句
                    lastJoin = mc2.group();
                    if(!hasKey(oneJoin)){
                        //没有参数，直接拼接
                        sb.append(oneJoin);
                        //sbHead.append(mc2.group());
                        iStart2 = mc2.end();
                        continue;//继续下一段处理
                    }
                    //AND和OR的条件转换
                    sb.append(andOrConditionConvert(oneJoin));
                    iStart2 = mc2.end();
                }
                sb.append(lastJoin);
                //5 之前正则式中最后一段SQL的AND和OR的条件转换
                sb.append(andOrConditionConvert(sFrom.substring(iStart2)));

                //6.WHERE段的SQL处理
                String sWhereString = mcWhere.group();
                sb.append(sWhereString);//添加上WHERE
                int iLength = sb.length();
                //6.1 AND和OR的条件转换
                sb.append(andOrConditionConvert(sFromWhere.substring(mcWhere.end())));
                //6.2、如果所有条件都为空，即sbHead的长度没变
                if(iLength == sb.length()){
                    sb.delete(iLength-sWhereString.length(),iLength);//移除多余的WHER字符，因为WHERE后面没有条件，不过一般这种情况很少见
                }

            }
            isDealWhere = true;
        }

        if(!isDealWhere){
            //二、 如果语句中没有FROM语句，那会直接进入
            Matcher mcWhere = ToolHelper.getMatcher(sSql, StaticConstants.wherePattern);
            while (mcWhere.find()) {
                String sWhereString = mcWhere.group();
                sb.append(sWhereString);
                int iLength = sb.length();
                //2.1 AND和OR的条件转换
                sb.append(andOrConditionConvert(sSql.substring(mcWhere.end())));
                //2.2、如果所有条件都为空，即sbHead的长度没变
                if(iLength == sb.length()){
                    sb.delete(iLength-sWhereString.length(),iLength);//移除多余的WHER字符，因为WHERE后面没有条件，不过一般这种情况很少见
                }
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
        while (mc.find()) {
            //2、得到一个AND或OR段
            String oneSql = sCond.substring(iStart,mc.start());
            //查看是否有：##序号##
            boolean parenthesesRounFlag = false;//没有
            Matcher mc2 = ToolHelper.getMatcher(oneSql, parenthesesRoundKeyPattern);
            if(mc2.find()){
                parenthesesRounFlag = true;
            }
            if(hasKey(oneSql) || parenthesesRounFlag) {
                //2.1、当键存在，或存在：##序号##时，调用括号键转换处理方法
                sb.append(complexParenthesesKeyConvert(oneSql, sBeforeAndOr));
            }else {
                //2.2、当键存在时，调用括号键转换处理方法
                sb.append(sBeforeAndOr + oneSql);
            }
            sBeforeAndOr = mc.group();
            iStart = mc.end();
        }
        //最后一个AND或OR之后的的SQL字符串处理，也是调用括号键转换处理方法
        sb.append(complexParenthesesKeyConvert(sCond.substring(iStart),sBeforeAndOr));

        return sb.toString();
    }

    /**
     * 复杂的括号键转换处理：
     *  之前为了降低复杂度，将包含()的子查询或函数替换为##序号##，这里需要取出来分析
     * @param sSql 包含##序号##的SQL
     * @param sLastAndOr 上次处理中最后的那个AND或OR字符
     */
    protected String complexParenthesesKeyConvert(String sSql, String sLastAndOr){
        StringBuilder sb = new StringBuilder();
        String sValue = "";
        //1、分析是否有包含 ##序号## 正则式的字符
        Matcher mc = ToolHelper.getMatcher(sSql, parenthesesRoundKeyPattern);
        if(!mc.find()){
            //没有双括号，但可能存在单括号，如是要修改为1=1或AND 1=1 的形式
            return parenthesesConvert(sSql, sLastAndOr);
        }

        //2、有 ##序号## 字符的语句分析
        String sSource = mapsParentheses.get(mc.group());//取出 ##序号## 内容
        if(!hasKey(sSource)){
            //2.1 没有键，得到替换并合并之前的AND或OR字符
            String sConnect = sLastAndOr + sSql.replace(mc.group(),sSource);
            if(!hasKey(sConnect)){
                //2.2 合并后也没有键，则直接追加到头部字符构建器
                return sConnect;
            }
            //2.3 如果有键传入，那么进行单个键转换
            return singleKeyConvert(sConnect);
        }

        //判断是否所有键为空
        boolean allKeyNull = true;
        Matcher mc1 = ToolHelper.getMatcher(sSource, keyPattern);
        while (mc1.find()){
            if(ToolHelper.IsNotNull(singleKeyConvert(mc1.group()))){
                allKeyNull =false;
            }
        }

        String sPre = sSql.substring(0, mc.start());
        String sEnd = sSql.substring(mc.end());
        //3、子查询处理
        String sChildQuery = childQueryConvert(sLastAndOr + sPre, sEnd, sSource);
        sb.append(sChildQuery);//加上子查询
        if(allKeyNull || ToolHelper.IsNotNull(sChildQuery)){
            return sb.toString();//如果全部参数为空，或者子查询已处理，直接返回
        }
        //4、有键值传入，并且非子查询，做AND或OR正则匹配分拆字符
        sb.append(sLastAndOr + sSql.replace(mc.group(),""));//因为不能移除"()"，所以这里先拼接收"AND"或"OR"
        //AND或OR正则匹配处理
        // 注：此处虽然与【andOrConditionConvert】有点类似，但有不同，不能将以下代码替换为andOrConditionConvert方法调用
        Matcher mc2 = ToolHelper.getMatcher(sSource, StaticConstants.andOrPatter);
        int iStart = 0;
        String beforeAndOr = "";
        while (mc2.find()){
            //4.1 存在AND或OR
            String sOne = sSource.substring(iStart,mc2.start()).trim();
            //【括号SQL段转换方法】
            sValue = parenthesesConvert(sOne,beforeAndOr);
            sb.append(sValue);
            iStart = mc2.end();
            beforeAndOr = mc2.group();
        }
        //4.2 最后一个AND或OR之后的的SQL字符串处理，也是调用【括号SQL段转换方法】
        sValue = parenthesesConvert(sSource.substring(iStart),beforeAndOr);
        sb.append(sValue);

        return sb.toString();
    }

    /**
     * 括号的SQL段转换(注：已经过AND或OR拆分，只含一个键)
     *  例如( ( CREATOR = '#CREATOR#' OR CREATOR_ID = #CREATOR_ID# ) AND TFLG = '#TFLG#')
     * @param sSql 只有一个key的字符（即已经过AND或OR的正则表达式匹配后分拆出来的部分字符）
     * @param sLastAndOr 前一个拼接的AND或OR字符
     */
    private String parenthesesConvert(String sSql, String sLastAndOr) {
        //1、剔除开头的一个或多个左括号，并且把这些左括号记录到变量中，方便后面拼接
        String sOne = sSql;
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
        Matcher mcChild = ToolHelper.getMatcher(sSource, "\\(SELECT\\s+");
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

        /** 4、子查询又相当于一个SELECT语句，这里又存在FROM和WHERE处理，所以这部分是根据SELECT模式，再解析一次。
        *   这就是为何将queryHeadSqlConvert和queryBeforeFromConvert放在本抽象父类的缘故。
        */
        mcChild = ToolHelper.getMatcher(sSource, StaticConstants.selectPattern);//抽取出SELECT部分
        while (mcChild.find()) {
            //4.1 调用查询头部转换方法
            sb.append(queryHeadSqlConvert(sSource));
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
        Matcher mc = ToolHelper.getMatcher(sSql, keyPattern);
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
        Matcher mc = ToolHelper.getMatcher(sSql, keyPattern);
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
        Matcher mc = ToolHelper.getMatcher(sSql, keyPattern);
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
    protected String queryHeadSqlConvert(String sSql) {
        StringBuilder sb = new StringBuilder();
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.selectPattern);//抽取出SELECT部分
        while (mc.find()){
            sb.append(mc.group());//不变的SELECT部分先加入
            sSql = sSql.substring(mc.end()).trim();
            sb.append(fromSqlConvert(sSql));
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
            String colString = complexParenthesesKeyConvert(sComma + col,"");
            sb.append(colString);
            //第一个有效元素后的元素前要加逗号：查询的字段应该是不能去掉的，回头这再看看？？？
            if(sComma.isEmpty()){
                String sKey = getFirstKeyName(col);
                if(mapSqlKeyValid.containsKey(sKey)){
                    sComma = ",";
                }
            }
        }
        return sb.toString();
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

}
