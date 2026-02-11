export interface MemberItem {
  id: number;
  name: string;
  bodyData: string;
  photoUrl: string | null;
  styleTags: string;
}

export interface BodyMeasurements {
  heightCm: number;
  weightKg: number;
  shoulderWidthCm: number;
  bustCm: number;
  waistCm: number;
  hipCm: number;
  bodyShape: 'X' | 'H' | 'A' | 'O';
  legRatio: 'short' | 'regular' | 'long';
  topSize: 'XS' | 'S' | 'M' | 'L' | 'XL';
  bottomSize: 'XS' | 'S' | 'M' | 'L' | 'XL';
}

export interface BodyDerivedMetrics {
  bmi: number;
  whr: number;
  shapeClass: 'X' | 'H' | 'A' | 'O';
}

export interface BodyProfileV2 {
  version: 2;
  measurements: BodyMeasurements;
  derived: BodyDerivedMetrics;
}

export type ClothingType = 'TOP' | 'BOTTOM' | 'ONE_PIECE' | 'SET';

export interface ClothingSizeData {
  shoulderWidthCm?: number;
  bustCm?: number;
  waistCm?: number;
  hipCm?: number;
  lengthCm?: number;
  sleeveLengthCm?: number;
  inseamCm?: number;
  topSize?: 'XS' | 'S' | 'M' | 'L' | 'XL';
  bottomSize?: 'XS' | 'S' | 'M' | 'L' | 'XL';
}

export interface ClothingItem {
  id: number;
  name: string;
  imageUrl: string | null;
  styleTags: string;
  clothingType: ClothingType;
  status: 'ON_SHELF' | 'OFF_SHELF';
  sizeData?: ClothingSizeData;
}

export interface MatchResultItem {
  clothingId: number;
  reason: string;
  score: number;
}

export interface OutfitPreview {
  title: string;
  outfitDescription: string;
  imagePrompt: string;
}

export interface OutfitRecommendation {
  outfitNo: number;
  topClothingId: number;
  bottomClothingId: number;
  score: number;
  reason: string;
  preview: OutfitPreview | null;
  warning: string | null;
}

export type TaskStatus = 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED';

export interface MatchTaskDetail {
  taskId: string;
  status: TaskStatus;
  strategyName: string | null;
  outfits: OutfitRecommendation[];
  result: MatchResultItem[];
  preview: OutfitPreview | null;
  errorMessage: string | null;
}

export interface MatchTaskSummary {
  taskId: string;
  memberId: number;
  scene: string | null;
  status: TaskStatus;
  strategyName: string | null;
  createdAt: string;
}

export interface MatchHistoryItem {
  id: number;
  memberId: number;
  memberName: string | null;
  clothingId: number;
  clothingName: string | null;
  status: 'DRAFT' | 'ACCEPTED' | 'BROADCASTED' | 'REJECTED';
  performanceScore: number | null;
  broadcastDate: string | null;
}

export interface CreateManualHistoryPayload {
  clothingId: number;
  broadcastDate?: string;
  performanceScore?: number;
}

export interface UploadFileResponse {
  url: string;
  objectKey: string;
  bizType: 'member' | 'clothing';
  originalName: string;
  size: number;
  contentType: string | null;
}
