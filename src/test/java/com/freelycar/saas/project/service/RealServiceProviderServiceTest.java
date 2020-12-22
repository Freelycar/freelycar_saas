package com.freelycar.saas.project.service;

import com.freelycar.saas.BootApplication;
import com.freelycar.saas.wechat.model.BaseOrderInfo;
import junit.framework.TestCase;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.Transformers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author: pyt
 * @Date: 2020/12/18 11:18
 * @Description:
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BootApplication.class)
public class RealServiceProviderServiceTest extends TestCase {
    @Autowired
    private LocalContainerEntityManagerFactoryBean entityManagerFactory;

    /*@Test
    public void testModify() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT `comment`,`name`,price,storeId FROM project WHERE projectTypeId IN\n" +
                "(SELECT p.id FROM `projecttype` p WHERE p.name = '4S店' \n" +
                "AND p.storeId IN ( SELECT id FROM store WHERE delStatus = FALSE)) AND delStatus = FALSE");

        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql.toString());
        @SuppressWarnings({"unused", "unchecked"})
        List<Object[]> resultList = nativeQuery.getResultList();
        //关闭entityManagerFactory
        em.close();
        Set<String> serviceProviderNames = new HashSet<>();

    }*/
}