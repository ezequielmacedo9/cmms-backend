package br.com.cmms.cmms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Collections;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableAsync
public class CmmsApplication {

    private static final Logger log = LoggerFactory.getLogger(CmmsApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CmmsApplication.class);
        String port = System.getenv("PORT");
        if (port != null) {
            app.setDefaultProperties(Collections.singletonMap("server.port", port));
        }
        app.run(args);
    }
}
