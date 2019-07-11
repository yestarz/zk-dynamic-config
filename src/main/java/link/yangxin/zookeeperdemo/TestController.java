package link.yangxin.zookeeperdemo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yangxin
 * @date 2019/7/11
 */
@AutoUpdate
@RestController
public class TestController {

    @Value("${myname}")
    private String name;

    @RequestMapping("/test")
    public String test(){
        return name;
    }



}