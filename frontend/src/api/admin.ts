export type ExcerptStatus = 'ACTIVE' | 'DRAFT' | 'REJECTED' | 'DELETED';

export interface DraftExcerpt {
  excerptId: number;
  composerId: number;
  composerName: string;
  workId: number | null;
  uploadedByUserId: number;
  uploaderUsername: string;
  name: string;
  audioUrl: string;
  compositionYear: number | null;
  description: string | null;
  dateUploaded: string | null;
  timesUsed: number;
  status: ExcerptStatus;
}

export interface ApproveExcerptRequest {
  composerId: number;
  workId: number | null;
  name: string;
  compositionYear: number | null;
  description: string;
}

export interface ExcerptsPage {
  content: DraftExcerpt[];
  totalElements: number;
  totalPages: number;
  number: number;   // current page (0-indexed)
  first: boolean;
  last: boolean;
}

export type SortOption = 'timesUsed_asc' | 'timesUsed_desc' | 'dateUploaded_asc' | 'dateUploaded_desc';

export async function getExcerpts(
  token: string,
  statuses: ExcerptStatus[] = ['DRAFT'],
  composerId: number | null = null,
  sort: SortOption = 'timesUsed_asc',
  page = 0,
  size = 10,
): Promise<ExcerptsPage> {
  const params = new URLSearchParams();
  statuses.forEach(s => params.append('status', s));
  if (composerId != null) params.set('composerId', String(composerId));
  params.set('sort', sort);
  params.set('page', String(page));
  params.set('size', String(size));
  const res = await fetch(`/api/admin/excerpts?${params}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error('Failed to fetch excerpts');
  return res.json();
}

export interface DailyChallengeEntry {
  excerptId: number;
  excerptName: string;
  composerName: string;
  challengeNumber: number;
}

export interface DailyChallengesResponse {
  today: DailyChallengeEntry | null;
  tomorrow: DailyChallengeEntry | null;
}

export async function getDailyChallenges(token: string): Promise<DailyChallengesResponse> {
  const res = await fetch('/api/admin/daily-challenges', {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error('Failed to fetch daily challenges');
  return res.json();
}

export async function scheduleTomorrow(id: number, token: string): Promise<void> {
  const res = await fetch(`/api/admin/excerpts/${id}/schedule-tomorrow`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error((body as { error?: string }).error ?? 'Failed to schedule');
  }
}

export async function updateExcerptStatus(id: number, status: ExcerptStatus, token: string): Promise<void> {
  const res = await fetch(`/api/admin/excerpts/${id}/status`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ status }),
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error((body as { error?: string }).error ?? 'Failed to update status');
  }
}

export async function approveExcerpt(id: number, data: ApproveExcerptRequest, token: string): Promise<void> {
  const res = await fetch(`/api/admin/excerpts/${id}/approve`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error((body as { error?: string }).error ?? 'Failed to approve excerpt');
  }
}
