package io.github.huck5.crmintegrationservice.crm.hubspot.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.time.Instant;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class HubSpotContact {
  private String id;
  private HubSpotProperties properties;
  private Instant createdAt;
  private Instant updatedAt;
  private boolean archived;
  private String url;

  public String getId() {
    return id;
  }

  public HubSpotProperties getProperties() {
    return properties;
  }
}
