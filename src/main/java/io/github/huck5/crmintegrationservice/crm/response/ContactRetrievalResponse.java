package io.github.huck5.crmintegrationservice.crm.response;

import io.github.huck5.crmintegrationservice.crm.ContactDto;

import java.util.List;

public record ContactRetrievalResponse(List<ContactDto> contacts, Long totalAvailable, boolean hasMore, String continuationToken) {
}
