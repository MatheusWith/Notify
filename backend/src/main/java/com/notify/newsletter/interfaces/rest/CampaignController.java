package com.notify.newsletter.interfaces.rest;

import com.notify.identity.infrastructure.security.UserPrincipal;
import com.notify.identity.interfaces.resolver.CurrentUser;
import com.notify.newsletter.application.dto.*;
import com.notify.newsletter.application.port.in.CampaignUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/newsletter/{slug}/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignUseCase campaignService;

    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(@PathVariable String slug,
            @Valid @RequestBody CreateCampaignRequest request, @CurrentUser UserPrincipal currentUser) {
        CampaignResponse response = campaignService.create(slug, currentUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<CampaignResponse>> listCampaigns(@PathVariable String slug,
            @CurrentUser UserPrincipal currentUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CampaignResponse> response = campaignService.list(slug, currentUser.userId(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{campaignId}")
    public ResponseEntity<CampaignResponse> getCampaign(@PathVariable String slug, @PathVariable UUID campaignId,
            @CurrentUser UserPrincipal currentUser) {
        CampaignResponse response = campaignService.getById(slug, campaignId, currentUser.userId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{campaignId}")
    public ResponseEntity<CampaignResponse> updateCampaign(@PathVariable String slug, @PathVariable UUID campaignId,
            @Valid @RequestBody UpdateCampaignRequest request, @CurrentUser UserPrincipal currentUser) {
        CampaignResponse response = campaignService.update(slug, campaignId, currentUser.userId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{campaignId}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable String slug, @PathVariable UUID campaignId,
            @CurrentUser UserPrincipal currentUser) {
        campaignService.delete(slug, campaignId, currentUser.userId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{campaignId}/status")
    public ResponseEntity<CampaignResponse> updateStatus(@PathVariable String slug, @PathVariable UUID campaignId,
            @Valid @RequestBody CampaignStatusRequest request, @CurrentUser UserPrincipal currentUser) {
        CampaignResponse response = campaignService.updateStatus(slug, campaignId, currentUser.userId(), request);
        return ResponseEntity.ok(response);
    }
}
