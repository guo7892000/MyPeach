package org.breezee.mypeach.core;

import lombok.extern.slf4j.Slf4j;
import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.config.StaticConstants;
import org.breezee.mypeach.enums.SqlTypeEnum;
import org.breezee.mypeach.utils.ToolHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
@Slf4j
public class InsertSqlParser extends AbstractSqlParser {

    String sValuesPattern = "\\)\\s*VALUES\\s*\\(\\s*"; //正则式：)VALUES(
    String sInsertIntoPattern = "^INSERT\\s+INTO\\s+\\S+\\s*\\(\\s*";//正则式：INSERT INTO TABLE_NAME(
    //数据库列名：[列A]、`列B`
    String ParaEndfix = "\\s*(\\[|`)?\\w+[\\]`]?,?" + StaticConstants.remarkPatter;
    String colParaFirst = "\\("+ ParaEndfix;//首列
    String colParaComma = "," + ParaEndfix;//其他列

    public InsertSqlParser(MyPeachProperties properties) {
        super(properties);
        sqlTypeEnum = SqlTypeEnum.INSERT;
    }

    @Override
    public void headSqlConvert(String sSql){
        //1、抽取出INSERT INTO TABLE_NAME(部分
        Pattern regex = Pattern.compile(sInsertIntoPattern);
        Matcher mc = regex.matcher(sSql);
        while (mc.find()){
            sbHead.append(mc.group());//不变的INSERT INTO TABLE_NAME(部分先加入
            sSql = sSql.substring(mc.end()).trim();
            //log.debug("Remove INSERT INTO TABLE_NAME(:",sSql);
        }

        //2、判断是否insert into ... values形式
        boolean insertValuesFlag = false;
        regex = Pattern.compile(sValuesPattern);//先根据VALUES关键字将字符分隔为两部分
        mc = regex.matcher(sSql);
        String sInsert ="";
        String sPara="";
        while (mc.find()){
            sInsert = sSql.substring(0,mc.start()).trim();
            sPara = sSql.substring(mc.end()).trim();
            sbTail.append(mc.group());//不变的)VALUES(部分先加入
            insertValuesFlag = true;
        }

        if(insertValuesFlag){
            //3、 insert into ... values形式
            String[] colArray = sInsert.split(",");
            String[] paramArray = sPara.split(",");

            int iGood = 0;
            for (int i = 0; i < colArray.length; i++) {
                String sParamSql = singleKeyConvert(paramArray[i]);
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

            if(!sbTail.toString().endsWith(")")){
                sbTail.append(")");
            }
        } else {
            //4、INSERT INTO TABLE_NAME 。。 SELECT形式
            regex = Pattern.compile("\\s*\\)\\s+SELECT\\s+");//抽取出INSERT INTO TABLE_NAME(部分
            mc = regex.matcher(sSql);
            while (mc.find()){
                sInsert = sSql.substring(0,mc.start()) + mc.group();
                sbHead.append(sInsert);//不变的INSERT INTO TABLE_NAME(部分先加入
                sSql = sSql.substring(mc.end()).trim();
                fromSqlConvert(sSql);
            }
        }
    }

    @Override
    protected void beforeFromConvert(String sSql) {
        String[] colArray = sSql.split(",");
        for (int i = 0; i < colArray.length; i++) {
            if(i==0){
                parenthesesKeyConvert(colArray[i]," ");
            } else {
                parenthesesKeyConvert(colArray[i],",");
            }

        }
    }

}
