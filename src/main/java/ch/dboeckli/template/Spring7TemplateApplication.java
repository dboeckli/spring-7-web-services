package ch.dboeckli.template;
// TODOS: RENAME PACKAGE

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@Slf4j
// TODOS: RENAME ME
public class Spring7TemplateApplication {

    public static void main(String[] args) {
        log.info("Starting Spring 6 Template Application...");
        SpringApplication.run(Spring7TemplateApplication.class, args);
    }

    @RequestMapping(path = "/hello", produces = "application/json")
    String home() {
        log.info("home() has been called");
        return "{\"message\":\"Hello World!\"}";
    }
}
