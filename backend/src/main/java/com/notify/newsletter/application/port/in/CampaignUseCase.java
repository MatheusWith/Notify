package com.notify.newsletter.application.port.in;

import com.notify.newsletter.application.dto.CampaignResponse;
import com.notify.newsletter.application.dto.CampaignStatusRequest;
import com.notify.newsletter.application.dto.CreateCampaignRequest;
import com.notify.newsletter.application.dto.UpdateCampaignRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CampaignUseCase {

    CampaignResponse create(String slug, Long senderId, CreateCampaignRequest request);

    CampaignResponse getById(String slug, UUID campaignId, Long senderId);

    Page<CampaignResponse> list(String slug, Long senderId, Pageable pageable);

    CampaignResponse update(String slug, UUID campaignId, Long senderId, UpdateCampaignRequest request);

    void delete(String slug, UUID campaignId, Long senderId);

    CampaignResponse updateStatus(String slug, UUID campaignId, Long senderId, CampaignStatusRequest request);
}
