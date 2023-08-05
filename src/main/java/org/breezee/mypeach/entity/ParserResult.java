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
    Map<String, SqlKeyValueEntity> mapQuery = new HashMap<>();
    Map<String, Object> mapObject = new HashMap<>();

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSourceSql() {
        return sourceSql;
    }

    public void setSourceSql(String sourceSql) {
        this.sourceSql = sourceSql;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public ArrayList getPositionCondition() {
        return positionCondition;
    }

    public void setPositionCondition(ArrayList positionCondition) {
        this.positionCondition = positionCondition;
    }

    public Map<String, String> getMapError() {
        return mapError;
    }

    public void setMapError(Map<String, String> mapError) {
        this.mapError = mapError;
    }

    public Map<String, SqlKeyValueEntity> getMapQuery() {
        return mapQuery;
    }

    public void setMapQuery(Map<String, SqlKeyValueEntity> mapQuery) {
        this.mapQuery = mapQuery;
    }

    public Map<String, Object> getMapObject() {
        return mapObject;
    }

    public void setMapObject(Map<String, Object> mapObject) {
        this.mapObject = mapObject;
    }

    public Map<String, String> getMapString() {
        return mapString;
    }

    public void setMapString(Map<String, String> mapString) {
        this.mapString = mapString;
    }

    Map<String, String> mapString = new HashMap<>();

    public static ParserResult success(String msg,String sSql,Map<String, SqlKeyValueEntity> queryMap,Map<String, Object> mapObject,Map<String, String> mapString,ArrayList pCondition){
        ParserResult result = new ParserResult();
        result.setCode("0");
        result.setSql(sSql);
        result.setMessage(msg);
        result.setMapQuery(queryMap);
        result.setMapObject(mapObject);
        result.setMapString(mapString);
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
