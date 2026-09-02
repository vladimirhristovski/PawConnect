export type ApplicationStatusCode =
  'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN' | 'CLOSED';

export type ApplicationDecision = 'APPROVE' | 'REJECT';

export interface Application {
  id: number;
  listingId: number;
  petName: string;
  applicantId: number;
  applicantUsername: string;
  statusCode: ApplicationStatusCode;
  statusName: string;
  message: string | null;
  contactPhone: string | null;
  contactEmail: string | null;
  reviewedBy: string | null;
  reviewedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateApplicationRequest {
  message?: string;
  contactPhone?: string;
  contactEmail?: string;
}
