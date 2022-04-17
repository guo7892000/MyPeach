package org.breezee.mypeach.entity;

import lombok.Data;
import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.config.StaticConstants;
import org.breezee.mypeach.enums.SqlKeyStyleEnum;
import org.breezee.mypeach.utils.ToolHelper;

import java.util.Map;

/**
 * @objectName: SQL中键值实体类
 * @description: 记录SQL中的键，及其传入的值等信息
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/12 16:45
 */
@Data
public class SqlKeyValueEntity {
    /**
     * 键字符(最原始的格式)：例如 '%#CITY_NAME#%'
     */
    private String keyString;
    /**
     * 是否有单引号 '
     */
    private boolean hasSingleQuotes =false;
    /**
     * 是否前模糊查询
     */
    private boolean hasLikePrefix =false;
    /**
     * 是否后模糊查询
     */
    private boolean hasLikeSuffix =false;
    /**
     * 是否有值传入
     */
    private boolean hasValue =false;
    /**
     * 键名（不包括前后缀和更多信息）：例如 CITY_NAME
     */
    private String keyName;
    /**
     * 键名（不包括前后缀，但有更多信息）：例如 CITY_NAME:N
     */
    private String keyNameMore;
    /**
     * 键名(含前后缀)：例如 #CITY_NAME#
     */
    private String keyNamePreSuffix;
    /**
     * 键值:方法参数传入
     */
    private Object keyValue;

    /**
     * 替换键后的值：例如 '%张%'
     */
    private Object replaceKeyWithValue;
    /**
     * 参数化的字符：例如 @CITY_NAME
     */
    private String paramString;
    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 更多键信息
     */
    private KeyMoreInfo keyMoreInfo;

    /**
     * 创建键值实体类对象
     * @param sKeyString 键字符串，例如：'%#CITY_NAME#%'
     * @param dicQuery
     * @param prop
     * @return
     */
    public static SqlKeyValueEntity build(String sKeyString, Map<String,Object> dicQuery, MyPeachProperties prop){
        SqlKeyValueEntity entity = new SqlKeyValueEntity();
        entity.setKeyString(sKeyString);
        if(sKeyString.contains("'")){
            entity.setHasSingleQuotes(true);
            sKeyString = sKeyString.replace("'","");
        }
        if(sKeyString.startsWith("%")){
            entity.setHasLikePrefix(true);
        }
        if(sKeyString.endsWith("%")) {
            entity.setHasLikeSuffix(true);
        }

        String sParamNameMore = ToolHelper.getKeyNameMore(sKeyString, prop);
        entity.setKeyNameMore(sParamNameMore);//设置更多信息字符

        String sParamName = ToolHelper.getKeyName(sKeyString, prop);
        entity.setKeyName(sParamName);
        entity.setParamString(prop.getParamPrefix()+sParamName+ prop.getParamSuffix());

        String sParamNamePreSuffix;
        if(prop.getKeyStyle()== SqlKeyStyleEnum.POUND_SIGN_BRACKETS){
            sParamNamePreSuffix = StaticConstants.HASH_LEFT_BRACE + sParamName + StaticConstants.RIGHT_BRACE;
        }else {
            sParamNamePreSuffix = StaticConstants.HASH + sParamName + StaticConstants.HASH;
        }

        Object inValue = null;
        if(dicQuery.containsKey(sParamName) && ToolHelper.IsNotNull(dicQuery.get(sParamName))){
            inValue = dicQuery.get(sParamName);

        }
        if(dicQuery.containsKey(sParamNamePreSuffix) && ToolHelper.IsNotNull(dicQuery.get(sParamNamePreSuffix))){
            inValue = dicQuery.get(sParamNamePreSuffix);
        }

        entity.setKeyMoreInfo(KeyMoreInfo.build(sParamNameMore,inValue));//设置更多信息对象

        if(inValue!=null){
            entity.setKeyValue(inValue);
            entity.setReplaceKeyWithValue(inValue);
            entity.setHasValue(true);
            if(entity.hasLikePrefix){
                entity.setReplaceKeyWithValue("%" + entity.getReplaceKeyWithValue());
            }
            if(entity.hasLikeSuffix){
                entity.setReplaceKeyWithValue(entity.getReplaceKeyWithValue() + "%");
            }
            if(entity.hasSingleQuotes){
                entity.setReplaceKeyWithValue("'" + entity.getReplaceKeyWithValue() + "'");
            }
        }else {
            if(!entity.keyMoreInfo.nullable){
                entity.setErrorMessage("键("+entity.getKeyName() + ")的值没有传入。");
            }
        }

        return entity;
    }
}
