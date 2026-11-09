package io.github.huck5.crmintegrationservice.crm;


import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.Objects;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ContactDto {
  private final CrmSource source;
  private final String externalId;
  private final String firstName;
  private final String lastName;
  private final String email;

  private ContactDto(Builder builder) {
    this.source = builder.source;
    this.externalId = builder.externalId;
    this.firstName = builder.firstName;
    this.lastName = builder.lastName;
    this.email = builder.email;
  }

  public String getEmail() {
    return email;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ContactDto that = (ContactDto) o;
    return source == that.source && Objects.equals(externalId, that.externalId) && Objects.equals(
      firstName, that.firstName) && Objects.equals(lastName, that.lastName) && Objects.equals(
      email, that.email);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, externalId, firstName, lastName, email);
  }

  @Override
  public String toString() {
    return "ContactDTO{" +
      "source=" + source +
      ", externalId='" + externalId + '\'' +
      ", firstName='" + firstName + '\'' +
      ", lastName='" + lastName + '\'' +
      ", email='" + email + '\'' +
      '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private CrmSource source;
    private String externalId;
    private String firstName;
    private String lastName;
    private String email;

    public Builder source(CrmSource source) {
      this.source = source;
      return this;
    }

    public Builder externalId(String externalId) {
      this.externalId = externalId;
      return this;
    }

    public Builder firstName(String firstName) {
      this.firstName = firstName;
      return this;
    }

    public Builder lastName(String lastName) {
      this.lastName = lastName;
      return this;
    }

    public Builder email(String email) {
      this.email = email;
      return this;
    }

    public ContactDto build() {
      return new ContactDto(this);
    }
  }
}
