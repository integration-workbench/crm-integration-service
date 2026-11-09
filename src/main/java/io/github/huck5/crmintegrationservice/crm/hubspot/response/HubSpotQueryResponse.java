package io.github.huck5.crmintegrationservice.crm.hubspot.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class HubSpotQueryResponse<T> implements HubSpotResponse {
  private List<T> results;

  public List<T> getResults() {
    return results;
  }
}
