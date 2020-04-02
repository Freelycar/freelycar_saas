package com.freelycar.screen.service;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author pyt
 * @date 2020/4/2 17:04
 * @email 2630451673@qq.com
 * @desc
 */
@SpringBootTest
@RunWith(SpringRunner.class)
class ConsumerProjectServiceTest {
    @Autowired
    private ConsumerProjectService consumerProjectService;

    @Test
    void genProjectRanking() {
        System.out.println(consumerProjectService.genProjectRanking());
    }
}