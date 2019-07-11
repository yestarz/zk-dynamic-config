package link.yangxin.zookeeperdemo;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * 从zookeeper中读取数据，注入到spring容器中
 *
 * @author yangxin
 * @date 2019/7/11
 */
@Slf4j
public class ZookeeperPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer implements ApplicationContextAware {

    public static ApplicationContext applicationContext;

    private CountDownLatch connectedSemaphore = new CountDownLatch(1);
    private ZooKeeper zk = null;
    private Stat stat = new Stat();

    public ZookeeperPropertyPlaceholderConfigurer() throws Exception {
        super();
        zk = new ZooKeeper("127.0.0.1:2181", 5000,
                (event) -> {
                    if (Watcher.Event.KeeperState.SyncConnected == event.getState()) {  //zk连接成功通知事件
                        if (Watcher.Event.EventType.None == event.getType() && null == event.getPath()) {
                            log.info("zookeeper 连接成功！");
                            connectedSemaphore.countDown();
                        } else if (event.getType() == Watcher.Event.EventType.NodeDataChanged) {  //zk目录节点数据变化通知事件
                            try {
                                log.info("配置已修改，新值为{}", new String(zk.getData(event.getPath(), true, stat)));
                                this.reload();
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

    /**
     * 从zookeeper中读取数据
     *
     * @return
     * @throws Exception
     */
    private Properties load() throws Exception {
        // 读取zookeeper中spring节点下的子节点
        List<String> children = zk.getChildren("/spring", true);

        Properties properties = new Properties();
        for (String child : children) {
            // 获取子节点的数据值
            String data = new String(zk.getData("/spring" + "/" + child, true, stat));
            log.info("child:{},data:{}", child, data);
            // 设置到配置对象中
            properties.setProperty(child, data);
        }
        return properties;
    }

    /**
     * 从zookeeper中读取数据 ，然后遍历spring容器中所有的bean 对已修改的配置进行动态修改
     *
     * @throws Exception
     */
    private void reload() throws Exception {
        Properties properties = this.load();

        // 获取spring容器中定义的bean列表
        String[] beans = applicationContext.getBeanDefinitionNames();
        for (String beanName : beans) {
            // 拿到bean的Class对象
            Class<?> beanType = applicationContext.getType(beanName);
            if (beanType == null) {
                continue;
            }
            // 当前必须注解了AutoUpdate
            AutoUpdate autoUpdate = beanType.getAnnotation(AutoUpdate.class);
            if (autoUpdate == null) {
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
                field.setAccessible(true);
                // 从spring容器中拿到这个具体的bean对象
                Object bean = applicationContext.getBean(beanName);
                // value的值是${xxx} 所以要把${}剔除，拿到变量名
                String configName = getValueName(value.value());
                // 通过变量名拿到具体的变量值
                String configValue = properties.getProperty(configName);
                // 当前字段设置新的值
                field.set(bean, configValue);
            }
        }
    }

    private String getValueName(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("${", "").replace("}", "");
    }

    @Override
    protected void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props)
            throws BeansException {
        super.processProperties(beanFactory, props);
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ZookeeperPropertyPlaceholderConfigurer.applicationContext = applicationContext;
    }
}
