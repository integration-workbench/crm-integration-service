package io.github.huck5.crmintegrationservice.crm.hubspot.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class HubSpotProperties {
  @JsonProperty("createdate")
  private Instant createDate;
  @JsonProperty
  private String email;
  @JsonProperty("firstname")
  private String firstName;
  @JsonProperty("lastname")
  private String lastName;
  @JsonProperty("lastmodifieddate")
  private Instant lastModifiedDate;

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getEmail() {
    return email;
  }
}
