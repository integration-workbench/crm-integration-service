package io.github.huck5.crmintegrationservice.crm.hubspot.mapper;

import io.github.huck5.crmintegrationservice.crm.ContactDto;
import io.github.huck5.crmintegrationservice.crm.CrmSource;
import io.github.huck5.crmintegrationservice.crm.hubspot.model.HubSpotContact;
import io.github.huck5.crmintegrationservice.crm.hubspot.response.HubSpotQueryResponse;
import io.github.huck5.crmintegrationservice.crm.response.ContactRetrievalResponse;

import java.util.ArrayList;
import java.util.List;

public class HubSpotContactMapper {
  private HubSpotContactMapper() {
  }

  public static ContactRetrievalResponse map(HubSpotQueryResponse<HubSpotContact> response) {
    List<ContactDto> contacts = new ArrayList<>();

    response.getResults().forEach(contact -> {
      ContactDto contactDTO = ContactDto.builder()
                                        .source(CrmSource.HUBSPOT)
                                        .externalId(contact.getId())
                                        .firstName(contact.getProperties().getFirstName())
                                        .lastName(contact.getProperties().getLastName())
                                        .email(contact.getProperties().getEmail())
                                        .build();
      contacts.add(contactDTO);
    });

    HubSpotQueryResponse.Paging paging = response.getPaging();
    HubSpotQueryResponse.Next next = paging == null ? null : paging.next();

    boolean hasMore = next != null;
    String continuationToken = hasMore ? next.after() : null;


    return new ContactRetrievalResponse(contacts, null, hasMore, continuationToken);
  }
}
