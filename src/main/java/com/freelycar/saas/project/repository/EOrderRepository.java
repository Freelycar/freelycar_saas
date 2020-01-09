package com.freelycar.saas.project.repository;

import com.freelycar.saas.project.entity.EOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * @author puyuting
 * @date 2020/1/3
 * @email 2630451673@qq.com
 */
public interface EOrderRepository extends JpaRepository<EOrder, String> {

    List<EOrder> findByConsumerOrderId(String consumerOrderId);

    Optional<EOrder> findByOrderId(Integer orderId);
}
