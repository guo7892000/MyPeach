package org.breezee.mypeach.core;

import lombok.extern.slf4j.Slf4j;
import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.config.StaticConstants;
import org.breezee.mypeach.enums.SqlTypeEnum;
import org.breezee.mypeach.utils.ToolHelper;

import java.util.regex.Matcher;

/**
 * @objectName:Delete Sql Analyzer(删除SQL分析器)
 * @description:
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/12 16:45
 */
@Slf4j
public class DeleteSqlParser extends AbstractSqlParser {

    /**
     * 构造函数
     * @param properties
     */
    public DeleteSqlParser(MyPeachProperties properties) {
        super(properties);
        sqlTypeEnum = SqlTypeEnum.DELETE;
    }

    /**
     * 头部SQL转换
     * @param sSql
     * @return
     */
    @Override
    public String headSqlConvert(String sSql) {
        StringBuilder sb = new StringBuilder();
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.deletePattern);//抽取出INSERT INTO TABLE_NAME(部分
        while (mc.find()){
            sb.append(mc.group());//不变的INSERT INTO TABLE_NAME(部分先加入
            //FROM部分SQL处理
            String sWhereSql = fromWhereSqlConvert(sSql.substring(mc.end()),false);
            //如果禁用全表更新，并且条件为空，则抛错！
            if(ToolHelper.IsNull(sWhereSql) && myPeachProp.isForbidAllTableUpdateOrDelete()){
                mapError.put("出现全表删除，已停止","删除语句不能没有条件，那样会清除整张表数据！");//错误列表
            }
            sb.append(sWhereSql);
        }
        return sb.toString();
    }

    /**
     * FROM前段SQL处理
     * @param sSql
     * @return
     */
    @Override
    protected String beforeFromConvert(String sSql) {
        return "";
    }

}
