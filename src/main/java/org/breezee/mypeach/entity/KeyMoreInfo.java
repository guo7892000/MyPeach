package org.breezee.mypeach.entity;

import org.breezee.mypeach.config.SqlKeyConfig;

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
    /**
     * 是否必须替换（有些键不做参数化时使用）
     */
    boolean mustValueReplace = false;

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
        String[] arr = sKeyMore.split(":");
        for (int i = 0; i < arr.length; i++) {
            if(i==0) continue;
            String sOne = arr[i];
            if(sOne.isEmpty()) continue;

            if(SqlKeyConfig.NOT_NULL.equals(sOne) || SqlKeyConfig.IS_MUST.equals(sOne)){
                moreInfo.setNullable(false);//非空
            } else if(SqlKeyConfig.VALUE_REPLACE.equals(sOne)){
                moreInfo.setMustValueReplace(true);//必须替换
            } else if(SqlKeyConfig.IS_FIRST.equals(sOne)){
                moreInfo.setFirst(true);//是否优先使用本配置
            }else if(SqlKeyConfig.STRING_LIST.equals(sOne)){
                listConvert(objValue, moreInfo,true);
            } else if(SqlKeyConfig.INTEGE_LIST.equals(sOne)){
                listConvert(objValue, moreInfo,false);
            }

            String[] arrChild = sOne.split("-");
            for (int j = 0; j < arrChild.length; j++) {
                String sOneItem = arrChild[j];
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
