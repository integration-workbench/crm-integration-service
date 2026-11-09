package io.github.huck5.crmintegrationservice.crm.salesforce;

import io.github.huck5.crmintegrationservice.crm.ContactDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;

@RestController
@RequestMapping("/salesforce")
public class SalesforceController {
  private final SalesforceConnector connector;

  public SalesforceController (SalesforceConnector connector) {
    this.connector = connector;
  }

  @GetMapping("/contacts")
  public List<ContactDto> getContacts() throws URISyntaxException {
    return connector.getContacts();
  }
}
