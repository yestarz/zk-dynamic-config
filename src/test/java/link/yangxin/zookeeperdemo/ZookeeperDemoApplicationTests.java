package link.yangxin.zookeeperdemo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ZookeeperDemoApplicationTests {

    @Value("${server.port}")
    private String port;

    @Value("${myname}")
    private String name;

    @Test
    public void contextLoads() {
        System.out.println(port);
        System.out.println(name);
    }

}
