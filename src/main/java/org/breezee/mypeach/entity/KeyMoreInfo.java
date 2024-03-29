package org.breezee.mypeach.entity;

import org.breezee.mypeach.config.SqlKeyConfig;
import org.breezee.mypeach.config.StaticConstants;
import org.breezee.mypeach.utils.ToolHelper;

import java.util.*;

/**
 * @objectName: 键更多信息
 * @description: N或M-非空；R-替换；LI-整型列表；LS-字符列表
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/16 23:53
 * @history:
 *    2023/07/27 BreezeeHui 增加LI和LS中传入的为字符时，先去掉单引号，根据传入值以逗号分隔后，重新做值替换。listConvert中传入值为空时直接返回。
 *    2023/08/04 BreezeeHui 键设置增加优先使用配置项（F）的支持，即当一个键出现多次时，优先使用该配置内容。
 *    2023/08/13 BreezeeHui 键设置增加默认值、不加引号。
 *    2023/08/18 BreezeeHui 字符比较忽略大小写（以equalsIgnoreCase 代替 equals）。子配置支持支持-&@|分隔
 *    2023/08/30 BreezeeHui 针对默认值，如配置为不加引号，那么也把值中的引号去掉。
 */
public class KeyMoreInfo {
    /**
     * 可空（默认是）
     */
    boolean nullable = true;

    /// <summary>
    /// 是否必填
    /// </summary>
    boolean isMust;

    /*
    * 是否优先使用的配置（默认否）
    * */
    boolean isFirst = false;

    /**
     * IN字符串(注：指括号里边部分)
     */
    String inString;
    /// <summary>
    /// In条件中的列名：注列名也可以是有函数转换，虽然那样的SQL不推荐
    /// </summary>
    String InColumnName = "";
    /// <summary>
    /// 默认值：示例：D-默认值-R-N
    /// </summary>
    String DefaultValue = "";

    /// <summary>
    /// 是否默认值不加引号：默认都加上。不要时可设置为ture
    /// </summary>
    boolean IsDefaultValueNoQuotationMark = false;
    /// <summary>
    /// 是否默认值必须值替换：默认为false，即当值来使用，使用参数化。为ture时，是直接使用值，如函数等
    /// </summary>
    boolean IsDefaultValueValueReplace = false;
    /// <summary>
    /// 值不加引号：默认都加上。不要时可设置为ture
    /// </summary>
    boolean IsNoQuotationMark = false;
    //是否必须替换（有些键不做参数化时使用）
    boolean mustValueReplace = false;

    public String getInColumnName() {
        return InColumnName;
    }

    public void setInColumnName(String inColumnName) {
        InColumnName = inColumnName;
    }

    public int getPerInListMax() {
        return PerInListMax;
    }

    public void setPerInListMax(int perInListMax) {
        PerInListMax = perInListMax;
    }

    /// <summary>
    /// 每次In清单项最大值，超过该值后会拆分成多个OR IN ('','')
    /// </summary>
    int PerInListMax = 0;

    public boolean isDefaultValueNoQuotationMark() {
        return IsDefaultValueNoQuotationMark;
    }

    public void setDefaultValueNoQuotationMark(boolean defaultValueNoQuotationMark) {
        IsDefaultValueNoQuotationMark = defaultValueNoQuotationMark;
    }

    public boolean isDefaultValueValueReplace() {
        return IsDefaultValueValueReplace;
    }

    public void setDefaultValueValueReplace(boolean defaultValueValueReplace) {
        IsDefaultValueValueReplace = defaultValueValueReplace;
    }

    public String getDefaultValue() {
        return DefaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        DefaultValue = defaultValue;
    }

    public boolean isNoQuotationMark() {
        return IsNoQuotationMark;
    }

