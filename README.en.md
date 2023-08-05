MyPeach Dynamic SQL Parser Tool for Java
=================

#### Description
MyPeach is a dynamic SQ conversion tool that can generate dynamic SQL based on the configured keys (default format: # Keyname #) and key value sets (Map<String, Object>) in SQL.
If a value is passed in for a certain key, the condition will be retained and parameterized or replaced with characters (configurable and selectable); Otherwise, discard or modify the condition to AND 1=1 (when analyzing multiple conditions enclosed in parentheses).
The dynamic parts include: all types of conditions, Insert items, and UPDATE items.

#### Software Architecture
*Based on Spring Boot, very lightweight
*Database independence
*Supported statement styles:
```
    INSER INTO ...VALUES... 
    INSERT INTO...SELECT...FROM...WHERE... 
    INSERT INTO...WITH...SELECT...FROM...WHERE... 
    WITH...INSERT INTO... SELECT...FROM...WHERE... 
    UPDATE ... SET...FROM...WHERE...  
    DELETE FROM...WHERE...  
    SEELCT...FROM...WHERE...GROUP BY...HAVING...ORDER BY...LIMIT...  
    WITH...AS (),WITH...AS () SELECT...FROM...WHERE...
    SELECT。。。UNION ALL SELECT..   
```
*SQL statement keys can come with built-in validation rule descriptions, making SQL more secure
Conditional use: key characters support the '# MDLIST: N: R: LS: F #' format, where N or M represents non empty, R represents value replacement, LS represents character list, and LI represents integer list, that is, some characters in IN brackets; F represents the preferred configuration for use.
*Generate SQL only, do not execute. To use the generated parameterized SQL, you need to retrieve the parameterized list from the ParserResult, that is, the mapQuery attribute value, which is of type Map<string, SqlKeyValueEntity>.
*Generate SQL types with optional parameterization or character replacement; For single quotes in string values, they are removed and then added before and after the value.

## Background
Since I started working in the software industry, I have often seen the use of concatenated SQL in programs, which is inconvenient for debugging and modification. In 2008, I came across a project that used the ability to dynamically replace keys in SQL based on incoming conditions,
Although it can solve some common SQL problems, as long as it involves function conversions, multiple parentheses with keys, subqueries with keys, etc., it is powerless. The parsed SQL is incorrect (not all conditions are removed when they do not exist) and cannot be run.
Moreover, parameterization is not implemented, and the processing of SQL injection prevention is not perfect. At that time, I was thinking, is there a perfect way to solve it? At that time, my understanding of regular expressions was not deep enough, although I could roughly understand its logic,
But no better way was found. Afterwards, when I used C # to create a work assistant tool, I also discussed it but couldn't solve it. By March 2022, during this relatively idle period of job hunting (most of which is still preparing for interviews),
The technology stack of JAVA is too broad, but I can only rely on the accumulation of time. I have reconsidered this and started writing code for verification. There were also many detours in between. If you initially wanted to support annotations, but found that annotations can be ubiquitous, you need to include them in all regular expressions,
Each regular expression is too redundant. So later on, I simply removed the annotations. After more than a week, the process was gradually streamlined and the code was optimized, targeting every SQL type (SELECT, Insert, UPDATE, DELETE) as much as possible,
Write complex statements to verify the test, and after filling each hole one by one, I finally see the dawn of victory. At present, all the bugs I have discovered have been modified, and the issues mentioned earlier, such as function conversion, multiple parentheses with keys, and subqueries with keys, have been resolved,
Then there are further functional enhancements, such as on-demand configuration, and more auxiliary information configuration for keys. This tool is definitely a disruptive mini product, as MyBatis and MyBatis plug, which are commonly used nowadays, are all configured with conditions in XML to concatenate SQL,
I really dislike this approach because I have to run to know if there are any errors in my SQL. However, this project's approach allows me to write the most complete SQL and run it in the query analyzer (of course, I need to comment out the IN condition section first), so that I can
Check for grammar errors; In the key configuration of SQL, verification information can also be added to ensure the correctness of incoming parameters.

##Implementation ideas
Using Spring Boot's SPI mechanism (automated configuration); More emphasis is placed on character construction, concatenation, deletion, and modification, using a single threaded approach that concatenates SQL statements from front to back,
How efficient is it in large quantities?? The following are the specific processing ideas:
*1. Pass in SQL and key value sets that have already been keyed (# Key Configuration #) (Map<String, Object>)
*2. Pre processing: Remove spaces before and after SQL. Note: Do not convert to uppercase here as some values are case sensitive, and converting all to uppercase will result in SQL condition errors!
*3. Exclude remarks: comments that start with -- or match/* */
*4. Extract the key set and valid key set from SQL: If there are non empty keys passed in but no corresponding values passed in, interrupt the conversion, and return information such as unsuccessful
*5. Remove comment information: comments starting with # (note: the # key will be replaced with other characters before matching)
*6. For regular expressions that conform to (), replace the loop with: # # ordinal # #, which facilitates controlling SQL statement segments from a large perspective and conducting accurate matching analysis
*7. Call the head processing method of the subclass: handle while disassembling
*8. Subclass calls parent class's FROM processing method
*   8.1 Existence of From:
*       8.1.1 Handling from
*       8.1.2 Process WHERE and update the WHERE processing flag to true
*   8.2 If the WHERE processing flag is false, then process the WHERE
*9 Finally, return the ParserResult object and complete the SQL conversion.
*10 Some key logical descriptions:
For each SQL segment, it will be split by AND or OR to ensure that the key segment SQL processed each time only includes one parameter, which facilitates the processing of the entire SQL segment: modify or delete. But the split SQL segment,
Firstly, it is necessary to first check whether there is a # # sequence number # #. If there is, it needs to be analyzed internally: first, perform sub query analysis. If it is a sub query, call the header analysis of the SELECT statement (that is, treat it as a complete SELECT statement to convert);
If it is not a subquery, then it needs to be AND or OR split, and then complex left and right parenthesis processing methods (processing at the beginning and end of the left parenthesis) need to be called. Finally, call the conversion method for a single key (parameterization or character replacement).
During each SQL segment processing, spaces before and after are usually removed. The logic here is not easy to describe. Please refer to the comments in the code for details!

## Tutorial
*1 Download the source code and compile this project command:
`Mvn clean package install`
*2 Using this library in other projects:
*2.1 Introducing Dependencies:
```
<dependency>
    <groupId>org.breezee</groupId>
    <artifactId>mypeach</artifactId>
    <version>1.0.1</version>
</dependency>
```
2.2 Modify Configuration: The following are the default values (parameterization, with # sign before and after the key name, generated parameterization prefix of @, and empty suffix). Generally, it is sufficient to maintain the default values without adding the following configuration.
````
mypeach.target-sql-param-type-enum=param
mypeach.key-style=pound_sign_around
mypeach.param-prefix=@
mypeach.param-suffix=
````
*2.3 Usage: Key characters support '# MDLIST: N: R: LS #' format:
Where N represents non empty; R represents a mandatory value replacement; Both LS and LI represent characters within IN parentheses, which can be passed into an array or ArrayList. Where LI is an integer list, with no quotes around the values.
*2.3.1 Automatic injection objects
```
    String testFilePrefix = "src/main/resources/sql/";
    @Autowired
    SelectSqlParser selectSqlParser;    //方式一：只能做查询SQL转换
    @Autowired
    SqlParsers sqlParsers;  //方式二（推荐）：转换方法parse第一个参数需要指定SQL语句类型
```
*2.3.2 Method calls
````
    public String selecet() throws IOException {
        String sSql = new String(Files.readAllBytes(Paths.get(testFilePrefix + "01_Select.txt")));
        Map<String, Object> dicQuery = new HashMap<>();
        dicQuery.put("PROVINCE_ID","张三");
        dicQuery.put("#PROVINCE_CODE#","BJ");
        dicQuery.put("#PROVINCE_NAME#","北京");
        dicQuery.put("#DATE#","2022-02-10");
        //dicQuery.put("NAME",1);
        dicQuery.put("#REMARK#","测试");
        //dicQuery.put("BF","back");
        List<Integer> list = new ArrayList<Integer>();
        list.addAll(Arrays.asList(2,3,4));
        dicQuery.put("MODIFIER_IN",list);//传入一个数组
        ParserResult parserResult = sqlParsers.parse(SqlTypeEnum.SELECT, sSql, dicQuery);
        return parserResult.getCode().equals("0")?parserResult.getSql():parserResult.getMessage();//0转换成功，返回SQL；1转换失败，返回错误信息
    }
````
If our SQL is like this (don't worry about the meaning of each field or condition in SQL, I just want to include SQL for all situations as much as possible to verify the reliability of the conversion algorithm):
````
SELECT A.[PROVINCE_ID]
  ,A.[PROVINCE_CODE]
 ,B.[CITY_NAME]
 ,((SELECT TOP 1 ID FROM SUB T WHERE T.RID = A.RID AND A.NAME ='#NAME#')) AS ID
  ,A.[UPDATE_CONTROL_ID]
FROM TAB A
LEFT JOIN BAB B on A.ID = B.ID AND A.NAME ='#BNAME#' AND TO_CAHR(A.CDATE,'yyyy-MM-dd') ='#DATE#'
LEFT JOIN BC C on C.ID = B.ID AND C.TNAME ='#TNAME#'
 WHERE PROVINCE_ID = '#PROVINCE_ID#'
	AND UPDATE_CONTROL_ID= '#UPDATE_CONTROL_ID#'
	OR REMARK LIKE '%#REMARK#'
	AND ( ( CREATOR = '#CREATOR#' OR CREATOR_ID = #CREATOR_ID# ) AND TFLG = '#TFLG#')
     AND ( TFLG = '#TFLG#' OR ( CREATOR = '#CREATOR#' OR CREATOR_ID = #CREATOR_ID# ) )
 AND TO_CHAR(TFLG,'yyyy') = '#TFLG2#'
AND TFLG =  TO_DATE('#TFLG#','yyyy-MM-dd')
	AND MODIFIER IN ('#MODIFIER_IN:N:LS#')
AND EXISTS(SELECT 1 FROM TBF G WHERE G.ID = A.ID AND G.BF = '#BF#' )
````
After the above key values are passed in for execution, the returned result is:
````
SELECT A.[PROVINCE_ID]
  ,A.[PROVINCE_CODE]
 ,B.[CITY_NAME]
 ,((SELECT TOP 1 ID FROM SUB T WHERE T.RID = A.RID)) AS ID
  ,A.[UPDATE_CONTROL_ID]
FROM TAB ABAB B ON A.ID = B.ID AND TO_CAHR(A.CDATE,'YYYY-MM-DD') =@DATE
LEFT JOIN BC C ON C.ID = B.ID
 WHERE PROVINCE_ID = @PROVINCE_ID
	OR REMARK LIKE @REMARK
	AND MODIFIER IN ('2','3','4')
AND EXISTS(SELECT 1 FROM TBF G WHERE G.ID = A.ID)
````
If we modify the configuration: mypeach. target sql param type enum=DIRECT_ RUN, then the return result is:
```
SELECT A.[PROVINCE_ID]
  ,A.[PROVINCE_CODE]
 ,B.[CITY_NAME]
 ,((SELECT TOP 1 ID FROM SUB T WHERE T.RID = A.RID)) AS ID
  ,A.[UPDATE_CONTROL_ID]
FROM TAB ABAB B ON A.ID = B.ID AND TO_CAHR(A.CDATE,'YYYY-MM-DD') ='20222-02-10'
LEFT JOIN BC C ON C.ID = B.ID
 WHERE PROVINCE_ID = '张三'
	OR REMARK LIKE '%测试'
	AND MODIFIER IN ('2','3','4')
AND EXISTS(SELECT 1 FROM TBF G WHERE G.ID = A.ID)
```

##Future and Prospects
The path of technology is destined to be full of ups and downs and infinite hardships, but no difficulty can stop my love!! The open source framework has taught me a lot of skills, and I am grateful for their selfless efforts. Always using someone else's,
Actually, I also really want to have my own open source project that can help solve a pain point in everyone's development and further reduce workload (often 996 is a physical test).
MyPeach is a solution that I personally came up with after experiencing the pain points of dynamic SQL (whether it's code concatenation or XML conditional configuration used in Mybatis) and years of accumulation, as well as the idle time I spent looking for a job in March and April 2022, which allowed me to contemplate and come up with a solution.
The project did not use a very impressive technology stack (my personal technical ability is limited), but the solution is definitely excellent. But when a new thing comes out, many companies are afraid to use it casually. It takes years of testing and most problems are solved before it becomes popular.
So I am still very optimistic about this small project, it will be popular in the future^_^ Haha! I also hope to see friends of this project. If you feel the same way and want to solve such pain points, please try it out or help promote it (there are a large number of projects on Github and Gitee),
It is very difficult to attract others' attention. If you have any questions or ideas, please give me feedback or directly participate in the improvement and enhancement of the code. My own project is like my own son, and I will continuously improve it responsibly. I hope he can bring you a wonderful SQL experience,
I also hope that in the future, this tool can be better integrated into Mybatis plug, which will be even more perfect^_^

##Issue and bug submission
Content to be provided for submitting bugs:
*1. SQL that has been keyed
*2. Content of Key Value Condition Set (Map<String, Object>)
*3. Problem Description
[Email feedback suggestions or questions]（ guo7892000@126.com ）
