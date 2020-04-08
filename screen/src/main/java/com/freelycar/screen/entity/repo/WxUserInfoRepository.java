package com.freelycar.screen.entity.repo;

import com.freelycar.screen.entity.WxUserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author pyt
 * @date 2020/4/1 15:37
 * @email 2630451673@qq.com
 * @desc
 */
@Repository
public interface WxUserInfoRepository extends JpaRepository<WxUserInfo,String> {
    List<WxUserInfo> findByDelStatusAndCreateTimeAfter(boolean delStatus,Timestamp createTime);

    List<WxUserInfo> findByDelStatusAndCreateTimeBefore(boolean delStatus,Timestamp createTime);
}
