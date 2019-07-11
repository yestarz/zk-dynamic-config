package link.yangxin.zookeeperdemo;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yangxin
 * @date 2019/7/11
 */
@Configuration
public class Config{



    @Bean
    public static PropertyPlaceholderConfigurer properties() throws Exception {
        return new ZookeeperPropertyPlaceholderConfigurer();
    }


}