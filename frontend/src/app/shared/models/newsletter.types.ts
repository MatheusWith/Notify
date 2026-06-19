export interface NewsletterSummary {
  id: string;
  name: string;
  slug: string;
  subscriberCount: number;
}

export interface NewsletterProfile {
  id: string;
  name: string;
  slug: string;
  description: string;
  subscriberCount: number;
}

export interface SubscribeRequest {
  email: string;
  slug?: string;
}

export interface SubscribeResponse {
  id: string;
  email: string;
  status: string;
  expiresAt: string;
}

export interface SubscriberResponse {
  name: string;
  status: string;
  createdAt: string;
}

export interface Campaign {
  id: string;
  newsletterId: string;
  subject: string;
  content: string;
  status: 'DRAFT' | 'PENDING' | 'PUBLISHED' | 'SENT';
  scheduledAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCampaignRequest {
  subject: string;
  content: string;
  scheduledAt?: string;
}

export interface UpdateCampaignRequest {
  subject: string;
  content: string;
  scheduledAt?: string;
}

export interface CampaignStatusRequest {
  status: 'PUBLISHED' | 'PENDING';
}
