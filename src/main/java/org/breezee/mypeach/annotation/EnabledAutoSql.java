package org.breezee.mypeach.annotation;

import org.breezee.mypeach.autoconfigure.MyPeachAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @objectName: 启用自动化构建SQL功能
 * @description:
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/17 8:43
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({MyPeachAutoConfiguration.class})
public @interface EnabledAutoSql {

}
