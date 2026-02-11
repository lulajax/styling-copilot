import { http } from './http';
import type { ApiResult, PageResponse } from '@/types/api';
import type { ClothingItem, ClothingType } from '@/types/domain';

interface ClothingPoolResponse {
  items: ClothingItem[];
  total: number;
}

export async function fetchOnShelfClothing(page = 0, size = 20): Promise<ClothingPoolResponse> {
  const { data } = await http.get<ApiResult<ClothingPoolResponse>>('/api/clothing', {
    params: { status: 'ON_SHELF', page, size }
  });
  return data.data;
}

export async function fetchAllClothing(page = 0, size = 10): Promise<PageResponse<ClothingItem>> {
  const { data } = await http.get<ApiResult<PageResponse<ClothingItem>>>('/api/clothing/all', { params: { page, size } });
  return data.data;
}

export async function createClothing(payload: {
  name: string;
  imageUrl?: string;
  styleTags?: string;
  clothingType: ClothingType;
  status?: 'ON_SHELF' | 'OFF_SHELF';
}): Promise<ClothingItem> {
  const { data } = await http.post<ApiResult<ClothingItem>>('/api/clothing', payload);
  return data.data;
}

export async function updateClothing(
  clothingId: number,
  payload: {
    name?: string;
    imageUrl?: string;
    styleTags?: string;
    clothingType: ClothingType;
  }
): Promise<ClothingItem> {
  const { data } = await http.put<ApiResult<ClothingItem>>(`/api/clothing/${clothingId}`, payload);
  return data.data;
}

export async function updateClothingStatus(
  clothingId: number,
  status: 'ON_SHELF' | 'OFF_SHELF'
): Promise<ClothingItem> {
  const { data } = await http.patch<ApiResult<ClothingItem>>(`/api/clothing/${clothingId}/status`, { status });
  return data.data;
}

export async function deleteClothing(clothingId: number): Promise<void> {
  await http.delete(`/api/clothing/${clothingId}`);
}
