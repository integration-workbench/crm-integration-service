package io.github.huck5.crmintegrationservice.crm.salesforce;

import io.github.huck5.crmintegrationservice.crm.AbstractAccessToken;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SalesforceAccessToken extends AbstractAccessToken {
  @JsonProperty
  private String scope;
  @JsonProperty("instance_url")
  private String instanceUrl;
  @JsonProperty
  private String id;

  public String getInstanceUrl() {
    return instanceUrl;
  }
}
