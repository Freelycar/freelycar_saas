package com.freelycar.saas.project.repository;

import com.freelycar.saas.project.entity.RSPProject;
import com.freelycar.saas.project.entity.RealServiceProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Ting
 * Date: 2020-09-10
 * Time: 15:49
 */
public interface RSPProjectRepository extends JpaRepository<RSPProject, String> {
    Optional<RSPProject> findByName(String name);

    List<RSPProject> findByNameAndRspIdAndDelStatus(String name, String rspId, boolean delStatus);

    List<RSPProject> findByRspIdInAndDelStatus(List<String> rspIds, boolean delStatus);

    Optional<RSPProject> findByIdAndDelStatus(String id, boolean delStatus);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "update RSPProject set delStatus = 1 where id in :ids", nativeQuery = true)
    int delById(Set<String> ids);

    Page<RSPProject> findByDelStatusAndNameContainingAndRspIdOrderByIdAsc(boolean delStatus, String name, String rspId, Pageable pageable);
}
