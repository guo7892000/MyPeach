package org.breezee.mypeach.entity;

import java.util.ArrayList;
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
     * 转换前的SQL
     */
    private String sourceSql;

    /**
     * 转换后的SQL
     */
    private String sql;

    /**
     * 参数位置化的查询条件值
     */
    private ArrayList positionCondition;

    /**
     * 错误信息集合
     */
    Map<String, String> mapError = new HashMap<>();
    /**
     * 有效条件集合
     */
    Map<String, SqlKeyValueEntity> entityQuery = new HashMap<>();
    Map<String, String> stringQuery = new HashMap<>();
    Map<String, Object> objectQuery = new HashMap<>();

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSourceSql() {
        return this.sourceSql;
    }

    public void setSourceSql(String sourceSql) {
        this.sourceSql = sourceSql;
    }

    public String getSql() {
        return this.sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public ArrayList getPositionCondition() {
        return this.positionCondition;
    }

    public void setPositionCondition(ArrayList positionCondition) {
        this.positionCondition = positionCondition;
    }

    public Map<String, String> getMapError() {
        return this.mapError;
    }

    public void setMapError(Map<String, String> mapError) {
        this.mapError = mapError;
    }

    public Map<String, SqlKeyValueEntity> getEntityQuery() {
        return this.entityQuery;
    }

    public void setEntityQuery(Map<String, SqlKeyValueEntity> stringQuery) {
        this.entityQuery = stringQuery;
    }

    public Map<String, Object> getObjectQuery() {
        return this.objectQuery;
    }

    public void setObjectQuery(Map<String, Object> mapObject) {
        this.objectQuery = mapObject;
    }

    public Map<String, String> getStringQuery() {
        return this.stringQuery;
    }

    public void setStringQuery(Map<String, String> stringQuery) {
        this.stringQuery = stringQuery;
    }



    public static ParserResult success(String msg,String sSql,Map<String, SqlKeyValueEntity> entityQuery,Map<String, Object> objectQuery,Map<String, String> stringQuery,ArrayList pCondition){
        ParserResult result = new ParserResult();
        result.setCode("0");
        result.setSql(sSql);
        result.setMessage(msg);
        result.setEntityQuery(entityQuery);
        result.setObjectQuery(objectQuery);
        result.setStringQuery(stringQuery);
        result.setPositionCondition(pCondition);
        return result;
    }

    public static ParserResult success(String sSql,Map<String, SqlKeyValueEntity> queryMap,Map<String, Object> mapObject,Map<String, String> mapString,ArrayList pCondition){
        return success("SQL转换成功，有效条件请见mapQuery集合！",sSql,queryMap,mapObject,mapString,pCondition);
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
