package org.breezee.mypeach.autoconfigure;

import org.breezee.mypeach.core.SqlParsers;
import org.breezee.mypeach.core.DeleteSqlParser;
import org.breezee.mypeach.core.InsertSqlParser;
import org.breezee.mypeach.core.SelectSqlParser;
import org.breezee.mypeach.core.UpdateSqlParser;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @objectName: 自动配置类
 * @description: 符合Spring Boot的SPI机制，只要添加依赖就即可
 * @author: guohui.huang
 * @email: guo7892000@126.com
 * @wechat: BreezeeHui
 * @date: 2022/4/12 16:45
 */
@Configuration
@EnableConfigurationProperties(value = {MyPeachProperties.class})
public class MyPeachAutoConfiguration {

    /**
     * 新增SQL转换器
     * @param properties
     * @return
     */
    @Bean
    public InsertSqlParser insertSqlAnalyzer(MyPeachProperties properties){
        return new InsertSqlParser(properties);
    }

    /**
     * 更新SQL转换器
     * @param properties
     * @return
     */
    @Bean
    public UpdateSqlParser updateSqlAnalyzer(MyPeachProperties properties){
        return new UpdateSqlParser(properties);
    }
    /**
     * 删除SQL转换器
     * @param properties
     * @return
     */
    @Bean
    public DeleteSqlParser deleteSqlAnalyzer(MyPeachProperties properties){
        return new DeleteSqlParser(properties);
    }
    /**
     * 查询SQL转换器
     * @param properties
     * @return
     */
    @Bean
    public SelectSqlParser selectSqlAnalyzer(MyPeachProperties properties){
        return new SelectSqlParser(properties);
    }

    /**
     * 统一的SQL转换器
     * @param properties
     * @return
     */
    @Bean
    public SqlParsers sqlParsers(MyPeachProperties properties){
        return new SqlParsers(properties);
    }
}
