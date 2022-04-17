package org.breezee.mypeach.enums;

/**
 * @objectName: SQL中键的样式枚举
 * @description:
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/17 7:47
 */
public enum SqlKeyStyleEnum {
    /**
     * 前后#号方式（默认），例如：#KEY#
     */
    POUND_SIGN_AROUND(1),
    /**
     * 使用#{}方式，即使用MyBatis表示键的方式，例如：#{KEY}
     */
    POUND_SIGN_BRACKETS(2);

    private int code;

    SqlKeyStyleEnum(int i){
        code = i;
    }

    public int getCode(){
        return code;
    }
}
