package com.freelycar.saas.project.repository;

import com.freelycar.saas.project.entity.RSPStoreSort;
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
 * Time: 16:33
 */
@Repository
public interface RSPStoreSortRepository extends JpaRepository<RSPStoreSort, String> {
    RSPStoreSort findTopByStoreIdOrderBySortDesc(String storeId);

    List<RSPStoreSort> findByStoreId(String storeId);

    int deleteByStoreIdAndRspId(String storeId, String rspId);

    List<RSPStoreSort> findByStoreIdAndRspId(String storeId, String rspId);

    @Query(value = "select storeId from RSPStoreSort where rspId = :rspId", nativeQuery = true)
    Set<String> findStoreIdByRspId(String rspId);

    @Query(value = "select rspId from RSPStoreSort where storeId = :storeId", nativeQuery = true)
    List<String> findRspIdByStoreId(String storeId);

}
