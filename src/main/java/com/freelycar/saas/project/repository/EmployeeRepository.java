package com.freelycar.saas.project.repository;

import com.freelycar.saas.project.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * @author tangwei - Toby
 * @date 2019-06-17
 * @email toby911115@gmail.com
 */
public interface EmployeeRepository extends JpaRepository<Employee, String> {
    Employee findTopByAccountAndPasswordAndDelStatus(String account, String password, boolean delStatus);

    Employee findTopByPhoneAndDelStatus(String phone, boolean delStatus);

    List<Employee> findAllByAgentIdAndDelStatus(String agentId, boolean delStatus);

    /**
     * 验证系统手机号唯一性（排除数据本身）
     *
     * @param id
     * @param agentId
     * @param phone
     * @return
     */
    @Query(value = "select * from employee where id != :id and delStatus = 0 and agentId=:agentId and phone = :phone", nativeQuery = true)
    List<Employee> checkRepeatPhone(String id, String agentId, String phone);

    /**
     * 验证系统中手机号唯一性（不排除数据本身）
     *
     * @param agentId
     * @param phone
     * @return
     */
    @Query(value = "select * from employee where delStatus = 0 and agentId=:agentId and phone = :phone", nativeQuery = true)
    List<Employee> checkRepeatPhone(String agentId, String phone);
}
