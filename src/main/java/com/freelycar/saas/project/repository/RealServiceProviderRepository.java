package com.freelycar.saas.project.repository;

import com.freelycar.saas.project.entity.RealServiceProvider;
import com.freelycar.saas.project.entity.ServiceProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RealServiceProviderRepository extends JpaRepository<RealServiceProvider, String> {
    Optional<RealServiceProvider> findByName(String name);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "update realServiceProvider set delStatus = 1 where id in :ids", nativeQuery = true)
    int delById(Set<String> ids);

    Optional<RealServiceProvider> findByIdAndDelStatus(String id, boolean delStatus);

    Optional<RealServiceProvider> findByIdAndDelStatusAndServiceStatus(String id, boolean delStatus,boolean serviceStatus);

    Page<RealServiceProvider> findByDelStatusAndNameContainingOrderBySortAsc(boolean delStatus, String name, Pageable pageable);

    RealServiceProvider findTopByDelStatusAndSortIsNotNullOrderBySortDesc(boolean delStatus);

    List<RealServiceProvider> findByIdInAndDelStatus(Set<String> ids,boolean delStatus);
}
