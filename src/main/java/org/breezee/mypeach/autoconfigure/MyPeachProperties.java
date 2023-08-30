package org.breezee.mypeach.autoconfigure;

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
 * @history:
 *   2023/08/18 BreezeeHui 取消KeyStyle的设置，默认都支持#{参数}和#参数#两种方式，只是在处理过程中，会将#{}转换为#参数#后，再统一处理。
 *   2023/08/27 BreezeeHui 增加IN清单的最大项数配置，默认为1000。
 */
@ConfigurationProperties(prefix = "mypeach")
public class MyPeachProperties {
    /**
     *名称：参数化的前缀（Sql param prefix）
     * 描述：在TargetSqlEnum为param时使用。
     */
    private String paramPrefix = "@";

    /**
     * 名称：参数化的后缀（Sql param suffix）
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
     * TargetSqlEnum.NameParam：命名参数化的SQL，默认
     * TargetSqlEnum.PositionParam：位置参数化
     * TargetSqlEnum.DIRECT_RUN：转换为可以直接运行的SQL，SQL中的键已被替换为具体值。注：此方式可能存在SQL注入风险！！
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

    /// <summary>
    /// IN清单的最大项数配置
    /// </summary>
    public int inMax = 1000;

    public String getParamPrefix() {
        return paramPrefix;
    }

    public void setParamPrefix(String paramPrefix) {
        this.paramPrefix = paramPrefix;
    }

    public String getParamSuffix() {
        return paramSuffix;
    }

    public void setParamSuffix(String paramSuffix) {
        this.paramSuffix = paramSuffix;
    }

    public boolean isForbidAllTableUpdateOrDelete() {
        return forbidAllTableUpdateOrDelete;
    }

    public void setForbidAllTableUpdateOrDelete(boolean forbidAllTableUpdateOrDelete) {
        this.forbidAllTableUpdateOrDelete = forbidAllTableUpdateOrDelete;
    }

    public TargetSqlParamTypeEnum getTargetSqlParamTypeEnum() {
        return targetSqlParamTypeEnum;
    }

    public void setTargetSqlParamTypeEnum(TargetSqlParamTypeEnum targetSqlParamTypeEnum) {
        this.targetSqlParamTypeEnum = targetSqlParamTypeEnum;
    }

    public boolean isShowDebugSql() {
        return showDebugSql;
    }

    public void setShowDebugSql(boolean showDebugSql) {
        this.showDebugSql = showDebugSql;
    }

    public String getLogSqlPath() {
        return logSqlPath;
    }

    public void setLogSqlPath(String logSqlPath) {
        this.logSqlPath = logSqlPath;
    }
}
