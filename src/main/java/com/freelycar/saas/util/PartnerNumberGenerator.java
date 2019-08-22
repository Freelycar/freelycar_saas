package com.freelycar.saas.util;

import com.freelycar.saas.project.entity.PartnerNumber;
import com.freelycar.saas.project.repository.PartnerNumberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author tangwei - Toby
 * @date 2019/8/22
 * @email toby911115@gmail.com
 */
@Component
public class PartnerNumberGenerator implements ApplicationRunner, DisposableBean {
    public static Long partnerCount = 0L;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PartnerNumberRepository partnerNumberRepository;

    /**
     * 10分钟的定时器（600000毫秒）
     */
    @Scheduled(fixedRate = 600000)
    public void reportCurrentTime() {
        partnerCount++;
    }

    @Override
    public void destroy() throws Exception {
        logger.info("-----Spring容器即将关闭，将合作伙伴数据存入数据库中，当前合作伙伴数为：" + partnerCount + "------");
        PartnerNumber partnerNumber = partnerNumberRepository.findById(1).orElse(null);
        if (null != partnerNumber) {
            partnerNumber.setNum(partnerCount);
        } else {
            partnerNumber = new PartnerNumber(1, partnerCount);
        }
        PartnerNumber res = partnerNumberRepository.save(partnerNumber);
        logger.info("----存储合作伙伴数据成功：结果为：" + res + "----");
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("--------初始化合作伙伴计数器---------");
        PartnerNumber partnerNumber = partnerNumberRepository.findById(1).orElse(null);
        if (null != partnerNumber) {
            partnerCount = partnerNumber.getNum();
        } else {
            partnerCount = 0L;
            partnerNumber = new PartnerNumber(1, partnerCount);
            PartnerNumber res = partnerNumberRepository.save(partnerNumber);
            logger.info("----第一次运行，初始化数据库中的计数器数据，结果为：" + res + "----");
        }
        logger.info("--------初始化完毕，从数据库中得到合作伙伴数为：" + partnerCount + "---------");
    }
}
