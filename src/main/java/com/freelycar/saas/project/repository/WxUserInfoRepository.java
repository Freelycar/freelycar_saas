package com.freelycar.saas.project.repository;

import com.freelycar.saas.project.entity.WxUserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

/**
 * @author tangwei
 * @date 2018/9/25
 */
public interface WxUserInfoRepository extends JpaRepository<WxUserInfo, String> {
    WxUserInfo findWxUserInfoByOpenId(String openId);

    WxUserInfo findWxUserInfoByPhone(String phone);

    WxUserInfo findWxUserInfoByDelStatusAndPhone(boolean delStatus, String phone);

    List<WxUserInfo> findByDelStatusAndCreateTimeAfter(boolean delStatus, Timestamp createTime);

    List<WxUserInfo> findByDelStatusAndCreateTimeBefore(boolean delStatus, Timestamp createTime);

    @Query(value = "select defaultClientId from WxUserInfo where delStatus = ?1 and createTime >= ?2")
    Set<String> findClientIdByDelStatusAndCreateTimeAfter(boolean delStatus, Timestamp createTime);
}
