package io.github.huck5.crmintegrationservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
  "HS_CLIENT_ID=fake_hubspot_id",
  "HS_CLIENT_SECRET=fake_hubspot_secret",
  "HS_REFRESH_TOKEN=fake_hubspot_refresh_token"
})
class CrmIntegrationServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
