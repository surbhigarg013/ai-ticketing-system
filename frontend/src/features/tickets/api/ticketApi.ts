import { apiRequest } from '../../../shared/api/client';

export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'CANCELLED';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH';
export type Category = 'PAYMENT' | 'SHIPMENT' | 'ACCOUNT' | 'GENERAL';

export interface Comment {
  id: string;
  content: string;
  author: string;
  createdAt: string;
}

export interface TicketSummary {
  id: string;
  displayId: string;
  title: string;
  status: TicketStatus;
  priority: Priority;
  category: Category;
  assignee: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TicketDetail extends TicketSummary {
  description: string;
  resolution: string | null;
  comments: Comment[];
}

export interface TicketPage {
  items: TicketSummary[];
  page: number;
  size: number;
  total: number;
}

export interface CreateTicketRequest {
  title: string;
  description: string;
  priority: Priority;
  assignee?: string;
  category?: Category;
}

export interface UpdateTicketRequest {
  title?: string;
  description?: string;
  priority?: Priority;
  assignee?: string | null;
  category?: Category;
}

export interface CreateCommentRequest {
  content: string;
  author: string;
}

export interface StatusTransitionRequest {
  status: TicketStatus;
  resolution?: string;
}

export interface ListTicketsParams {
  q?: string;
  status?: TicketStatus;
  page?: number;
  size?: number;
}

function buildQuery(params: ListTicketsParams): string {
  const search = new URLSearchParams();
  if (params.q) search.set('q', params.q);
  if (params.status) search.set('status', params.status);
  if (params.page !== undefined) search.set('page', String(params.page));
  if (params.size !== undefined) search.set('size', String(params.size));
  const query = search.toString();
  return query ? `?${query}` : '';
}

export function listTickets(params: ListTicketsParams = {}): Promise<TicketPage> {
  return apiRequest<TicketPage>(`/tickets${buildQuery(params)}`);
}

export function getTicket(ticketId: string): Promise<TicketDetail> {
  return apiRequest<TicketDetail>(`/tickets/${ticketId}`);
}

export function createTicket(request: CreateTicketRequest): Promise<TicketDetail> {
  return apiRequest<TicketDetail>('/tickets', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function updateTicket(
  ticketId: string,
  request: UpdateTicketRequest,
): Promise<TicketDetail> {
  return apiRequest<TicketDetail>(`/tickets/${ticketId}`, {
    method: 'PATCH',
    body: JSON.stringify(request),
  });
}

export function addComment(
  ticketId: string,
  request: CreateCommentRequest,
): Promise<Comment> {
  return apiRequest<Comment>(`/tickets/${ticketId}/comments`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function transitionStatus(
  ticketId: string,
  request: StatusTransitionRequest,
): Promise<TicketDetail> {
  return apiRequest<TicketDetail>(`/tickets/${ticketId}/status`, {
    method: 'PATCH',
    body: JSON.stringify(request),
  });
}

export const VALID_TRANSITIONS: Record<TicketStatus, TicketStatus[]> = {
  OPEN: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['RESOLVED', 'CANCELLED'],
  RESOLVED: ['CLOSED'],
  CLOSED: [],
  CANCELLED: [],
};
