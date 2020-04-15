package com.freelycar.saas;

import com.freelycar.saas.screen.websocket.server.ScreenWebsocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

/**
 * @author toby
 */
@SpringBootApplication(exclude = {org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration.class})
@EnableCaching
@EnableScheduling
public class BootApplication extends SpringBootServletInitializer {

    private static Logger logger = LoggerFactory.getLogger(BootApplication.class);

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(BootApplication.class, args);

        logger.debug("通过SpringBoot来注入依赖:");

        String[] beanNames = ctx.getBeanDefinitionNames();
        Arrays.sort(beanNames);
        for (String beanName : beanNames) {
            logger.debug(beanName);
        }
        logger.info("FreelyCar-SaaS服务启动完成。");
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(BootApplication.class);
    }


//    @EnableAsync
//    @Configuration
//    class TaskPoolConfig {
//
//        @Bean("taskExecutor")
//        public Executor taskExecutor() {
//            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//            executor.setCorePoolSize(10);
//            executor.setMaxPoolSize(20);
//            executor.setQueueCapacity(10);
//            executor.setKeepAliveSeconds(60);
//            executor.setThreadNamePrefix("taskThead-");
//            executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
//            return executor;
//        }
//    }
}
