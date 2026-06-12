package com.notify.newsletter.application.port.in;

import com.notify.newsletter.application.dto.ConfirmationResponse;
import com.notify.newsletter.application.dto.ConfirmSubscriptionRequest;
import com.notify.newsletter.application.dto.NewsletterProfileResponse;
import com.notify.newsletter.application.dto.SubscribeRequest;
import com.notify.newsletter.application.dto.SubscribeResponse;
import com.notify.newsletter.application.dto.SubscriberResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NewsletterUseCase {

    NewsletterProfileResponse getNewsletterInfo(String slug);

    SubscribeResponse subscribe(SubscribeRequest request);

    ConfirmationResponse confirm(ConfirmSubscriptionRequest request);

    SubscribeResponse resendConfirmation(SubscribeRequest request);

    Page<SubscriberResponse> listSubscribers(String slug, Long senderId, Pageable pageable);
}
