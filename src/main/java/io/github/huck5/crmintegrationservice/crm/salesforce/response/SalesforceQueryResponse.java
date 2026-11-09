package io.github.huck5.crmintegrationservice.crm.salesforce.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SalesforceQueryResponse<T> implements SalesforceResponse {
  private int totalSize;
  private boolean done;
  private List<T> records;
  private String nextRecordsUrl;

  public List<T> getRecords() {
    return records;
  }
}
