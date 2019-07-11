package link.yangxin.zookeeperdemo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yangxin
 * @date 2019/7/11
 */
@RestController
@AutoUpdate
public class TestController2 {

    @Value("${myname}")
    private String name;

    @RequestMapping("/test2")
    public String test(){
        return name;
    }

}