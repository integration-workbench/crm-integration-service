package io.github.huck5.crmintegrationservice.crm.hubspot.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HubSpotErrorResponse implements HubSpotResponse {
  @JsonProperty
  private String status;
  @JsonProperty
  private String message;
  @JsonProperty
  private String category;
  @JsonProperty
  private String correlationId;


  public String getCategory() {
    return category;
  }
}
