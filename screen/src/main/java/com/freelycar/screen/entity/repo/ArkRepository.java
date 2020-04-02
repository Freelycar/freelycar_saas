package com.freelycar.screen.entity.repo;

import com.freelycar.screen.entity.Ark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author pyt
 * @date 2020/4/1 11:02
 * @email 2630451673@qq.com
 * @desc
 */
@Repository
public interface ArkRepository extends JpaRepository<Ark,String> {
    List<Ark> findByDelStatusAndCreateTimeAfter(boolean delStatus,Timestamp createTime);
    List<Ark> findByDelStatusAndCreateTimeBefore(boolean delStatus,Timestamp createTime);
}
