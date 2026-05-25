package com.hse.adminservice;

import com.hse.adminservice.testinfra.AdminServiceTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(AdminServiceTestConfiguration.class)
class AdminServiceApplicationTests {

    @Test
    void contextLoadsWithTestProfile() {
    }
}
