import { fetchEventSource } from '@microsoft/fetch-event-source';
import { http, getAccessToken } from './http';
import type { ApiResult, PageResponse } from '@/types/api';
import type {
  CreateManualHistoryPayload,
  MatchHistoryItem,
  MatchTaskDetail,
  MatchTaskSummary,
  TaskStatus
} from '@/types/domain';

export interface CreateTaskResponse {
  taskId: string;
  status: TaskStatus;
}

export interface MatchHistoryResponse {
  records: MatchHistoryItem[];
  total: number;
}

export type TaskEventName = 'task_started' | 'task_progress' | 'task_completed' | 'task_failed';

export async function createMatchTask(payload: {
  memberId: number;
  clothingIds: number[];
  scene?: string;
}): Promise<CreateTaskResponse> {
  const { data } = await http.post<ApiResult<CreateTaskResponse>>('/api/match/tasks', payload);
  return data.data;
}

export async function fetchTask(taskId: string): Promise<MatchTaskDetail> {
  const { data } = await http.get<ApiResult<MatchTaskDetail>>(`/api/match/tasks/${taskId}`);
  return data.data;
}

export async function generateTaskOutfitPreview(taskId: string, outfitNo: number): Promise<MatchTaskDetail> {
  const { data } = await http.post<ApiResult<MatchTaskDetail>>(
    `/api/match/tasks/${taskId}/outfits/${outfitNo}/preview`
  );
  return data.data;
}

export async function fetchTaskList(memberId?: number, page = 0, size = 10): Promise<PageResponse<MatchTaskSummary>> {
  const params: Record<string, string | number> = { page, size };
  if (typeof memberId === 'number') {
    params.memberId = memberId;
  }
  const { data } = await http.get<ApiResult<PageResponse<MatchTaskSummary>>>('/api/match/tasks', { params });
  return data.data;
}

export async function fetchHistory(memberId: number, limit = 10): Promise<MatchHistoryResponse> {
  const { data } = await http.get<ApiResult<MatchHistoryResponse>>(`/api/members/${memberId}/history`, {
    params: { limit }
  });
  return data.data;
}

export async function createManualHistory(
  memberId: number,
  payload: CreateManualHistoryPayload
): Promise<MatchHistoryItem> {
  const { data } = await http.post<ApiResult<MatchHistoryItem>>(`/api/members/${memberId}/history/manual`, payload);
  return data.data;
}

export async function updateHistoryStatus(
  memberId: number,
  recordId: number,
  status: MatchHistoryItem['status']
): Promise<MatchHistoryItem> {
  const { data } = await http.patch<ApiResult<MatchHistoryItem>>(
    `/api/members/${memberId}/history/${recordId}/status`,
    { status }
  );
  return data.data;
}

export async function streamTaskEvents(
  taskId: string,
  handlers: {
    onEvent: (name: TaskEventName, data: unknown) => void;
    onError?: (error: Error) => void;
    signal?: AbortSignal;
  }
): Promise<void> {
  const token = getAccessToken();
  const baseUrl = import.meta.env.VITE_API_BASE_URL ?? '';

  await fetchEventSource(`${baseUrl}/api/match/tasks/${taskId}/events`, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: 'text/event-stream'
    },
    openWhenHidden: true,
    signal: handlers.signal,
    onmessage(event) {
      const name = event.event as TaskEventName;
      let parsed: unknown = event.data;
      try {
        parsed = JSON.parse(event.data);
      } catch (error) {
        // Keep raw string payload for non-JSON messages.
      }
      handlers.onEvent(name, parsed);
    },
    onerror(error) {
      handlers.onError?.(error as Error);
      throw error;
    }
  });
}
