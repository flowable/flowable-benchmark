package org.flowable.benchmark.app;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author Filip Hrisafov
 */
@SpringBootApplication
@EnableConfigurationProperties({
    BenchmarkProperties.class
})
public class BenchmarkApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(BenchmarkApplication.class)
            .web(WebApplicationType.NONE)
            .run(args)
            .close();
    }
}
