package com.vendorsphere;

import com.vendorsphere.auth.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class VendorSphereApplication {

    public static void main(String[] args) {
        SpringApplication.run(VendorSphereApplication.class, args);
    }
}
