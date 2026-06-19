package com.notify.newsletter.interfaces.rest;

import com.notify.identity.infrastructure.security.UserPrincipal;
import com.notify.identity.interfaces.resolver.CurrentUser;
import com.notify.newsletter.application.dto.*;
import com.notify.newsletter.application.port.in.NewsletterUseCase;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterUseCase newsletterService;

    @GetMapping("/{slug}")
    public ResponseEntity<NewsletterProfileResponse> getNewsletterInfo(@PathVariable String slug) {
        NewsletterProfileResponse response = newsletterService.getNewsletterInfo(slug);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<NewsletterSummaryResponse>> getMyNewsletters(@CurrentUser UserPrincipal currentUser) {
        List<NewsletterSummaryResponse> response = newsletterService.getMyNewsletters(currentUser.userId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/subscribe")
    public ResponseEntity<SubscribeResponse> subscribe(@Valid @RequestBody SubscribeRequest request) {
        SubscribeResponse response = newsletterService.subscribe(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<ConfirmationResponse> confirm(@Valid @RequestBody ConfirmSubscriptionRequest request) {
        ConfirmationResponse response = newsletterService.confirm(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend")
    public ResponseEntity<SubscribeResponse> resend(@Valid @RequestBody SubscribeRequest request) {
        SubscribeResponse response = newsletterService.resendConfirmation(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{slug}/subscribers")
    public ResponseEntity<Page<SubscriberResponse>> listSubscribers(@PathVariable String slug,
            @CurrentUser UserPrincipal currentUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<SubscriberResponse> response = newsletterService.listSubscribers(slug, currentUser.userId(), pageable);
        return ResponseEntity.ok(response);
    }
}
