package org.breezee.mypeach.autoconfigure;

import lombok.Data;
import org.breezee.mypeach.enums.SqlKeyStyleEnum;
import org.breezee.mypeach.enums.TargetSqlParamTypeEnum;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @objectName: 全局配置类
 * @description:
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/12 16:45
 */
@ConfigurationProperties(prefix = "mypeach")
@Data
public class MyPeachProperties {
    /**
     * SQL中键的样式枚举
     */
    SqlKeyStyleEnum keyStyle = SqlKeyStyleEnum.POUND_SIGN_AROUND;
    /**
     *名称：参数化的前缀（Sql param prefix）
     * 描述：在TargetSqlEnum为param时使用。
     */
    private String paramPrefix = "@";

    /**
     * 名称：参数化的前缀（Sql param suffix）
     * 描述：在TargetSqlEnum为param时使用。
     */
    private String paramSuffix = "";

    /**
     * 禁止全表更新或删除：默认是
     */
    private boolean forbidAllTableUpdateOrDelete = true;

    /**
     * 名称：生成的SQL类型
     * 描述：
     * TargetSqlEnum.NameParam：参数化的SQL，默认
     * TargetSqlEnum.directRun：转换为可以直接运行的SQL，SQL中的键已被替换为具体值。注：此方式可能存在SQL注入风险！！
     */
    private TargetSqlParamTypeEnum targetSqlParamTypeEnum = TargetSqlParamTypeEnum.NameParam;

    /**
     * 是否在标准输出中显示调试的SQL：默认否
     */
    private boolean showDebugSql = false;
    /*
     *SQL输出日志路径，默认为空，即不输出。如果我们设置了目录，那么会按天生成类似： sql.20220709.txt文件
     * 注：相对路径时，开头不要加/，要以目录名开头。
     */
    private String logSqlPath = "";
}
