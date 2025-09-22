package com.nempeth.korven;

import com.nempeth.korven.config.TestMailConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.Test;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestMailConfiguration.class)
class KorvenApplicationTests {

	@Test
	void contextLoads() {
	}

}
