package com.freelycar.screen.entity.repo;

import com.freelycar.screen.entity.ConsumerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

/**
 * @author pyt
 * @date 2020/4/1 16:22
 * @email 2630451673@qq.com
 * @desc
 */
@Repository
public interface ConsumerOrderRepository extends JpaRepository<ConsumerOrder, String> {
    List<ConsumerOrder> findByDelStatusAndCreateTimeAfter(boolean delStatus, Timestamp createTime);

    @Query(value = "select clientId from ConsumerOrder where delStatus = ?1")
    Set<String> findByDelStatus(boolean delStatus);
}
