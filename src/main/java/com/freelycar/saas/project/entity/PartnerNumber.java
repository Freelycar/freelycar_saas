package com.freelycar.saas.project.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * @author tangwei - Toby
 * @date 2019/8/22
 * @email toby911115@gmail.com
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartnerNumber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, columnDefinition = "bigint default 0")
    private Long num;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":")
                .append(id);
        sb.append(",\"num\":")
                .append(num);
        sb.append('}');
        return sb.toString();
    }
}
