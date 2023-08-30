package org.breezee.mypeach.utils;

import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.config.StaticConstants;
import org.breezee.mypeach.enums.SqlKeyStyleEnum;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * @objectName: 工具辅助类
 * @description:
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/14 16:23
 */
public class ToolHelper {
    /**
     * 判断对象是否非空
     * @param obj
     * @return
     */
    public static boolean IsNotNull(Object obj){

        return obj != null &&  !String.valueOf(obj).trim().isEmpty();
    }

    /**
     * 判断对象是否为空
     * @param obj
     * @return
     */
    public static boolean IsNull(Object obj){
        return obj == null ||  String.valueOf(obj).trim().isEmpty();
    }
    /**
     * 获取键名（不含前后缀，但含更多信息）
     * @param sKeyString
     * @param prop
     * @return
     */
    public static String getKeyNameMore(String sKeyString, MyPeachProperties prop){
        String keyPrefix = StaticConstants.HASH;
        String keySuffix = StaticConstants.HASH;
//        if(prop.getKeyStyle()== SqlKeyStyleEnum.POUND_SIGN_BRACKETS){
//            keyPrefix = StaticConstants.HASH_LEFT_BRACE;
//            keySuffix = StaticConstants.RIGHT_BRACE;
//        }
        String sKeyNameMore = sKeyString.replace("'","").replace("%","")
                .replace(keyPrefix,"").replace(keySuffix,"").trim();
        return sKeyNameMore;//键中包含其他信息
    }

    /**
     * 获取键名（不含前后缀，且不含更多信息）
     * @param sKeyString：例如：'%#CITY_NAME#%'、'%#CITY_NAME:N#%'
     * @param prop
     * @return 例如：CITY_NAME
     */
    public static String getKeyName(String sKeyString, MyPeachProperties prop){
        String sKeyNameMore = getKeyNameMore(sKeyString,prop);
        if(sKeyNameMore.indexOf(":") <0){
            return sKeyNameMore.trim();//键中没有包含其他信息
        }else {
            return sKeyNameMore.split(":")[0].trim();//键中包含其他信息，但第一个必须是键名
        }
    }

    /**
     * 获取目标参数化的字段名
     * @param sParamName：例如：'%#CITY_NAME#%'
     * @param prop
     * @return 例如：@CITY_NAME
     */
    public static String getTargetParamName(String sParamName, MyPeachProperties prop){
        return prop.getParamPrefix() + getKeyName(sParamName, prop) + prop.getParamSuffix();
    }

    /**
     * 获取匹配对象
     * @param sSql 要匹配的SQL
     * @param sPattern 匹配的正则式
     * @return
     */
    public static Matcher getMatcher(String sSql, String sPattern) {
        Pattern regexInner = Pattern.compile(sPattern, CASE_INSENSITIVE);//先根据WHERE关键字将字符分隔为两部分
        return regexInner.matcher(sSql);
    }

    /**
     * 移除SQL前后括号
     * @param sSql
     * @return
     */
    public static String removeBeginEndParentheses(String sSql){
        sSql = sSql.trim();
        sSql = sSql.startsWith("(")?sSql.substring(1):sSql;
        sSql = sSql.endsWith(")")?sSql.substring(0,sSql.length()-1):sSql;

        return sSql;
    }

    /// <summary>
    /// 获取整型值
    /// </summary>
    /// <param name="sInt">要转换的字符</param>
    /// <param name="iDefault">默认值</param>
    /// <returns>根据传入字符转换，成功则取转换后值，否则取默认值</returns>
    public static int getInt(String sInt,int iDefault) {
        int result;
        try {
            result = Integer.parseInt(sInt);
        }
        catch (Exception e) {
            result = iDefault;
        }
        return result;
    }
}
