package io.github.huck5.crmintegrationservice.crm.salesforce;

import io.github.huck5.crmintegrationservice.crm.response.ContactRetrievalResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;

@RestController
@RequestMapping("/salesforce")
public class SalesforceController {
  private final SalesforceConnector connector;

  public SalesforceController (SalesforceConnector connector) {
    this.connector = connector;
  }

  @GetMapping("/contacts")
  public ContactRetrievalResponse getContacts() throws URISyntaxException {
    return connector.getContacts();
  }
}
