package link.yangxin.zookeeperdemo;

import java.lang.annotation.*;

/**
 * @author yangxin
 * @date 2019/7/11
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoUpdate {
}
