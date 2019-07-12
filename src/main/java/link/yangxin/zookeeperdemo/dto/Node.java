package link.yangxin.zookeeperdemo.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author yangxin
 * @date 2019/7/12
 */
@Data
public class Node implements Serializable {

    private String path;

    private String value;

    private List<String> nodes;

}