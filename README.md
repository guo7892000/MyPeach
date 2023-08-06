MyPeach Dynamic SQL Parser Tool for Java
=================
## 概述
MyPeach是一个动态SQ转换工具，它能根据SQL中配置的键（默认格式：#键名#）和 键值集合（Map<String, Object>）来生成动态的SQL。  
即某键如有值传入，那么会将该条件保留，并将其参数化或字符替换（可配置选择）；否则将该条件抛弃或修改为 AND 1=1（一些用括号括起来的多个条件分析时）。
可动态的部分包括：所有类型的条件，INSERT项，UPDATE项。
## 特点
* 基于Spring Boot，非常轻量
* 数据库无关性
* 支持的语句样式：
```
    INSER INTO ...VALUES... 
    INSERT INTO...SELECT...FROM...WHERE... 
    INSERT INTO...WITH...SELECT...FROM...WHERE... 
    WITH...INSERT INTO... SELECT...FROM...WHERE... 
    UPDATE ... SET...FROM...WHERE...  
    DELETE FROM...WHERE...  
    SEELCT...FROM...WHERE...GROUP BY...HAVING...ORDER BY...LIMIT...  
    WITH...AS (),WITH...AS () SELECT...FROM...WHERE...
    SELECT...UNION ALL SELECT...   
```
* SQL语句键可带内置的校验规则描述，让SQL更安全  
  条件使用：键字符支持'#MDLIST:N:R:LS:F#'格式，其中N或M表示非空，R表示值替换，LS表示字符列表，LI为整型列表，即IN括号里的部分字符；F表示优先使用的配置。  
* 只生成SQL，不执行。如需使用生成后的参数化SQL，需要从ParserResult中取出参数化列表，即mapQuery属性值，其为Map<string, SqlKeyValueEntity>类型。  
* 生成SQL类型可选参数化、还是字符替换；对于字符串值中的单引号，会被剔除掉，然后再在值前后分别加上单引号。  
## 背景
从本人从事软件行业以来，就经常能看到程序中用到拼接SQL，这样不方便调试和修改。在2008年我接触到的一个项目，使用了能根据传入条件动态替换SQL中的键，
虽然能解决一些常见的SQL，但只要涉及到有函数转换、多个括号内有键、子查询有键等，它就无能为力了，解析出来的SQL就是错的（条件不存在时没有全部去掉），无法运行。
而且没有实现参数化，对防SQL注入的处理也不是很完善。那时我就在想，有没有一种完美的方法去解决它呢?那时本人对正则表达式的理解不够深刻，虽然能大略看懂它的逻辑，
但也没找到更好的办法。此后，自己用c#做的一个工作助手小工具时， 也探讨过，但也未能解决。到2022年3月，在这段找工作比较空闲的时间里（大部分时间还是为面试准备，
JAVA的技术栈太广，无奈只能靠日积月累），我重新对此进行了思考， 并开始写代码进行验证。这中间也走了很多弯路。如最开始想支持注释，但发现注释可以无处不在，要在所有正则表达式里包括它，
每个正则表达式就太冗余了。所以后来我干脆剔除了注释。 经过一周多的时间，慢慢理顺了流程，并优化了代码，尽量针对每种SQL类型（SELECT，INSERT，UPDATE，DELETE），
写出复杂的语句来校验测试， 在一个一个坑被填了之后， 我终于看到了胜利的曙光。目前我发现的BUG都已修改，之前提到过的函数转换、多个括号内有键、子查询有键等问题都已解决，
然后就是进一步功能增强，如按需的配置化、键的更多辅助信息配置等。本工具绝对是一个颠覆性的迷你产品，因为现在用的比较多的MyBatis、MyBatis-plug目前都是在XML中配置条件来拼接SQL，
我很讨厌这种方式，因为必须跑起来才知道自己写的SQL有没报错，而本项目的方式可以把包含最全的SQL写好，然后在查询分析器中运行（当然对IN条件那段要先注释掉），从而能
检查出语法有没错误；在SQL的键配置中，还可以附加校验信息来保证传入参数的正确性。
## 实现思路
使用Spring Boot的SPI机制（自动化配置）；更多的是字符构建、拼接、删除、修改，使用的是单线程方式，按SQL语句的从前至后的处理方式来拼接，
不知在大数量下效率如何？？以下为具体处理思路：  
* 1.传入已经键化（#键配置#）的SQL和键值集合（Map<String, Object>）
* 2.预处理：去掉SQL前后空格。注：这里不要转换为大写，因为存在一些值是区分大小的，全部转换为大写会导致SQL条件错误！
* 3.剔除备注信息：以--开头或符合/**/的注释
* 4.取出SQL中的键集合和有效键集合：如果有非空键传入，但没有对应值传入，中断转换，返回不成功等信息
* 5.剔除备注信息：以#开头的注释（注：会优先将#键配置#的替换为其他字符后，才匹配） 
* 6.对于符合()正则式，循环替换为：##序号##，这样就方便从大的方面掌控SQL语句段，进行准确匹配分析
* 7.调用子类的头部处理方法：边拆边处理  
* 8.子类调用父类的FROM处理方法
  * 8.1 存在FROM：  
    * 8.1.1 处理FROM  
    * 8.1.2 处理WHERE，更新WHERE处理标志为true  
  * 8.2 如果WHERE处理标志为false，那么处理WHERE  
