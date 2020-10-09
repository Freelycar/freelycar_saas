package com.freelycar.saas.project.repository;

import com.freelycar.saas.project.entity.RspStaffStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Ting
 * Date: 2020-09-30
 * Time: 14:51
 */
@Repository
public interface RspStaffStoreRepository extends JpaRepository<RspStaffStore, String> {
    @Query(value = "select storeId from RspStaffStore where staffId = :staffId", nativeQuery = true)
    Set<String> findByStaffId(String staffId);

    int deleteByStaffId(String staffId);

    @Query(value = "select staffId from RspStaffStore where storeId in :storeIds", nativeQuery = true)
    Set<String> findByStoreIdIn(Set<String> storeIds);
}
