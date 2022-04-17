package org.breezee.mypeach.utils;

import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.config.StaticConstants;
import org.breezee.mypeach.enums.SqlKeyStyleEnum;

/**
 * @objectName: 工具辅助类
 * @description:
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/14 16:23
 */
public class ToolHelper {
    public static boolean IsNotNull(Object obj){
        return obj != null &&  !String.valueOf(obj).trim().isEmpty();
    }
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
        String keyPrefix = "#";
        String keySuffix = "#";
        if(prop.getKeyStyle()== SqlKeyStyleEnum.POUND_SIGN_BRACKETS){
            keyPrefix = StaticConstants.HASH_LEFT_BRACE;
            keySuffix = StaticConstants.RIGHT_BRACE;
        }
        String sKeyNameMore = sKeyString.replace("'","").replace("%","")
                .replace(keyPrefix,"").replace(keySuffix,"");
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
            return sKeyNameMore;//键中没有包含其他信息
        }else {
            return sKeyNameMore.split(":")[0];//键中包含其他信息，但第一个必须是键名
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
}
