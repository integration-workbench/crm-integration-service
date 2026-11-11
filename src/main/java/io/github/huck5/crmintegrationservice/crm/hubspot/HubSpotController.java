package io.github.huck5.crmintegrationservice.crm.hubspot;

import io.github.huck5.crmintegrationservice.crm.ContactDto;
import io.github.huck5.crmintegrationservice.crm.response.ContactRetrievalResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;

@RestController
@RequestMapping("/hubspot")
public class HubSpotController {
  private final HubSpotConnector connector;

  public HubSpotController(HubSpotConnector connector) {
    this.connector = connector;
  }

  @GetMapping("/contacts")
  public ContactRetrievalResponse getContacts() throws URISyntaxException {
    return connector.getContacts();
  }
}
