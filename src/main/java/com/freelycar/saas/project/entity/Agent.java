package com.freelycar.saas.project.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.validator.constraints.Length;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;

/**
 * 代理商
 */
@Entity
@Data
public class Agent {
    /**
     * 主键ID
     */
    @Id
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @GeneratedValue(generator = "uuid")
    @NotNull
    @Length(max = 50)
    private String id;

    /**
     * 删除标记位（0：有效；1：无效）
     */
    @Column(nullable = false, columnDefinition = "bit default 0")
    private Boolean delStatus;

    /**
     * 创建时间
     */
    @Column(nullable = false, columnDefinition = "datetime default NOW()")
    private Timestamp createTime;

    private String agentName;

    private String linkMan;

    private String linkPhone;

    private String remark;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":\"")
                .append(id).append('\"');
        sb.append(",\"delStatus\":")
                .append(delStatus);
        sb.append(",\"createTime\":\"")
                .append(createTime).append('\"');
        sb.append(",\"agentName\":\"")
                .append(agentName).append('\"');
        sb.append(",\"linkMan\":\"")
                .append(linkMan).append('\"');
        sb.append(",\"linkPhone\":\"")
                .append(linkPhone).append('\"');
        sb.append(",\"remark\":\"")
                .append(remark).append('\"');
        sb.append('}');
        return sb.toString();
    }
}
