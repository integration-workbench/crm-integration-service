package io.github.huck5.crmintegrationservice.crm.salesforce.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SalesforceQueryResponse<T> implements SalesforceResponse {
  private Long totalSize;
  private boolean done;
  private List<T> records;
  private String nextRecordsUrl;

  public SalesforceQueryResponse(Long totalSize, boolean done, List<T> records, String nextRecordsUrl) {
    this.totalSize = totalSize;
    this.done = done;
    this.records = records;
    this.nextRecordsUrl = nextRecordsUrl;
  }

  public Long getTotalSize() {
    return totalSize;
  }

  public boolean isDone() {
    return done;
  }

  public List<T> getRecords() {
    return records;
  }

  public String getNextRecordsUrl() {
    return nextRecordsUrl;
  }
}
