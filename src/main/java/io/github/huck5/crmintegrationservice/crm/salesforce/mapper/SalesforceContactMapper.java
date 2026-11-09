package io.github.huck5.crmintegrationservice.crm.salesforce.mapper;

import io.github.huck5.crmintegrationservice.crm.ContactDto;
import io.github.huck5.crmintegrationservice.crm.CrmSource;
import io.github.huck5.crmintegrationservice.crm.salesforce.model.SalesforceContact;
import io.github.huck5.crmintegrationservice.crm.salesforce.response.SalesforceQueryResponse;

import java.util.ArrayList;
import java.util.List;

public class SalesforceContactMapper {
  private SalesforceContactMapper() {
  }

  public static List<ContactDto> map(SalesforceQueryResponse<SalesforceContact> response) {
    List<ContactDto> contacts = new ArrayList<>();

    response.getRecords().forEach(contact -> {
      ContactDto contactDTO = ContactDto.builder()
                                        .source(CrmSource.SALESFORCE)
                                        .externalId(contact.getId())
                                        .firstName(contact.getFirstName())
                                        .lastName(contact.getLastName())
                                        .email(contact.getEmail())
                                        .build();
      contacts.add(contactDTO);
    });

    return contacts;
  }
}
