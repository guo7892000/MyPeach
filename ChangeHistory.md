# 变更历史
## 版本：1.1.4  发布日期：2023-03-26
* 取消SQL大写的转换，因为那样会导致条件中的字母大写而使条件失效！
## 版本：1.1.3  发布日期：2022-07-24
* 增加区分命名参数（默认）和位置参数的SQL转换配置。
* 给parse方法增加了一个指定TargetSqlParamTypeEnum的重载方法。以下为两种方式的使用示例： 
* String sSql = "SELECT * FROM BAS_PROVINCE T WHERE T.PROVINCE_ID = '#PROVINCE_ID#' AND T.PROVINCE_CODE = '#PROVINCE_CODE#'";
* ParserResult parserResult = sqlParsers.parse(SqlTypeEnum.SELECT, sSql, dicQuery);//默认使用命名参数方式
* List<Map<String, Object>> maps = namedParameterJdbcTemplate.queryForList(parserResult.getSql(),parserResult.getMapObject());
* //指定使用位置参数方式 
* ParserResult parserResultPos = sqlParsers.parse(SqlTypeEnum.SELECT, sSql, dicQuery, TargetSqlParamTypeEnum.PostionParam);
* maps = jdbcTemplate.queryForList(parserResultPos.getSql(), parserResultPos.getPositionCondition().toArray());
## 版本：1.1.2  发布日期：2022-07-09
* 增加调试SQL显示、SQL日志输出路径配置功能。
* 对于没有键配置的SQL，直接返回原SQL。
## 1.1.1稳定版
* 针对JOIN的更多样式支持，以及BUG修正。
## 1.1.0
* 修正JOIN括号复杂查询的转换不正确问题。
* 修正INSERT语句中因为【修改括号匹配正则式】而导致的BUG。
## 1.0.7
* 修正JOIN语句中转换不正确问题。
* 针对已被替换为##序号##的语句，其内还可能有括号的更复杂的查询，对其进一步做##序号##替换并分析。
## 1.0.6
* 修正删除中的子查询缺少选择项问题。
* 增加键中R替换值的支持。
* 增加禁止全表更新或删除配置，默认禁用。
* 修改括号匹配正则式，原方式无法匹配换行后的括号。通过分析发现正则匹配左或右括号，并记录它们的数量，如果它们数量相等，那么最前的一个左括号至当前右括号为一组。
## 1.0.5
* 支持UNION和UNION ALL。
* 虽然GROUP BY、HAVING、ORDER BY等一般不会有键配置，但还是增加兼容
* WHERE段提取方法，减少代码冗余
* SELECT正则式加上DISTINCT和TOP N的截取，精准确定查询项
## 1.0.4
* 修正子查询中有括号复杂查询时解析不正确问题
* 优化代码，部分重复代码抽取为方法调用
* 增加没有键的判断，如没有，提示SQL中没有发现键等信息后退出。
## 1.0.3
* 修正子查询条件键为空时整个去掉问题
* 代码优化
## 1.0.2
* 将全局的字符拼接，修改为每个方法返回处理后的字符给调用者。
* 修正键为#{}的问题
## 1.0.1
* 初始版本上传


