package com.freelycar.saas.project.repository;

import com.freelycar.saas.project.entity.RealServiceProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RealServiceProviderRepository extends JpaRepository<RealServiceProvider, String> {
    Optional<RealServiceProvider> findByName(String name);
}
