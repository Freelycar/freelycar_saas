package com.freelycar.saas.project.repository;

import com.freelycar.saas.project.entity.RspProjectStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Ting
 * Date: 2020-10-14
 * Time: 15:05
 */
@Repository
public interface RspProjectStoreRepository extends JpaRepository<RspProjectStore, String> {
    @Query(value = "select rspProjectId from RspProjectStore where storeId = :storeId", nativeQuery = true)
    Set<String> findRspProjectIdByStoreId(String storeId);

    int deleteByStoreIdAndRspProjectIdIn(String storeId, List<String> rspProjectIds);

    List<RspProjectStore> findByStoreIdAndRspProjectId(String storeId, String rspProjectId);
}
