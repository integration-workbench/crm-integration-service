package io.github.huck5.crmintegrationservice.crm.salesforce.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SalesforceContact {
  @JsonProperty
  private SalesforceAttribute attributes;
  @JsonProperty("Id")
  private String id;
  @JsonProperty("FirstName")
  private String firstName;
  @JsonProperty("LastName")
  private String lastName;
  @JsonProperty("Email")
  private String email;

  public String getId() {
    return id;
  }

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
