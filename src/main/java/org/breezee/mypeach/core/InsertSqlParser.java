package org.breezee.mypeach.core;

import lombok.extern.slf4j.Slf4j;
import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.config.StaticConstants;
import org.breezee.mypeach.entity.SqlSegment;
import org.breezee.mypeach.enums.SqlSegmentEnum;
import org.breezee.mypeach.enums.SqlTypeEnum;
import org.breezee.mypeach.utils.ToolHelper;

import java.util.ArrayList;
import java.util.List;
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

    public InsertSqlParser(MyPeachProperties properties) {
        super(properties);
        sqlTypeEnum = SqlTypeEnum.INSERT_VALUES;
    }

    @Override
    public String headSqlConvert(String sSql){
        StringBuilder sbHead = new StringBuilder();
        sSql = insertValueConvert(sSql,sbHead);
        if(ToolHelper.IsNull(sSql)){
            return sbHead.toString();
        }

        //4、INSERT INTO TABLE_NAME 。。 SELECT形式
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.insertSelectPattern);//抽取出INSERT INTO TABLE_NAME(部分
        while (mc.find()){
            sqlTypeEnum = SqlTypeEnum.INSERT_SELECT;
            String sInsert = sSql.substring(0,mc.start()) + mc.group();
            sbHead.append(sInsert);//不变的INSERT INTO TABLE_NAME(部分先加入
            sSql = sSql.substring(mc.end()).trim();
            //FROM段处理
            sbHead.append(fromWhereSqlConvert(sSql));
        }
        return sbHead.toString();
    }

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

    @Override
    protected List<SqlSegment> split(String sSql) {
        List<SqlSegment> list = new ArrayList<>();
        StringBuilder sbHead = new StringBuilder();
        sSql = insertValueConvert(sSql,sbHead);
        if(ToolHelper.IsNull(sSql)){
            SqlSegment segment = new SqlSegment();
            segment.setFinalSql(sbHead.toString());
            segment.setNeedParse(false);
            segment.setSqlSegmentEnum(SqlSegmentEnum.INSER_VALUE);
            list.add(segment);
        } else {
            Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.insertSelectPattern);//抽取出INSERT INTO TABLE_NAME(部分
            int indexSelct = 0;
            if (mc.find()){
                sqlTypeEnum = SqlTypeEnum.INSERT_SELECT;
                String sInsert = sSql.substring(0,mc.start())+ mc.group();
                sSql = sSql.substring(mc.end()).trim();

                SqlSegment segment = new SqlSegment();
                segment.setHeadString(sbHead.toString() + sInsert );
                segment.setSql(sSql);
                segment.setSqlSegmentEnum(SqlSegmentEnum.MORE_SELECT);
                list.add(segment);
            }
        }
        return list;
    }

    private String insertValueConvert(String sSql,StringBuilder sb){
        StringBuilder sbHead = new StringBuilder();
        StringBuilder sbTail = new StringBuilder();
        //1、抽取出INSERT INTO TABLE_NAME(部分
        Matcher mc = ToolHelper.getMatcher(sSql, StaticConstants.insertIntoPattern);
        while (mc.find()){
            sbHead.append(mc.group());//不变的INSERT INTO TABLE_NAME(部分先加入
            sSql = sSql.substring(mc.end()).trim();
        }

        //2、判断是否insert into ... values形式
        mc = ToolHelper.getMatcher(sSql, StaticConstants.valuesPattern);//先根据VALUES关键字将字符分隔为两部分
        String sInsert ="";
        String sPara="";
        if (mc.find()){
            sInsert = sSql.substring(0,mc.start()).trim();
            sPara = sSql.substring(mc.end()).trim();
            sbTail.append(mc.group());//不变的)VALUES(部分先加入

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
            sSql = "";//处理完毕清空SQL
        }
        sb.append(sbHead.toString()+sbTail.toString());
        return sSql;
    }

}
