package com.freelycar.saas.project.repository;

import com.freelycar.saas.project.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.QueryHint;
import java.util.List;
import java.util.Set;

import static org.hibernate.jpa.QueryHints.HINT_COMMENT;

/**
 * @author tangwei - Toby
 * @date 2018/10/17
 * @email toby911115@gmail.com
 */
public interface StoreRepository extends JpaRepository<Store, String> {
    List<Store> findStoreByDelStatusAndNameContainingOrderBySortAsc(boolean delStatus, String name);

    List<Store> findStoreByDelStatusAndIdIn(boolean delStatus, Set<String> ids);

    Page<Store> findStoreByDelStatusAndNameContainingOrderBySortAsc(boolean delStatus, String name, Pageable pageable);

    @QueryHints(value = {@QueryHint(name = HINT_COMMENT, value = "a query for pageable")})
    @Query("select u from Store u where u.delStatus = ?1 and u.name like CONCAT('%',?2,'%')  and u.propertyCompany like CONCAT('%',?3,'%') ")
    Page<Store> findStoreByDelStatusAndNameContainingAndPropertyCompanyContainingOrderBySortAsc(boolean delStatus, String name, String propertyCompany, Pageable pageable);

    @QueryHints(value = {@QueryHint(name = HINT_COMMENT, value = "a query for pageable")})
    @Query("select u from Store u where u.delStatus = ?1 and u.name like CONCAT('%',?2,'%')  and (u.propertyCompany like CONCAT('%','','%') or u.propertyCompany is null)")
    Page<Store> findStoreByDelStatusAndNameContainingAndPropertyCompanyContainingOrderBySortAsc(boolean delStatus, String name, Pageable pageable);


    List<Store> findAllByDelStatusOrderBySortAsc(boolean delStatus);

    @Query(value = "select id FROM store where delStatus = :delStatus", nativeQuery = true)
    Set<String> findIdByDelStatus(@Param("delStatus") Boolean delStatus);

    Store findTopByDelStatusAndSortIsNotNullOrderBySortDesc(boolean delStatus);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "update store set delStatus = 1 where id=:id", nativeQuery = true)
    int delById(String id);

    List<Store> findByDelStatusAndIdIn(boolean delStatus, List<String> id);

    Store findByNameAndDelStatus(String name, boolean delStatus);

    List<Store> findByNameContainingAndDelStatus(String name, boolean delStatus);
}
