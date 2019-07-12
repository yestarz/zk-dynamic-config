package link.yangxin.zookeeperdemo.controller;

import link.yangxin.zookeeperdemo.annotation.AutoUpdate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yangxin
 * @date 2019/7/11
 */
@AutoUpdate
@RestController
public class TestController {

    @Value("${myname}")
    private String name;

    @Value("${server.port}")
    private Integer port;

    @RequestMapping("/test")
    public String test(){
        return name + " " +  port;
    }
}