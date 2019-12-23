package com.freelycar.saas.project.repository;

import com.freelycar.saas.project.entity.ServiceProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.Max;
import java.util.List;

/**
 * @author puyuting
 * @date 2019/12/23
 * @email 2630451673@qq.com
 */
public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, String> {
    List<ServiceProvider> findByNameAndDelStatus(String name, boolean delStatus);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "update serviceProvider set delStatus = 1 where id=:id", nativeQuery = true)
    int delById(@Param("id") String id);

    List<ServiceProvider> findByDelStatusAndNameContainingOrderByIdAsc(boolean delStatus, String name);

    Page<ServiceProvider> findByDelStatusAndNameContainingOrderByIdAsc(boolean delStatus, String name, Pageable pageable);
}
