package org.breezee.mypeach.entity;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @objectName: 分析结果
 * @description: 作为SQL转换后返回的结果
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/17 0:21
 */
@Data
public class ParserResult {
    /**
     * 状态码:0成功，1失败
     */
    private String code;

    /**
     * 成功或错误信息
     */
    private String message;

    /**
     * 转换后的SQL
     */
    private String sql;

    /**
     * 错误信息集合
     */
    Map<String, String> mapError = new HashMap<>();
    /**
     * 有效条件集合
     */
    Map<String, SqlKeyValueEntity> mapQuery = new HashMap<>();

    public static ParserResult success(String msg,String sSql,Map<String, SqlKeyValueEntity> queryMap){
        ParserResult result = new ParserResult();
        result.setCode("0");
        result.setMessage(msg);
        result.setMapQuery(queryMap);
        return result;
    }

    public static ParserResult success(String sSql,Map<String, SqlKeyValueEntity> queryMap){
        return success("SQL转换成功，有效条件请见mapQuery集合！",sSql,queryMap);
    }

    public static ParserResult fail(String msg,Map<String, String> errMap){
        ParserResult result = new ParserResult();
        result.setCode("1");
        result.setMessage(msg);
        result.setMapError(errMap);
        return result;
    }

    public static ParserResult fail(Map<String, String> errMap){
        return fail("SQL转换失败，详细请见mapError集合！",errMap);
    }
}
