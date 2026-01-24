package ch.dboeckli.soap.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@Slf4j
public class SpringSoapServiceApplication {

    static void main(String[] args) {
        log.info("Starting Spring 6 Template Application...");
        SpringApplication.run(SpringSoapServiceApplication.class, args);
    }
}
