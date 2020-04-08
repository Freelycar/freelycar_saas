package com.freelycar.screen.entity.repo;

import com.freelycar.screen.entity.ConsumerProjectInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sun.management.jdp.JdpPacket;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author pyt
 * @date 2020/4/2 15:21
 * @email 2630451673@qq.com
 * @desc
 */
@Repository
public interface ConsumerProjectInfoRepository extends JpaRepository<ConsumerProjectInfo, String> {
    List<ConsumerProjectInfo> findByDelStatusAndCreateTimeAfter(boolean delStatus, Timestamp createTime);
    List<ConsumerProjectInfo> findByDelStatus(boolean delStatus);
}
