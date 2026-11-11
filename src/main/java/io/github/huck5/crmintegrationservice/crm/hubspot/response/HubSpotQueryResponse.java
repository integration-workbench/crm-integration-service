package io.github.huck5.crmintegrationservice.crm.hubspot.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class HubSpotQueryResponse<T> implements HubSpotResponse {
  private List<T> results;
  private Paging paging;
  private Next next;

  public List<T> getResults() {
    return results;
  }

  public Paging getPaging() {
    return paging;
  }

  public record Paging(Next next) {
  }

  public record Next(String after, String link) {
  }
}
