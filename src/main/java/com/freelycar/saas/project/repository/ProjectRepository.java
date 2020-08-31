package com.freelycar.saas.project.repository;

import com.freelycar.saas.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * @author tangwei - Toby
 * @date 2018/10/18
 * @email toby911115@gmail.com
 */
public interface ProjectRepository extends JpaRepository<Project, String> {
    @Query(value = "select * from project where id != :id and storeId = :storeId and delStatus = 0 and name = :name", nativeQuery = true)
    List<Project> checkRepeatName(@Param("id") String id, @Param("name") String name, @Param("storeId") String storeId);

    @Query(value = "select * from project where storeId = :storeId and delStatus = 0 and name = :name", nativeQuery = true)
    List<Project> checkRepeatName(@Param("name") String name, @Param("storeId") String storeId);

    Page<Project> findAllByDelStatusAndStoreIdAndNameContainingAndProjectTypeIdOrderBySortAsc(boolean delStatus, String storeId, String name, String projectTypeId, Pageable pageable);

    Page<Project> findAllByDelStatusAndStoreIdAndNameContainingOrderBySortAsc(boolean delStatus, String storeId, String name, Pageable pageable);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "update project set delStatus = 1 where id=:id", nativeQuery = true)
    int delById(String id);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "update project set saleStatus = 1 where id=:id", nativeQuery = true)
    int uppArkById(@Param("id") String id);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "update project set saleStatus = 0 where id=:id", nativeQuery = true)
    int lowArkById(String id);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "update project set bookOnline = 1 where id=:id", nativeQuery = true)
    int uppById(String id);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "update project set bookOnline = 0 where id=:id", nativeQuery = true)
    int lowById(String id);

    List<Project> findAllByStoreIdAndDelStatusAndSaleStatusOrderByCreateTime(String storeId, boolean delStatus, boolean saleStatus);

    List<Project> findAllByStoreIdAndDelStatusAndSaleStatusOrderBySortAsc(String storeId, boolean delStatus, boolean saleStatus);

    List<Project> findByStoreIdAndDelStatusAndBookOnline(String storeId, boolean delStatus, boolean bookOnline);

    List<Project> findByDelStatusAndIdIn(boolean delStatus, Set<String> id);

    @Query(value = "select id from project where delStatus = :delStatus", nativeQuery = true)
    Set<String> findDistinctIdByDelStatus(@Param("delStatus") boolean delStatus);

    Project findByStoreIdAndName(String storeId, String name);

    Project findTopByStoreIdAndDelStatusAndSortIsNotNullOrderBySortDesc(String storeId, boolean delStatus);

    @Query(value = "SELECT DISTINCT(storeId) from project WHERE delStatus = FALSE", nativeQuery = true)
    Set<String> findDistinctStoreIdByDelStatus();

    List<Project> findByStoreIdAndDelStatusOrderByCreateTimeDesc(String storeId, boolean delStatus);

    List<Project> findByDelStatusAndComment(boolean delStatus, String comment);

}
