package com.freelycar.saas.project.repository;

import com.freelycar.saas.project.entity.Agent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface AgentRepository extends JpaRepository<Agent, String> {

    Page<Agent> findAllByDelStatusAndAgentNameContainingAndLinkManContaining(Boolean delStatus, String agentName, String linkMan, Pageable pageable);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "update agent set delStatus = 1 where id=:id", nativeQuery = true)
    int delById(String id);
}
