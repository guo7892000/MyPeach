package org.breezee.mypeach.core;

import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.config.StaticConstants;
import org.breezee.mypeach.enums.SqlTypeEnum;
import org.breezee.mypeach.utils.ToolHelper;

import java.util.regex.Matcher;

/**
 * @objectName: 新增SQL分析器（OK）
 * @description: 针对Insert into的SQL分析，思路：
 * 1.根据正则式：)VALUES(匹配，把数据库列与赋值分开，得到两个字符串。并且把匹配部分加到值字符构建器中
 * 2.
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/12 16:45
 */
public class InsertSqlParser extends AbstractSqlParser {
    /**
     * 构造函数
     * @param properties
     */
    public InsertSqlParser(MyPeachProperties properties) {
        super(properties);
        sqlTypeEnum = SqlTypeEnum.INSERT_VALUES;
    }

    /**
     * 头部SQL转换
     * @param sSql
     * @return
     */
    @Override
    public String headSqlConvert(String sSql){
        StringBuilder sbHead = new StringBuilder();

        sSql = insertValueConvert(sSql,sbHead);
        if(ToolHelper.IsNull(sSql)){
            return sbHead.toString();//当是INSERT INTO...VALUES...方式，则方法会返回空
        }

        //4、INSERT INTO TABLE_NAME 。。 SELECT形式
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.commonSelectPattern);//抽取出INSERT INTO TABLE_NAME(部分
        while (mc.find()){
            sqlTypeEnum = SqlTypeEnum.INSERT_SELECT;
            String sInsert = sSql.substring(0,mc.start()) + mc.group();
            sInsert = complexParenthesesKeyConvert(sInsert,"");
            sbHead.append(sInsert);//不变的INSERT INTO TABLE_NAME(部分先加入
            sSql = sSql.substring(mc.end()).trim();
            //FROM段处理
            String sFinalSql = fromWhereSqlConvert(sSql,false);
            sbHead.append(sFinalSql);
        }
        return sbHead.toString();
    }

    /**
     * FROM前段SQL处理
     * @param sSql
     * @return
     */
    @Override
    protected String beforeFromConvert(String sSql) {
        StringBuilder sbHead = new StringBuilder();
        String[] colArray = sSql.split(",");
        for (int i = 0; i < colArray.length; i++) {
            String sLastAndOr = i==0 ? "":",";
            String colString = complexParenthesesKeyConvert(colArray[i],sLastAndOr);

            if(sqlTypeEnum == SqlTypeEnum.INSERT_SELECT && ToolHelper.IsNull(colString)){
                String sKeyName = getFirstKeyName(colArray[i]);
                mapError.put(sKeyName,"SELECT中的查询项"+sKeyName+"，其值必须转入，不能为空！");
            }
            sbHead.append(colString);
        }
        return sbHead.toString();
    }

    /**
     * INSERT INTO及VALUES处理
     * @param sSql
     * @param sb
     * @return
     */
    private String insertValueConvert(String sSql,StringBuilder sb){
        StringBuilder sbHead = new StringBuilder();
        StringBuilder sbTail = new StringBuilder();
        //1、抽取出INSERT INTO TABLE_NAME部分
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.insertIntoPattern);
        if (mc.find()){
            sbHead.append(mc.group());//加入INSERT INTO TABLE_NAME
            sSql = sSql.substring(mc.end()).trim();
        }

        //2、判断是否insert into ... values形式
        mc = ToolHelper.getMatcher(sSql, StaticConstants.valuesPattern);//先根据VALUES关键字将字符分隔为两部分
        String sInsert ="";
        String sPara="";
        if (mc.find()){
            String sInsertKey = sSql.substring(0,mc.start()).trim();
            String sParaKey = sSql.substring(mc.end()).trim();

            sInsert = ToolHelper.removeBeginEndParentheses(mapsParentheses.get(sInsertKey));
            sPara = ToolHelper.removeBeginEndParentheses(mapsParentheses.get(sParaKey));
            sPara = generateParenthesesKey(sPara);//针对有括号的部分先替换为##序号##

            sbHead.append("(");//加入(
            sbTail.append(mc.group()+ "(");//加入VALUES(

            //3、 insert into ... values形式
            String[] colArray = sInsert.split(",");
            String[] paramArray = sPara.split(",");

            int iGood = 0;
            for (int i = 0; i < colArray.length; i++) {
                String sOneParam = paramArray[i];
                String sParamSql = complexParenthesesKeyConvert(sOneParam,"");
                if(ToolHelper.IsNotNull(sParamSql)){
                    if(iGood==0){
                        sbHead.append( colArray[i]);
                        sbTail.append(sParamSql);
                    }else {
                        sbHead.append("," +  colArray[i]);
                        sbTail.append("," + sParamSql);
                    }
                    iGood++;
                }
            }
            sbHead.append(")");
            sbTail.append(")");
            sSql = "";//处理完毕清空SQL
        }
        sb.append(sbHead.toString()+sbTail.toString());
        return sSql;
    }

}