    public void setNoQuotationMark(boolean noQuotationMark) {
        IsNoQuotationMark = noQuotationMark;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public boolean isMust() {
        return !this.nullable; //取反
    }

    public void setMust(boolean must) {
        this.nullable = !must; //取反
    }

    public boolean isFirst() {
        return isFirst;
    }

    public void setFirst(boolean first) {
        isFirst = first;
    }

    public String getInString() {
        return inString;
    }

    public void setInString(String inString) {
        this.inString = inString;
    }

    public boolean isMustValueReplace() {
        return mustValueReplace;
    }

    public void setMustValueReplace(boolean mustValueReplace) {
        this.mustValueReplace = mustValueReplace;
    }

    /**
     * 构建【键更多信息】对象
     * @param sKeyMore 键更多信息字符，例如：CITY_NAME:N:R
     * @return
     */
    public static KeyMoreInfo build(String sKeyMore, Object objValue){
        KeyMoreInfo moreInfo = new KeyMoreInfo();
        //配置大类分隔
        String[] arr = sKeyMore.split(StaticConstants.keyBigTypeSpit);
        for (int i = 0; i < arr.length; i++) {
            if(i==0) continue;
            String sOne = arr[i].trim();
            if(sOne.isEmpty()) continue;
            //配置小类分隔
            String[] sMoreArr = sOne.split(StaticConstants.keySmallTypeSpit);
            sOne = sMoreArr[0].trim();

            if(SqlKeyConfig.V_MUST.equalsIgnoreCase(sOne)){
                moreInfo.setNullable(false);//非空
            } else if(SqlKeyConfig.V_REPLACE.equalsIgnoreCase(sOne)){
                moreInfo.setMustValueReplace(true);//必须替换
            } else if(SqlKeyConfig.CFG_FIRST.equalsIgnoreCase(sOne)){
                moreInfo.setFirst(true);//是否优先使用本配置
            }else if(SqlKeyConfig.STRING_LIST.equalsIgnoreCase(sOne)){
                listConvert(objValue, moreInfo,true);//字符列表
                if (sMoreArr.length > 1) {
                    moreInfo.PerInListMax = ToolHelper.getInt(sMoreArr[1],0);
                }
            } else if(SqlKeyConfig.INTEGE_LIST.equalsIgnoreCase(sOne)){
                listConvert(objValue, moreInfo,false);//整型列表
                if (sMoreArr.length > 1) {
                    moreInfo.PerInListMax = ToolHelper.getInt(sMoreArr[1],0);
                }
            }else if(SqlKeyConfig.V_DEFAULT.equalsIgnoreCase(sOne)){
                for (int j = 1; j < sMoreArr.length; j++) {
                    if (j == 1) {
                        moreInfo.setDefaultValue(sMoreArr[1].trim());//默认值
                    } else {
                        if (SqlKeyConfig.V_REPLACE.equalsIgnoreCase(sMoreArr[j].trim())) {
                            moreInfo.setDefaultValueValueReplace(true); //默认值必须值替换
                        }
                        if (SqlKeyConfig.V_NO_QUOTATION_MARK.equalsIgnoreCase(sMoreArr[j].trim())) {
                            moreInfo.setDefaultValueNoQuotationMark(true);//默认值不加引号
                            moreInfo.setDefaultValue(moreInfo.getDefaultValue().replace("'", "").trim()); //去掉默认值中的引号
                        }
                    }
                }
            }else if(SqlKeyConfig.V_NO_QUOTATION_MARK.equalsIgnoreCase(sOne)){
                listConvert(objValue, moreInfo,false);//不加引号
            }else {
                //throw new Exception("未知的配置！！");
            }
        }
        return moreInfo;
    }

    /**
     * 转换SQL中的IN表单
     * @param objValue
     * @param moreInfo
     * @param stringFlag
     */
    private static void listConvert(Object objValue, KeyMoreInfo moreInfo,boolean stringFlag) {
        if(objValue == null){
            return;
        }
        if(stringFlag){
            //String数组或集合
            if(objValue instanceof String[]){
                moreInfo.setInString("'" + String.join("','",(String[]) objValue) + "'");
                moreInfo.setMustValueReplace(true);
                return;
            }
            if(objValue instanceof Collection){
                Iterator<?> iterator = ((Collection<?>) objValue).iterator();
                String sList = "";
                String sPre = "'";
                while(iterator.hasNext()){
                    Object next = iterator.next();
                    sList += sPre + next.toString().replace("'","");
                    sPre = "','";
                }
                sList += "'";
                moreInfo.setInString(sList);
                moreInfo.setMustValueReplace(true);
                return;
            }
            //其他当作字符来处理
            String[] split = objValue.toString().replace("'", "").split(",");
            moreInfo.setInString("'" + String.join("','",split)+"'");
            moreInfo.setMustValueReplace(true);
            return;
        } else {
            //Integer数组或集合
            if(objValue instanceof Integer[]){
                moreInfo.setInString(String.join(",",(String[]) objValue));
                moreInfo.setMustValueReplace(true);
                return;
            }
            if(objValue instanceof Collection){
                Iterator<?> iterator = ((Collection<?>) objValue).iterator();
                String sList = "";
                String sPre = "";
                while (iterator.hasNext()){
                    Object next = iterator.next();
                    sList += sPre + next.toString().replace("'","");
                    sPre = ",";
                }
                moreInfo.setInString(sList);
                moreInfo.setMustValueReplace(true);
                return;
            }

            //其他当作字符来处理
            String[] split = objValue.toString().replace("'", "").split(",");
            moreInfo.setInString(String.join(",",split));
            moreInfo.setMustValueReplace(true);
            return;
        }

    }
}
