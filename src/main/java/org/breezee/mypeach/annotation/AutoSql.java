package org.breezee.mypeach.annotation;

import org.breezee.mypeach.enums.SqlTypeEnum;

import java.lang.annotation.*;

/**
 * @objectName: 自动化SQL
 * @description: 暂时未考虑如何使用，预留
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/12 16:45
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AutoSql {
    String value();
    SqlTypeEnum sqlType = SqlTypeEnum.SELECT;
}
