package com.freelycar.saas.project.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * @author tangwei - Toby
 * @date 2018/10/18
 * @email toby911115@gmail.com
 */
@Entity
@Table
@DynamicInsert
@DynamicUpdate
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Project implements Serializable {
    private static final long serialVersionUID = 7L;

    @Id
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @GeneratedValue(generator = "uuid")
    @NotNull
    @Length(max = 50)
    private String id;

    @Column(nullable = false, columnDefinition = "bit default 0")
    private Boolean delStatus;

    @Column(nullable = false, columnDefinition = "datetime default NOW()")
    private Timestamp createTime;

    /**
     * 备注（内容）
     */
    @Column
    private String comment;

    /**
     * 项目名称
     */
    @Column
    private String name;

    /**
     * 金额
     */
    @Column(nullable = false, columnDefinition = "float default 0.0")
    private Float price;

    /**
     * 价格是否标准
     */
    @Column
    private Byte standard;

    /**
     * 会员价格
     */
    @Column(nullable = false, columnDefinition = "float default 0.0")
    private Float memberPrice;

    /**
     * 单价（冗余）
     */
    @Column(columnDefinition = "float default 0.0")
    private Float pricePerUnit;

    /**
     * 参考工时
     */
    @Column
    private Integer referWorkTime;

    /**
     * 项目类别ID
     */
    @Column
    private String projectTypeId;

    /**
     * 项目类别对象
     */
    @Transient
    private ProjectType projectType;

    /**
     * 项目类别名称
     */
    @Transient
    private String projectTypeName;

    /**
     * 使用次数
     */
    @Column
    private Integer useTimes;

    /**
     * 智能柜上架标记
     */
    @Column(nullable = false, columnDefinition = "bit default 0")
    private Boolean saleStatus;

    @Column
    private String storeId;

    /**
     * 上架标记
     */
    @Column(nullable = false, columnDefinition = "bit default 0")
    private Boolean bookOnline;

    @Transient
    private boolean staffReady = false;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":\"")
                .append(id).append('\"');
        sb.append(",\"delStatus\":")
                .append(delStatus);
        sb.append(",\"createTime\":\"")
                .append(createTime).append('\"');
        sb.append(",\"comment\":\"")
                .append(comment).append('\"');
        sb.append(",\"name\":\"")
                .append(name).append('\"');
        sb.append(",\"price\":")
                .append(price);
        sb.append(",\"memberPrice\":")
                .append(memberPrice);
        sb.append(",\"pricePerUnit\":")
                .append(pricePerUnit);
        sb.append(",\"referWorkTime\":")
                .append(referWorkTime);
        sb.append(",\"projectTypeId\":\"")
                .append(projectTypeId).append('\"');
        sb.append(",\"projectType\":")
                .append(projectType);
        sb.append(",\"projectTypeName\":\"")
                .append(projectTypeName).append('\"');
        sb.append(",\"useTimes\":")
                .append(useTimes);
        sb.append(",\"saleStatus\":")
                .append(saleStatus);
        sb.append(",\"storeId\":\"")
                .append(storeId).append('\"');
        sb.append(",\"bookOnline\":")
                .append(bookOnline);
        sb.append('}');
        return sb.toString();
    }
}
