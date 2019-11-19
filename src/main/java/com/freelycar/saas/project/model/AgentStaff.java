package com.freelycar.saas.project.model;

import com.freelycar.saas.project.entity.Staff;
import lombok.Data;

import java.sql.Timestamp;
import java.util.Set;

@Data
public class AgentStaff {

    private String employeeId;

    private String agentId;

    private String staffNumber;

    private String name;

    private String gender;

    private String phone;

    private Set<Staff> staff;

    private String position;

    private String level;

    private Timestamp createTime;
}
