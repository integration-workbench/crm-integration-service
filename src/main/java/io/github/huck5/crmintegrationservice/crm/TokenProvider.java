package io.github.huck5.crmintegrationservice.crm;

public interface TokenProvider {
  AbstractAccessToken getToken();
  AbstractAccessToken refreshToken();
}
