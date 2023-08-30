package org.breezee.mypeach.core;

import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.config.StaticConstants;
import org.breezee.mypeach.enums.SqlTypeEnum;
import org.breezee.mypeach.utils.ToolHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * @objectName: 更新SQL分析器
 * @description: 针对UPDATE SET的SQL分析，思路：
 * 1.根据正则式：)VALUES(匹配，把数据库列与赋值分开，得到两个字符串。并且把匹配部分加到值字符构建器中
 * 2.
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/12 16:45
 * @history:
 *
 */
public class UpdateSqlParser extends AbstractSqlParser {
    /**
     * 构造函数
     * @param properties
     */
    public UpdateSqlParser(MyPeachProperties properties) {
        super(properties);
        sqlTypeEnum = SqlTypeEnum.UPDATE;
    }

    /**
     * 头部SQL转换
     * @param sSql
     * @return
     */
    @Override
    public String headSqlConvert(String sSql) {
        StringBuilder sb = new StringBuilder();
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.updateSetPattern);//先截取UPDATE SET部分
        if(mc.find()) {
            sqlTypeEnum = SqlTypeEnum.UPDATE;
            sb.append(mc.group());//不变的UPDATE SET部分先加入
            sSql = sSql.substring(mc.end()).trim();
            //调用From方法
            String sFinalSql = fromWhereSqlConvert(sSql,false); //注：更新的条件也不可能会有UNION或UNION ALL
            //如果禁用全表更新，并且条件为空，则抛错！
            if (ToolHelper.IsNull(sFinalSql) && myPeachProp.isForbidAllTableUpdateOrDelete()) {
                mapError.put("出现全表更新，已停止", "更新语句不能没有条件，那样会更新整张表数据！");//错误列表
            }
            sb.append(sFinalSql);
        } else {
            sqlTypeEnum = SqlTypeEnum.Unknown;
        }
        return sb.toString();
    }

    /**
     * FROM前段SQL处理
     * @param sSql
     * @return
     */
    protected String beforeFromConvert(String sSql){
        return dealUpdateSetItem(sSql);
    }

    /**
     * 是否正确SQL类型实现方法
     * @param sSql
     * @return
     */
    @Override
    public  boolean isRightSqlType(String sSql)
    {
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.updateSetPattern);
        return mc.find();
    }
}
