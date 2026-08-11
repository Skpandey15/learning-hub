package com.learninghub;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class LearningHubApplicationTest {
    @Test
    void delegatesStartupToSpringApplication() {
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            String[] args = {"--spring.main.web-application-type=none"};

            LearningHubApplication.main(args);

            spring.verify(() -> SpringApplication.run(LearningHubApplication.class, args));
        }
    }
}
