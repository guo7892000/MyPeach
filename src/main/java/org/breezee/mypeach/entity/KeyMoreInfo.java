package org.breezee.mypeach.entity;

import lombok.Data;

import java.util.*;

/**
 * @objectName: 键更多信息
 * @description: 目前暂时只有一个N:非空
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/16 23:53
 */
@Data
public class KeyMoreInfo {
    /**
     * 可空（默认是）
     */
    boolean nullable = true;
    /**
     * IN字符串(注：指括号里边部分)
     */
    String inString;
    /**
     * 是否必须替换（有些键不做参数化时使用）
     */
    boolean mustValueReplace = false;

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

            if("N".equals(sOne)){
                moreInfo.setNullable(false);//非空
            } else if("R".equals(sOne)){
                moreInfo.setMustValueReplace(true);//必须替换
            } else if("LS".equals(sOne)){
                listConvert(objValue, moreInfo,true);
            } else if("LI".equals(sOne)){
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
        if(stringFlag){
            //String数组或集合
            if(objValue instanceof String[]){
                moreInfo.setInString("'" + String.join("','",(String[]) objValue) + "'");
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
            }
        } else {
            //Integer数组或集合
            if(objValue instanceof Integer[]){
                moreInfo.setInString(String.join(",",(String[]) objValue));
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
            }
        }

    }
}
