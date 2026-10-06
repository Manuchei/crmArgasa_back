package com.empresa.crm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.empresa.crm.services.WebPushService;

@SpringBootTest
@ActiveProfiles("test")

class ApplicationTests {
	
	@MockitoBean
	private WebPushService webPushService;

	@Test
	void contextLoads() {
	}

}
