package vn.nhom15.preorder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PreorderBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PreorderBackendApplication.class, args);
    }
}