* 9. 最后返回ParserResult对象，转换SQL结束。其中返回的结果对象中，sql为转换为参数化后的SQL，mapObject为有效参数值集合，mapQuery为有效详细参数实体信息，mapError为转换过程的错误信息，
*    positionCondition为位置参数值，code为返回参数（0成功，1失败），message为成功或失败信息，sourceSql为转换前的SQL。
* 10. 一些关键逻辑描述：  
   对于每个SQL段，都会按AND或OR进行分拆，保证每次处理的键段SQL只包括一个参数，这样就方便整段SQL的处理：修改还是删除。但分拆出来的SQL段， 
   首先还是先做是否有##序号##，有则需要对其内部先分析：先做子查询分析，如果是子查询，则要调用一次SELECT语句的头部分析（即把它也当作一个完整的SELCT语句来转换）；
   如不是子查询，那么又要对它进行AND或OR进行分拆，这时又要调用复杂的左右括号处理方法（左括号开头和右括号结尾的处理）。最后再调用单个键的转换方法（参数化还是字符替换）。
   每一次SQL段处理时，一般会去掉前后的空格。此处的逻辑不好描述，详细请见代码中的注释！

## 使用教程
* 1. 下载源码后编译本项目命令：  
    `mvn clean package install`
* 2. 在其他项目中使用该类库：  
* 2.1 引入依赖：  
```
<dependency>
    <groupId>org.breezee</groupId>
    <artifactId>mypeach</artifactId>
    <version>1.0.1</version>
</dependency>
```
* 2.2 修改配置：以下为默认值（参数化，键名前后加#号，生成的参数化前缀为@，后缀为空），一般保持默认即可，不用增加下面配置。  
````
mypeach.target-sql-param-type-enum=param
mypeach.key-style=pound_sign_around
mypeach.param-prefix=@
mypeach.param-suffix=
````
* 2.3 使用：键字符支持'#MDLIST:N:R:LS#'格式:
    其中N表示非空；R表示必须值替换；LS和LI都表示IN括号里的字符，可以传入数组或ArrayList。其中LI为整型列表，值两边不加引号。  
* 2.3.1 自动注入对象  
```
    String testFilePrefix = "src/main/resources/sql/";
    @Autowired
    SelectSqlParser selectSqlParser;    //方式一：只能做查询SQL转换
    @Autowired
    SqlParsers sqlParsers;  //方式二（推荐）：转换方法parse第一个参数需要指定SQL语句类型
```

* 2.3.2 方法调用  
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
如果我们的SQL是这样的(不要在意SQL中每个字段或条件的意义，我只想尽量包括所有情况的SQL来验证转换算法的可靠性)：  
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
那么在上述键值传入执行后，返回的结果为：  
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
如果我们修改配置：mypeach.target-sql-param-type-enum=DIRECT_RUN，那么返回结果为：
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
## 未来与展望
技术之路注定是充满坎坷和无限艰辛的，但任何困难都无法阻挡我的热爱！！开源框架给我学习了很多技能，感谢他们的无私付出。一直使用别人的，
其实我也很想有一个属于自己的，能帮忙解决大家的开发中某个痛点的，进一步减轻工作量（经常996对身体是个考验）的开源项目。 
MyPeach是我个人在经历过动态SQL的痛点（无论是代码中拼接，还是Mybatis中使用的XML条件配置）和多年沉淀积累后，也因2022年3、4月找工作的这段空闲时间让我静心思考后出的一种解决方案。
项目没用到很牛的技术栈（个人技术能力有限），但解决思路绝对是很优秀的。但一个新东西出来，很多企业是不敢随便用的，是要经过多年检验， 大部分问题解决了才会流行起来。 
所以我还是非常看好这个小项目，他日必火^_^哈哈！也希望看到这个项目的朋友，如你也有同样的感受，也想解决这样的痛点，那么请试用试用，或帮忙宣传宣传（Github、Gitee上有海量的项目，
想引起别人的关注是非常难的）。 如果有问题或想法，请反馈给我，或直接参与代码的完善与增强。 自己的项目就像自家儿子一样，我会很负责任地不断完善他，希望他能给你们带来美好的SQL体验，
也希望未来本工具能更好地集成到Mybatis-plug上， 那就更完美了^_^

## 问题和BUG提交
提交BUG需提供的内容：  
* 1.已经键化的SQL   
* 2.键值条件集合（Map<String, Object>）的内容   
* 3.问题简述  
[邮件反馈建议或问题](guo7892000@126.com)
[微信号] BreezeeHui