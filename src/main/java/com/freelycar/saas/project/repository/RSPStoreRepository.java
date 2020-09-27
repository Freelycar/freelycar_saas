package com.freelycar.saas.project.repository;

import com.freelycar.saas.project.entity.RSPStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Ting
 * Date: 2020-09-25
 * Time: 14:53
 */
@Repository
public interface RSPStoreRepository extends JpaRepository<RSPStore, String> {
    @Query(value = "select storeId from RSPStore where rspId = :rspId", nativeQuery = true)
    Set<String> findByRspId(String rspId);

    List<RSPStore> findByStoreIdAndRspId(String storeId, String rspId);
}
