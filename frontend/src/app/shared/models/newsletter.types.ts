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
