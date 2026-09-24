import { apiRequest } from '../../../shared/api/client';

export type ContentType = 'description' | 'comment' | 'resolution';

export interface Source {
  ticketId: string;
  displayId: string;
  contentTypes: ContentType[];
}

export interface AskRequest {
  question: string;
}

export interface AskResponse {
  answer: string;
  sources: Source[];
}

export interface IndexDocumentStatus {
  contentType: ContentType;
  indexedAt: string | null;
  textHash: string;
}

export interface IndexStatusResponse {
  ticketId: string;
  documents: IndexDocumentStatus[];
  lastIndexedAt: string | null;
  pendingJobs?: number;
}

export function askQuestion(request: AskRequest): Promise<AskResponse> {
  return apiRequest<AskResponse>('/assistant/ask', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function getIndexStatus(ticketId: string): Promise<IndexStatusResponse> {
  return apiRequest<IndexStatusResponse>(`/assistant/index-status/${ticketId}`);
}
