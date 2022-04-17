package org.breezee.mypeach.core;

import lombok.extern.slf4j.Slf4j;
import org.breezee.mypeach.autoconfigure.MyPeachProperties;
import org.breezee.mypeach.enums.SqlTypeEnum;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
@Slf4j
public class UpdateSqlParser extends AbstractSqlParser {
    public UpdateSqlParser(MyPeachProperties properties) {
        super(properties);
        sqlTypeEnum = SqlTypeEnum.UPDATE;
    }

    String sUpdateSetPattern = "^UPDATE\\s*\\S*\\s*SET\\s*";//正则式：UPDATE TABLE_NAME SET
    String sSetEqualPattern = "\\s*,\\s*?(\\[|`)?\\w+(]|`)";//正则式：set段中的赋值部分

    @Override
    public void headSqlConvert(String sSql) {
        Pattern regex = Pattern.compile(sUpdateSetPattern);//先截取UPDATE SET部分
        Matcher mc = regex.matcher(sSql);
        while (mc.find()){
            sbHead.append(mc.group());//不变的UPDATE SET部分先加入
            sSql = sSql.substring(mc.end()).trim();
            fromSqlConvert(sSql);
        }
    }

    protected void beforeFromConvert(String sSql){
        String[] sSetArray = sSql.split(",");
        String sComma="";
        for (String col:sSetArray) {
            if(!hasKey(col)){
                sbHead.append(sComma + col);
                sComma = ",";
                continue;
            }

            parenthesesKeyConvert(sComma + col,"");

            if(sComma.isEmpty()){
                String sKey = getFirstKeyName(col);
                if(mapSqlKeyValid.containsKey(sKey)){
                    sComma = ",";
                }
            }
        }
    }

}
