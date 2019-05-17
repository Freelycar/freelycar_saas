package com.freelycar.saas.menu;

import com.freelycar.saas.project.entity.WxUserInfo;
import com.freelycar.saas.project.service.WxUserInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author tangwei - Toby
 * @date 2019-05-16
 * @email toby911115@gmail.com
 */
@Component
public class MenuStartupRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(MenuStartupRunner.class);


    @Value("${router.init.enable}")
    private boolean enable;

    @Autowired
    private WxUserInfoService wxUserInfoService;

    @Override
    public void run(String... args) throws Exception {
        //TODO 清空菜单路由表，并重新生成数据
        if (enable) {
            logger.info("startup runner");
            WxUserInfo wxUserInfo = wxUserInfoService.findById("ea8ecbc5694d1d1d01694d2be8930000");
            System.out.println(wxUserInfo.getTrueName());
        }
    }
}
