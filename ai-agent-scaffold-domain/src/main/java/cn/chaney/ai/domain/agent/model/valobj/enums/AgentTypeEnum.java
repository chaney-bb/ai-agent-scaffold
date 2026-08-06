package cn.chaney.ai.domain.agent.model.valobj.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author chaney
 * @description 工作流类型枚举：YAML type ↔ 装配节点 Bean 名
 * @create 2026/8/6 14:30
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum AgentTypeEnum {

    /** name=说明，type=配置值，node=Spring Bean 名 */
    Loop("循环执行", "loop", "loopAgentNode"),
    Parallel("并行执行", "parallel", "parallelAgentNode"),
    Sequential("串行执行", "sequential", "sequentialAgentNode"),

    ;

    private String name;
    private String type;
    private String node;

    /**
     * 按 YAML 的 type 查找枚举（忽略大小写）；找不到返回 null
     */
    public static AgentTypeEnum formType(String type) {
        if (type == null) {
            return null;
        }
        // values()：编译器为枚举生成的静态方法，返回全部常量
        for (AgentTypeEnum value : values()) {
            if (value.getType().equalsIgnoreCase(type)) {
                return value;
            }
        }
        return null;
    }
}
