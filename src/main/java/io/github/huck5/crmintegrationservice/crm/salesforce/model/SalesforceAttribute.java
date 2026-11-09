package io.github.huck5.crmintegrationservice.crm.salesforce.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SalesforceAttribute {
  private String type;
  private String url;
}
