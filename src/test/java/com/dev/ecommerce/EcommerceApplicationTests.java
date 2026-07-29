package com.dev.ecommerce;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIf("com.dev.ecommerce.support.DockerConditions#isAvailable")
class EcommerceApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}

}
