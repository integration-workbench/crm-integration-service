package io.github.huck5.crmintegrationservice.crm.hubspot;

import io.github.huck5.crmintegrationservice.crm.AbstractAccessToken;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class HubSpotAccessToken extends AbstractAccessToken {
  @JsonProperty("refresh_token")
  private String refreshToken;

  @JsonProperty("hub_id")
  private String hubId;

  @JsonProperty private List<String> scopes;

  @JsonProperty("token_use")
  private String tokenUse;

  @JsonProperty("expires_in")
  private int expiresIn;

  public String getRefreshToken() {
    return refreshToken;
  }

  public int getExpiresIn() {
    return expiresIn;
  }
}
