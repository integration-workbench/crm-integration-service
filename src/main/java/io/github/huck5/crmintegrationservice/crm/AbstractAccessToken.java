package io.github.huck5.crmintegrationservice.crm;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class AbstractAccessToken {
  @JsonProperty("access_token")
  private String accessToken;
  @JsonProperty("token_type")
  private String tokenType;

  public String getAccessToken() {
    return accessToken;
  }

  public String getTokenType() {
    return tokenType;
  }
}
