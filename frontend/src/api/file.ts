import { http } from './http';
import type { ApiResult } from '@/types/api';
import type { UploadFileResponse } from '@/types/domain';

export async function uploadFile(file: File, bizType: 'member' | 'clothing'): Promise<UploadFileResponse> {
  const formData = new FormData();
  formData.append('file', file);

  const { data } = await http.post<ApiResult<UploadFileResponse>>('/api/files/upload', formData, {
    params: { bizType },
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return data.data;
}

