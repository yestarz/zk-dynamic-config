## zookeeper学习，并实现一个简单的动态修改spring容器配置的功能

### 代码实现

1. 使用IDEA新建一个spring boot项目，并加入zookeeper的依赖

```xml
<dependency>
        <groupId>org.apache.zookeeper</groupId>
        <artifactId>zookeeper</artifactId>
        <version>3.4.6</version>
        <exclusions>
            <exclusion>
                <groupId>org.slf4j</groupId>
                <artifactId>slf4j-api</artifactId>
            </exclusion>
            <exclusion>
                <groupId>org.slf4j</groupId>
                <artifactId>slf4j-log4j12</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
```

2. 创建zk读取配置并把配置注入到spring容器的类 `ZookeeperPropertyPlaceholderConfigurer`

- 定义几个静态变量

```java
    private static final String SPRING_PARENT_PATH = "/spring";

    private static final String SPRING_CHILD_PATH_PREFIX = SPRING_PARENT_PATH + "/";

    public static ApplicationContext applicationContext;

    private static CountDownLatch connectedSemaphore = new CountDownLatch(1);
    private static ZooKeeper zk = null;
    private static Stat stat = new Stat();
```

- 在构造器中构造zk实例，并配置监听器

```java
public ZookeeperPropertyPlaceholderConfigurer() throws Exception {
        zk = new ZooKeeper("127.0.0.1:2181", 5000,
                (event) -> {
                    if (Watcher.Event.KeeperState.SyncConnected == event.getState()) {  //zk连接成功通知事件
                        if (Watcher.Event.EventType.None == event.getType() && null == event.getPath()) {
                            log.info("zookeeper 连接成功！");
                            connectedSemaphore.countDown();
                        } else if (event.getType() == Watcher.Event.EventType.NodeDataChanged) {  //zk目录节点数据变化通知事件
                            try {
                                log.info("配置已修改，新值为{}", new String(zk.getData(event.getPath(), true, stat)));
                                this.reload(event.getPath());
                            } catch (Exception e) {
                                log.error(e.getMessage(), e);
                            }
                        }
                    }
                });
        //等待zk连接成功的通知
        connectedSemaphore.await();

        // 将数据库配置提供给spring容器
        this.setProperties(this.load());
    }
```

- 从zookeeper中读取数据

```java
    /**
     * 从zookeeper中读取数据
     *
     * @return
     * @throws Exception
     */
    private Properties load() throws Exception {
        // 读取zookeeper中spring节点下的子节点
        List<String> children = zk.getChildren(SPRING_PARENT_PATH, true);

        Properties properties = new Properties();
        for (String child : children) {
            // 获取子节点的数据值
            String data = new String(zk.getData(SPRING_CHILD_PATH_PREFIX + child, true, stat));
            log.info("child:{},data:{}", child, data);
            // 设置到配置对象中
            properties.setProperty(child, data);
        }
        return properties;
    }
```

- 新建一个`AutoUpdate`注解，标记在类上，表示这个类的属性可以进行动态更新

```java
package link.yangxin.zookeeperdemo.annotation;

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
```

- 当zk 节点发生变化时进行重新注入属性 

```java
/**
     * 从zookeeper中读取数据 ，然后遍历spring容器中所有的bean 对已修改的配置进行动态修改
     *
     * @param zkPath
     * @throws Exception
     */
    public void reload(String zkPath) throws Exception {
        if (!zkPath.startsWith(SPRING_CHILD_PATH_PREFIX)) {
            return;
        }
        String node = zkPath.split(SPRING_CHILD_PATH_PREFIX)[1];
        String data = new String(zk.getData(zkPath, true, stat));
        // 获取spring容器中定义的bean列表
        String[] beans = applicationContext.getBeanDefinitionNames();
        for (String beanName : beans) {
            // 拿到bean的Class对象
            Class<?> beanType = applicationContext.getType(beanName);
            if (beanType == null) {
                continue;
            }
            // 当前必须注解了AutoUpdate
            if (beanType.getAnnotation(AutoUpdate.class) == null) {
                continue;
            }
            // 拿到当前bean类型的所有字段
            Field[] declaredFields = beanType.getDeclaredFields();
            for (Field field : declaredFields) {
                // 当前字段如果注解了Value
                Value value = field.getAnnotation(Value.class);
                if (value == null) {
                    continue;
                }
                // value的值是${xxx} 所以要把${}剔除，拿到变量名
                String configName = getValueName(value.value());
                if (configName.equals(node)) {
                    // 从spring容器中拿到这个具体的bean对象
                    Object bean = applicationContext.getBean(beanName);
                    // 当前字段设置新的值
                    setFieldData(field, bean, data);
                }
            }
        }
    }

    private void setFieldData(Field field, Object bean, String data) throws Exception {
        field.setAccessible(true);
        Class<?> type = field.getType();
        if (type.equals(String.class)) {
            field.set(bean, data);
        } else if (type.equals(Integer.class)) {
            field.set(bean, Integer.valueOf(data));
        } else if (type.equals(Long.class)) {
            field.set(bean, Long.valueOf(data));
        } else if (type.equals(Double.class)) {
            field.set(bean, Double.valueOf(data));
        } else if (type.equals(Short.class)) {
            field.set(bean, Short.valueOf(data));
        } else if (type.equals(Byte.class)) {
            field.set(bean, Byte.valueOf(data));
        } else if (type.equals(Boolean.class)) {
            field.set(bean, Boolean.valueOf(data));
        } else if (type.equals(Date.class)) {
            field.set(bean, new Date(Long.valueOf(data)));
        } else {
            field.set(bean, data);
        }
    }
    
    private String getValueName(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("${", "").replace("}", "");
    }

```

3. 新建一个配置类 `Config.java`

```java
package link.yangxin.zookeeperdemo.config;

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
```

4. 新建两个测试controller 一个请求路径为http://localhost:8080/test 另一个为http://localhost:8080/test2

```java
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

    @Value("${port}")
    private Integer port;

    @RequestMapping("/test")
    public String test(){
        return name + " " +  port;
    }
}
```

5. 在zkCli命令行工具中添加几个节点：

- `create /spring/ config` 添加一个spring的节点，节点值是config
- `create /spring/myname yangxin` 添加一个/spring/myname节点 节点值是yangxin
- `create /spring/post 9999` 添加一个/spring/port 节点，节点值是9999

6. 启动SpringBoot程序，此时spring将会读取zk中的/spring节点下的数据，并注入到spring容器中

7. 修改某个节点测试效果 

- `set /spring/myname yangxin1` 将/spring/myname 节点的值设置为yangxin1

> 执行了此命令以后，监听器将会监听到此值的变化，并通过反射的形式进行修改配置的值

8. 此时可以访问`http://localhost:8080/test`查看已经修改成功的值

### zk API简介

> 参考 https://blog.csdn.net/liu88010988/article/details/51577783

### 此项目代码地址

> https://github.com/yangyangxinxin/zk-dynamic-config