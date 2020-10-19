package com.freelycar.saas.project.entity;

import com.freelycar.saas.project.model.RspProjectModel;
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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * Description: 服务商下项目
 * User: Ting
 * Date: 2020-09-10
 * Time: 15:46
 */
@Entity
@Table
@DynamicInsert
@DynamicUpdate
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RSPProject implements Serializable {
    private static final long serialVersionUID = 3281170104969787736L;
    @Id
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @GeneratedValue(generator = "uuid")
    @NotNull
    @Length(max = 50)
    private String id;

    @Column
    private String rspId;

    @Column
    private String name;

    @Column
    private Double price;

    @Column
    private String comment;

    @Column
    private BigDecimal sort;

    @Column(nullable = false, columnDefinition = "bit default 0")
    private Boolean delStatus;

    @Column(nullable = false, columnDefinition = "datetime default NOW()")
    private Timestamp createTime;

    public RspProjectModel toModel() {
        RspProjectModel model = new RspProjectModel();
        model.setId(this.id);
        model.setName(this.name);
        model.setPrice(this.price);
        model.setComment(this.comment);
        return model;
    }
}
