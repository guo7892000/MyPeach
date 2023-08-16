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
        return dealUpdateSetItem(sSql);
    }

    /**
     * FROM前段SQL处理
     * @param sSql
     * @return
     */
    protected String beforeFromConvert(String sSql){
        StringBuilder sb = new StringBuilder();
        String[] sSetArray = sSql.split(",");
        String sComma="";
        for (String col:sSetArray) {
            if(!hasKey(col)){
                sb.append(sComma + col);
                sComma = ",";
                continue;
            }

            sb.append(complexParenthesesKeyConvert(sComma + col,""));

            if(sComma.isEmpty()){
                String sKey = getFirstKeyName(col);
                if(mapSqlKeyValid.containsKey(sKey)){
                    sComma = ",";
                }
            }
        }
        return sb.toString();
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
