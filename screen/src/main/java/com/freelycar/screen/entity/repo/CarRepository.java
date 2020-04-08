package com.freelycar.screen.entity.repo;

import com.freelycar.screen.entity.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author pyt
 * @date 2020/4/8 14:45
 * @email 2630451673@qq.com
 * @desc
 */
@Repository
public interface CarRepository extends JpaRepository<Car, String> {
    List<Car> findByDelStatus(boolean delStatus);
}
