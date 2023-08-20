package org.breezee.mypeach.entity;

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
 * @history:
 *   2023/07/21 BreezeeHui 针对Like的前后模糊查询，其键值也相应增加%，以支持模糊查询
 *   2023/08/18 BreezeeHui 参数前后缀只取#参数#；当条件不传值时，取默认值，根据默认值是否必须值替换来决定值必须值替换。
 */
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

    public String getKeyString() {
        return keyString;
    }

    public void setKeyString(String keyString) {
        this.keyString = keyString;
    }

    public boolean isHasSingleQuotes() {
        return hasSingleQuotes;
    }

    public void setHasSingleQuotes(boolean hasSingleQuotes) {
        this.hasSingleQuotes = hasSingleQuotes;
    }

    public boolean isHasLikePrefix() {
        return hasLikePrefix;
    }

    public void setHasLikePrefix(boolean hasLikePrefix) {
        this.hasLikePrefix = hasLikePrefix;
    }

    public boolean isHasLikeSuffix() {
        return hasLikeSuffix;
    }

    public void setHasLikeSuffix(boolean hasLikeSuffix) {
        this.hasLikeSuffix = hasLikeSuffix;
    }

    public boolean isHasValue() {
        return hasValue;
    }

    public void setHasValue(boolean hasValue) {
        this.hasValue = hasValue;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getKeyNameMore() {
        return keyNameMore;
    }

    public void setKeyNameMore(String keyNameMore) {
        this.keyNameMore = keyNameMore;
    }

    public String getKeyNamePreSuffix() {
        return keyNamePreSuffix;
    }

    public void setKeyNamePreSuffix(String keyNamePreSuffix) {
        this.keyNamePreSuffix = keyNamePreSuffix;
    }

    public Object getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(Object keyValue) {
        this.keyValue = keyValue;
    }

    public Object getReplaceKeyWithValue() {
        return replaceKeyWithValue;
    }

    public void setReplaceKeyWithValue(Object replaceKeyWithValue) {
        this.replaceKeyWithValue = replaceKeyWithValue;
    }

    public String getParamString() {
        return paramString;
    }

    public void setParamString(String paramString) {
        this.paramString = paramString;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public KeyMoreInfo getKeyMoreInfo() {
        return keyMoreInfo;
    }

    public void setKeyMoreInfo(KeyMoreInfo keyMoreInfo) {
        this.keyMoreInfo = keyMoreInfo;
    }

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
    public static SqlKeyValueEntity build(String sKeyString, Map<String,Object> dicQuery, MyPeachProperties prop, boolean isPreGetCondition){
        sKeyString = sKeyString.trim();
        SqlKeyValueEntity entity = new SqlKeyValueEntity();
        entity.setKeyString(sKeyString);
        if(sKeyString.contains("'")){
            entity.setHasSingleQuotes(true);
            sKeyString = sKeyString.replace("'","").trim();
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

        String sParamNamePreSuffix= StaticConstants.HASH + sParamName + StaticConstants.HASH;

        //取出传入条件中的值：对于字符串类型，会替换字符中的单引号
        Object inValue = null;
        if(dicQuery.containsKey(sParamName) && ToolHelper.IsNotNull(dicQuery.get(sParamName))){
            inValue = dicQuery.get(sParamName);
            if(inValue instanceof String){
                inValue = String.valueOf(inValue).replace("'","");
            }
        }
        if(dicQuery.containsKey(sParamNamePreSuffix) && ToolHelper.IsNotNull(dicQuery.get(sParamNamePreSuffix))){
            inValue = dicQuery.get(sParamNamePreSuffix);
            if(inValue instanceof String){
                inValue = String.valueOf(inValue).replace("'","");
            }
        }

        entity.setKeyMoreInfo(KeyMoreInfo.build(sParamNameMore,inValue));//设置更多信息对象
        if (entity.getKeyMoreInfo().IsNoQuotationMark) {
            entity.setHasSingleQuotes(false); //重新根据配置来去掉引号
        }
        //使用默认值条件：条件传入值为空，非预获取参数，默认值不为空
        if (inValue == null && !isPreGetCondition && entity.getKeyMoreInfo().DefaultValue!=null && !entity.getKeyMoreInfo().DefaultValue.isEmpty()) {
            if (entity.getKeyMoreInfo().IsDefaultValueNoQuotationMark) {
                entity.setHasSingleQuotes(false);
            }
            if (entity.getKeyMoreInfo().IsDefaultValueValueReplace) {
                entity.getKeyMoreInfo().setMustValueReplace(true); //当没有传入值，且默认值为值替换时。当作是有传入默认值，且是值替换
                inValue= entity.getKeyMoreInfo().DefaultValue.replace("'","").trim();//取默认值。为防止SQL注入，去掉单引号
            }else{
                inValue= entity.getKeyMoreInfo().DefaultValue.trim(); //将作参数化，不需要替换掉引号
            }
        }

        if(inValue==null || inValue.toString().isEmpty()){
            if(!entity.keyMoreInfo.nullable){
                entity.setErrorMessage("键("+entity.getKeyName() + ")的值没有传入。");
            }
        }else {
            entity.setKeyValue(inValue);
            entity.setReplaceKeyWithValue(inValue);
            entity.setHasValue(true);
            if(entity.hasLikePrefix){
                entity.setReplaceKeyWithValue("%" + entity.getReplaceKeyWithValue());
                entity.setKeyValue("%" +entity.getKeyValue());
            }
            if(entity.hasLikeSuffix){
                entity.setReplaceKeyWithValue(entity.getReplaceKeyWithValue() + "%");
                entity.setKeyValue(entity.getKeyValue()+"%");
            }
            if(entity.hasSingleQuotes){
                entity.setReplaceKeyWithValue("'" + entity.getReplaceKeyWithValue() + "'");
            }
        }

        return entity;
    }
}
