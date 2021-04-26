package com.freelycar.saas.project.service;

import com.freelycar.saas.BootApplication;
import com.freelycar.saas.project.repository.ReminderRepository;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * @Author: pyt
 * @Date: 2021/4/21 16:56
 * @Description:
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BootApplication.class)
public class ReminderServiceTest {

    @Autowired
    private ReminderRepository reminderRepository;



}