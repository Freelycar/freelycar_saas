package com.freelycar.saas.project.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 微信公众号关注总人数
 * 按照微信官方文档说法，每天8点后才能保证上一天的统计数字生成
 * 所以定时任务尽量设置在8点以后，避免问题
 */
@Entity
@Data
public class WxCumulate implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 创建时间
     */
    @Column(columnDefinition = "datetime default NOW()")
    private Timestamp createTime;

    /**
     * 统计时间
     */
    @Column
    private String refDate;

    /**
     * 关注总人数
     */
    @Column
    private Long cumulateUser;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":")
                .append(id);
        sb.append(",\"createTime\":\"")
                .append(createTime).append('\"');
        sb.append(",\"refDate\":\"")
                .append(refDate).append('\"');
        sb.append(",\"cumulateUser\":")
                .append(cumulateUser);
        sb.append('}');
        return sb.toString();
    }
}
