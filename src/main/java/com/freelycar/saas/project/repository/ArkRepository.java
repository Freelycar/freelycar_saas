package com.freelycar.saas.project.repository;

import com.freelycar.saas.project.entity.Ark;
import com.freelycar.saas.project.model.ArkStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author tangwei - Toby
 * @date 2019-02-18
 * @email toby911115@gmail.com
 */
public interface ArkRepository extends JpaRepository<Ark, String> {
    Ark findTopBySnAndDelStatus(String sn, boolean delStatus);

    Page<Ark> findAllByStoreIdAndSnContainingAndDelStatus(String storeId, String sn, boolean delStatus, Pageable pageable);

    Page<Ark> findAllBySnContainingAndDelStatus(String sn, boolean delStatus, Pageable pageable);

    List<Ark> findByDelStatusAndCreateTimeAfter(boolean delStatus, Timestamp createTime);

    List<Ark> findByDelStatusAndCreateTimeBefore(boolean delStatus, Timestamp createTime);

    List<Ark> findByDelStatus(boolean delStatus);

    @Query(value = "select new com.freelycar.saas.project.model.ArkStore(ark.sn,ark.location,store.name) FROM Ark as ark LEFT JOIN Store as store ON ark.storeId = store.id WHERE ark.delStatus = ?1")
    List<ArkStore> findSnByDelStatus(boolean delStatus);
}
