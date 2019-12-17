package com.freelycar.saas.project.repository;

import com.freelycar.saas.project.entity.WxCumulate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WxCumulateRepository extends JpaRepository<WxCumulate, Long> {

    WxCumulate findTopByRefDateOrderByCreateTimeDesc(String refDate);
}
