package com.freelycar.saas.project.repository;

import com.freelycar.saas.project.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author: pyt
 * @Date: 2021/4/21 10:44
 * @Description:
 */
@Repository
public interface ReminderRepository extends JpaRepository<Reminder, String> {
    @Query(value = "select * from reminder where (toClient = :id or toEmployee = :id) and isRead = false order by createTime desc", nativeQuery = true)
    List<Reminder> findByToId(String id);

    @Query(value = "select count(1) from reminder where (toClient = :id or toEmployee = :id) and isRead = false order by createTime desc", nativeQuery = true)
    Integer countByToId(String id);
}
