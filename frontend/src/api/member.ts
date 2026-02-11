import { http } from './http';
import type { ApiResult, PageResponse } from '@/types/api';
import type { MemberItem } from '@/types/domain';

export async function fetchMembers(page = 0, size = 10): Promise<PageResponse<MemberItem>> {
  const { data } = await http.get<ApiResult<PageResponse<MemberItem>>>('/api/members', { params: { page, size } });
  return data.data;
}

export async function createMember(payload: {
  name: string;
  bodyData?: string;
  photoUrl?: string;
  styleTags?: string;
}): Promise<MemberItem> {
  const { data } = await http.post<ApiResult<MemberItem>>('/api/members', payload);
  return data.data;
}

export async function updateMember(
  memberId: number,
  payload: { name?: string; bodyData?: string; photoUrl?: string; styleTags?: string }
): Promise<MemberItem> {
  const { data } = await http.put<ApiResult<MemberItem>>(`/api/members/${memberId}`, payload);
  return data.data;
}

export async function deleteMember(memberId: number): Promise<void> {
  await http.delete(`/api/members/${memberId}`);
}
