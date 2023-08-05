#Change History

##Version: 1.1.8 Release Date: August 6, 2023
*Remove the dependency of Lambook.
*Add # annotation support; Correct the matching and removal of/* * /comments. Improve the annotation description.
*Increase support for prioritizing the use of configuration items (F).
*Fix the bug that occurs when there are multiple serial numbers. Do not use groupCount() to determine a match (there is a match but its value is 0, I don't know why), only find() is used to determine whether there is a match.
*Update readme instructions again and add English version instructions!

##Version: 1.1.7 Release Date: July 29, 2023
*Add support for With Insert INTO SELECT and Insert INTO With SELECT!

##Version: 1.1.6 Release Date: July 17, 2023
*SqlParsers adds a conversion overload method for automatically determining SQL types!

##Version: 1.1.4 Release Date: March 26, 2023
*Cancel the conversion of SQL uppercase, as it will cause the letters in the condition to be capitalized and invalidate the condition!

##Version: 1.1.3 Release Date: July 24, 2022
*Add SQL conversion configuration that distinguishes between named parameters (default) and positional parameters.
*Added an overloaded method to the parse method that specifies TargetSqlParamTypeEnum. The following are two examples of usage:
*String sql="SELECT * From BAS_PROVINCE T WHERE T. PROVINCE_ID='# PROVINCE_ID #' AND T. PROVINCE_CODE='# PROVINCE_CODE #'";
*ParserResult parserResult=sqlParsers. parse (SqlTypeEnum. SELECT, sql, dicQuery)// Default use of named parameter method
*List<Map<String, Object>>maps=namedParameterJdbcTemplate. queryForList (parserResult. getSql(), parserResult. getMapObject ());
*//Specify how to use positional parameters
*ParserResult parserResultPos=sqlParsers. parse (SqlTypeEnum. SELECT, sql, dicQuery, TargetSqlParamTypeEnum. PositionParam);
*Maps=jdbcTemplate. queryForList (parserResultPos. getSql(), parserResultPos. getPositionCondition(). toArray());

##Version: 1.1.2 Release Date: July 9th, 2022
*Add debugging SQL display and SQL log output path configuration functions.
*For SQL without key configuration, return the original SQL directly.

##1.1.1 Stable version
*More style support for JOIN and bug fixes.

##1.1.0
*Fixed incorrect conversion of JOIN parentheses complex queries.
*Fix the bug in the Insert statement caused by 'modifying bracket matching regex'.

##1.0.7
*Fix the issue of incorrect conversion in JOIN statements.
*For statements that have been replaced with # # ordinal # # and may have more complex queries with parentheses inside, further replace and analyze them with # # ordinal # #.

##1.0.6
*Fixed the issue of missing selection items for sub queries in deletion.
*Add support for replacing R values in keys.
*Add a configuration that prohibits full table updates or deletions, which is disabled by default.
*Modify the parentheses to match the regular expression, the original method cannot match the parentheses after line breaks. Through analysis, it is found that the regular matches left or right parentheses, and their quantity is recorded. If their quantity is equal, then the first left parenthesis to the current right parenthesis is a group.

##1.0.5
*Supports UNION and UNION ALL.
*Although there are generally no key configurations for GROUP BY, HAVING, ORDER BY, etc., compatibility is still increased
*WHERE segment extraction method to reduce Redundant code
*SELECT regularization combined with DISTINCT and TOP N truncation to accurately determine query terms

##1.0.4
*Correction of incorrect parsing of complex queries with parentheses in subqueries
*Optimize code, extract partial duplicate code into method calls
*Add the judgment of no key, if not, prompt that no key or other information was found in SQL before exiting.

##1.0.3
*Fix the issue of removing the entire subquery condition key when it is empty
*Code optimization

##1.0.2
*Splice the global characters and modify them to return the processed characters to the caller for each method.
*Fix the issue with key # {}

##1.0.1
*Initial version upload


