package link.yangxin.zookeeperdemo.controller;

import link.yangxin.zookeeperdemo.config.ZookeeperPropertyPlaceholderConfigurer;
import link.yangxin.zookeeperdemo.dto.Node;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author yangxin
 * @date 2019/7/12
 */
@Slf4j
@RestController
public class ZkController {

    @GetMapping("/listZkNode")
    public Node listZkNode(String parent) {
        try {
            ZooKeeper zookeeper = ZookeeperPropertyPlaceholderConfigurer.getZookeeper();
            Stat stat = ZookeeperPropertyPlaceholderConfigurer.getStat();
            List<String> children = zookeeper.getChildren(parent, true);
            Node node = new Node();
            node.setPath(parent);
            node.setValue(new String(zookeeper.getData(parent, true, stat)));
            node.setNodes(children);
            return node;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @RequestMapping("/setData")
    public void setData(String path, String data) {
        try {
            ZooKeeper zookeeper = ZookeeperPropertyPlaceholderConfigurer.getZookeeper();
            zookeeper.setData(path, data.getBytes(), -1);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}