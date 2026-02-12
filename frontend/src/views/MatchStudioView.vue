<template>
  <section class="panel">
    <div class="section-head">
      <h2>{{ $t('match.title') }}</h2>
      <el-button @click="loadInitial">{{ $t('common.reload') }}</el-button>
    </div>

    <el-form class="inline-form" label-position="top">
      <el-form-item :label="$t('common.member')">
        <el-select v-model="selectedMemberId" filterable style="width: 220px" @change="onMemberChange">
          <el-option v-for="member in members" :key="member.id" :label="member.name" :value="member.id" />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('common.scene')">
        <el-input v-model="scene" :placeholder="$t('match.scenePlaceholder')" style="width: 220px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :disabled="selectedClothingIds.length === 0 || !selectedMemberId" :loading="creating" @click="createTask">
          {{ $t('match.createTask') }}
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 快捷操作栏 -->
    <div class="toolbar">
      <el-button-group>
        <el-button type="primary" :icon="Opportunity" :disabled="!selectedMemberId" @click="smartSelect">
          {{ $t('match.smartSelect') }}
        </el-button>
        <el-button :icon="Select" @click="selectAll">
          {{ $t('match.selectAll') }}
        </el-button>
        <el-button :icon="Delete" @click="clearSelection">
          {{ $t('match.clearSelection') }}
        </el-button>
      </el-button-group>
      <span class="selection-count">
        {{ $t('match.selectedCount', { count: selectedClothingIds.length }) }}
      </span>
      <el-button v-if="taskId" type="success" @click="dialogVisible = true">
        {{ $t('match.viewTaskResult') }}
      </el-button>
    </div>

    <el-table ref="clothingTable" :data="clothingItems" row-key="id" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="50" reserve-selection />
      <el-table-column prop="id" :label="$t('common.id')" width="80" />
      <el-table-column :label="$t('common.image')" width="90">
        <template #default="scope">
          <el-image
            :src="scope.row.imageUrl || '/placeholder-clothing.png'"
            :preview-src-list="scope.row.imageUrl ? [scope.row.imageUrl] : []"
            fit="cover"
            style="width: 60px; height: 60px; border-radius: 4px"
          >
            <template #error>
              <div class="image-placeholder">
                <el-icon><Picture /></el-icon>
              </div>
            </template>
          </el-image>
        </template>
      </el-table-column>
      <el-table-column prop="name" :label="$t('common.name')" min-width="160" />
      <el-table-column :label="$t('members.styleTags')" min-width="180">
        <template #default="scope">
          {{ formatLocalizedStyleTags(scope.row.styleTags, t) }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('common.type')" width="100">
        <template #default="scope">
          <el-tag size="small" :type="scope.row.clothingType === 'TOP' ? 'primary' : 'success'">
            {{ $t(`clothingType.${scope.row.clothingType}`) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$t('common.status')" width="100">
        <template #default="scope">
          <el-tag size="small" :type="scope.row.status === 'ON_SHELF' ? 'success' : 'info'">
            {{ $t(`clothingStatus.${scope.row.status}`) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$t('clothing.sizeInfo')" width="100">
        <template #default="scope">
          <span v-if="scope.row.sizeData" class="size-tag">
            {{ getClothingSize(scope.row) }}
          </span>
          <span v-else class="size-tag empty">-</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('match.sizeMatch')" width="120">
        <template #default="scope">
          <el-tag
            v-if="selectedMemberId"
            size="small"
            :type="getSizeMatchStatus(scope.row).type"
          >
            {{ $t(`match.sizeMatchStatus.${getSizeMatchStatus(scope.row).status}`) }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 任务结果弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="taskId ? `${$t('common.taskId')}: ${taskId}` : $t('match.taskResult')"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="dialog-content">
        <div class="task-status-bar">
          <span>{{ $t('common.status') }}: {{ $t(`taskStatus.${taskStatus}`) }}</span>
          <el-progress :percentage="Math.floor(progress)" :status="taskStatus === 'FAILED' ? 'exception' : undefined" />
        </div>

        <div v-if="outfits.length > 0" class="outfit-list">
          <el-card v-for="outfit in outfits" :key="outfit.outfitNo" class="outfit-card">
            <template #header>
              <div class="outfit-header">
                <strong>{{ $t('match.outfitNo') }} #{{ outfit.outfitNo }}</strong>
                <div class="outfit-actions">
                  <el-tag type="success">{{ $t('common.score') }}: {{ outfit.score }}</el-tag>
                  <el-button
                    size="small"
                    :loading="previewLoadingOutfitNo === outfit.outfitNo"
                    :disabled="taskStatus !== 'SUCCEEDED'"
                    @click="generateOutfitPreview(outfit.outfitNo)"
                  >
                    {{ outfit.preview ? $t('match.regeneratePreview') : $t('match.generatePreview') }}
                  </el-button>
                </div>
              </div>
            </template>
            <div class="outfit-visual-grid">
              <div class="outfit-piece">
                <div class="piece-label">{{ $t('common.member') }}</div>
                <el-image
                  v-if="selectedMember?.photoUrl"
                  :src="selectedMember.photoUrl"
                  fit="contain"
                  class="piece-image"
                  :preview-src-list="[selectedMember.photoUrl]"
                >
                  <template #error>
                    <div class="piece-image-placeholder">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <div v-else class="piece-image-placeholder">
                  <el-icon><Picture /></el-icon>
                </div>
                <div class="piece-name">{{ selectedMember?.name || '-' }}</div>
              </div>

              <div class="outfit-piece">
                <div class="piece-label">{{ $t('match.topClothingId') }}</div>
                <el-image
                  v-if="getClothingById(outfit.topClothingId)?.imageUrl"
                  :src="getClothingById(outfit.topClothingId)?.imageUrl || ''"
                  fit="contain"
                  class="piece-image"
                  :preview-src-list="[getClothingById(outfit.topClothingId)?.imageUrl || '']"
                >
                  <template #error>
                    <div class="piece-image-placeholder">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <div v-else class="piece-image-placeholder">
                  <el-icon><Picture /></el-icon>
                </div>
                <div class="piece-name">{{ getClothingById(outfit.topClothingId)?.name || '-' }}</div>
                <div class="piece-id">ID: {{ outfit.topClothingId }}</div>
              </div>

              <div class="outfit-piece">
                <div class="piece-label">{{ $t('match.bottomClothingId') }}</div>
                <el-image
                  v-if="getClothingById(outfit.bottomClothingId)?.imageUrl"
                  :src="getClothingById(outfit.bottomClothingId)?.imageUrl || ''"
                  fit="contain"
                  class="piece-image"
                  :preview-src-list="[getClothingById(outfit.bottomClothingId)?.imageUrl || '']"
                >
                  <template #error>
                    <div class="piece-image-placeholder">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <div v-else class="piece-image-placeholder">
                  <el-icon><Picture /></el-icon>
                </div>
                <div class="piece-name">{{ getClothingById(outfit.bottomClothingId)?.name || '-' }}</div>
                <div class="piece-id">ID: {{ outfit.bottomClothingId }}</div>
              </div>
            </div>
            <p><strong>{{ $t('common.reason') }}:</strong> {{ outfit.reason }}</p>
            <template v-if="outfit.preview">
              <div class="preview-box">
                <h4>{{ outfit.preview.title }}</h4>
                <p><strong>{{ $t('match.outfitDescription') }}:</strong> {{ outfit.preview.outfitDescription }}</p>
                <p><strong>{{ $t('match.imagePrompt') }}:</strong> {{ outfit.preview.imagePrompt }}</p>
              </div>
            </template>
            <p v-else-if="outfit.warning" class="warning-text">{{ outfit.warning }}</p>
          </el-card>
        </div>
        <template v-else>
          <el-table :data="taskResult" style="margin-top: 12px">
            <el-table-column prop="clothingId" :label="$t('task.clothingId')" width="120" />
            <el-table-column prop="score" :label="$t('common.score')" width="120" />
            <el-table-column prop="reason" :label="$t('common.reason')" min-width="280" />
          </el-table>

          <div v-if="preview" class="preview-box">
            <h4>{{ preview.title }}</h4>
            <p><strong>{{ $t('match.outfitDescription') }}:</strong> {{ preview.outfitDescription }}</p>
            <p><strong>{{ $t('match.imagePrompt') }}:</strong> {{ preview.imagePrompt }}</p>
          </div>
        </template>

        <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
      </div>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { Opportunity, Select, Delete, Picture } from '@element-plus/icons-vue';
import { fetchMembers } from '@/api/member';
import { fetchOnShelfClothing } from '@/api/clothing';
import { createMatchTask, fetchTask, generateTaskOutfitPreview, streamTaskEvents } from '@/api/match';
import { formatLocalizedStyleTags } from '@/utils/styleTags';
import type { ClothingItem, ClothingSizeData, MatchResultItem, MemberItem, OutfitPreview, OutfitRecommendation, TaskStatus, BodyProfileV2 } from '@/types/domain';
import type { ElTable } from 'element-plus';

const { t } = useI18n();

const members = ref<MemberItem[]>([]);
const clothingItems = ref<ClothingItem[]>([]);
const selectedMemberId = ref<number | null>(null);
const selectedClothingIds = ref<number[]>([]);
const scene = ref('daily-live');
const clothingTable = ref<InstanceType<typeof ElTable>>();

const creating = ref(false);
const taskId = ref('');
const taskStatus = ref<TaskStatus>('QUEUED');
const progress = ref(0);
const outfits = ref<OutfitRecommendation[]>([]);
const taskResult = ref<MatchResultItem[]>([]);
const preview = ref<OutfitPreview | null>(null);
const errorMessage = ref('');
const previewLoadingOutfitNo = ref<number | null>(null);
const dialogVisible = ref(false);

let abortController: AbortController | null = null;
let progressTimer: ReturnType<typeof setInterval> | null = null;

// 进度条平滑增长
function startSmoothProgress(targetProgress: number, speed: number = 200) {
  stopSmoothProgress();
  progressTimer = setInterval(() => {
    if (progress.value < targetProgress) {
      // 缓慢增加，但越接近目标越慢
      const remaining = targetProgress - progress.value;
      const increment = Math.max(0.3, remaining * 0.05);
      progress.value = Math.min(targetProgress, progress.value + increment);
    }
  }, speed);
}

function stopSmoothProgress() {
  if (progressTimer) {
    clearInterval(progressTimer);
    progressTimer = null;
  }
}

// 获取当前选中主播的信息
const selectedMember = computed(() => {
  if (!selectedMemberId.value) return null;
  return members.value.find(m => m.id === selectedMemberId.value) || null;
});

// 解析主播身材数据
const selectedMemberBodyProfile = computed((): BodyProfileV2 | null => {
  if (!selectedMember.value?.bodyData) return null;
  try {
    return JSON.parse(selectedMember.value.bodyData) as BodyProfileV2;
  } catch {
    return null;
  }
});

async function loadInitial() {
  try {
    const [memberData, clothingData] = await Promise.all([
      fetchMembers(0, 100),
      fetchOnShelfClothing(0, 100)
    ]);
    members.value = memberData.items;
    clothingItems.value = clothingData.items;
    if (!selectedMemberId.value && members.value.length > 0) {
      selectedMemberId.value = members.value[0].id;
      // 自动预选
      setTimeout(() => smartSelect(), 0);
    }
  } catch (error) {
    ElMessage.error(t('match.loadMembersFailed'));
  }
}

function onSelectionChange(rows: ClothingItem[]) {
  selectedClothingIds.value = rows.map((row) => row.id);
}

function getClothingById(clothingId: number): ClothingItem | null {
  return clothingItems.value.find((item) => item.id === clothingId) ?? null;
}

// 主播切换时自动预选
function onMemberChange() {
  clearSelection();
  setTimeout(() => smartSelect(), 0);
}

// 解析 sizeData（可能是JSON字符串或对象）
function parseSizeData(sizeData: string | ClothingSizeData | undefined): ClothingSizeData | null {
  if (!sizeData) return null;
  if (typeof sizeData === 'string') {
    try {
      return JSON.parse(sizeData) as ClothingSizeData;
    } catch {
      return null;
    }
  }
  return sizeData;
}

// 获取服装尺码
function getClothingSize(clothing: ClothingItem): string {
  const sizeInfo = parseSizeData(clothing.sizeData);
  if (!sizeInfo) return '-';
  const size = clothing.clothingType === 'TOP'
    ? sizeInfo.topSize
    : sizeInfo.bottomSize;
  return size || '-';
}

// 判断尺码匹配状态
function getSizeMatchStatus(clothing: ClothingItem): { type: 'success' | 'warning' | 'danger' | 'info'; status: 'match' | 'close' | 'mismatch' | 'unknown' } {
  const sizeInfo = parseSizeData(clothing.sizeData);
  if (!selectedMemberBodyProfile.value || !sizeInfo) {
    return { type: 'info', status: 'unknown' };
  }

  const memberSize = clothing.clothingType === 'TOP'
    ? selectedMemberBodyProfile.value.measurements.topSize
    : selectedMemberBodyProfile.value.measurements.bottomSize;

  const clothingSize = clothing.clothingType === 'TOP'
    ? sizeInfo.topSize
    : sizeInfo.bottomSize;

  if (!clothingSize) {
    return { type: 'info', status: 'unknown' };
  }

  // 尺码匹配逻辑
  const sizeOrder = ['XS', 'S', 'M', 'L', 'XL'];
  const memberIdx = sizeOrder.indexOf(memberSize);
  const clothingIdx = sizeOrder.indexOf(clothingSize);

  if (memberIdx === -1 || clothingIdx === -1) {
    return { type: 'info', status: 'unknown' };
  }

  const diff = Math.abs(memberIdx - clothingIdx);

  if (diff === 0) {
    return { type: 'success', status: 'match' };
  } else if (diff === 1) {
    return { type: 'warning', status: 'close' };
  } else {
    return { type: 'danger', status: 'mismatch' };
  }
}

// 智能预选
function smartSelect() {
  if (!selectedMemberId.value) {
    ElMessage.warning(t('match.selectMemberHint'));
    return;
  }

  if (!selectedMemberBodyProfile.value) {
    ElMessage.warning(t('match.noBodyData'));
    return;
  }

  const member = selectedMember.value;
  const memberSizes = {
    top: selectedMemberBodyProfile.value.measurements.topSize,
    bottom: selectedMemberBodyProfile.value.measurements.bottomSize
  };

  // 找到匹配的衣服
  const matchedIds: number[] = [];
  const matchedItems: ClothingItem[] = [];

  clothingItems.value.forEach(item => {
    const matchStatus = getSizeMatchStatus(item);
    if (matchStatus.status === 'match' || matchStatus.status === 'close') {
      matchedIds.push(item.id);
      matchedItems.push(item);
    }
  });

  // 设置选中状态
  matchedItems.forEach(item => {
    clothingTable.value?.toggleRowSelection(item, true);
  });

  ElMessage.success(t('match.smartSelectSuccess', {
    name: member?.name,
    count: matchedIds.length
  }));
}

// 全选
function selectAll() {
  clothingItems.value.forEach(item => {
    clothingTable.value?.toggleRowSelection(item, true);
  });
}

// 清空选择
function clearSelection() {
  clothingTable.value?.clearSelection();
  selectedClothingIds.value = [];
}

async function createTask() {
  if (!selectedMemberId.value) {
    ElMessage.warning(t('match.selectMemberHint'));
    return;
  }

  if (selectedClothingIds.value.length === 0) {
    ElMessage.warning(t('match.selectClothingHint'));
    return;
  }

  creating.value = true;
  outfits.value = [];
  taskResult.value = [];
  preview.value = null;
  errorMessage.value = '';
  progress.value = 0;

  try {
    const response = await createMatchTask({
      memberId: selectedMemberId.value,
      clothingIds: selectedClothingIds.value,
      scene: scene.value
    });

    taskId.value = response.taskId;
    taskStatus.value = response.status;
    dialogVisible.value = true; // 自动打开弹窗
    await subscribeTaskStream(response.taskId);
  } catch (error) {
    ElMessage.error(t('match.createTaskFailed'));
  } finally {
    creating.value = false;
  }
}

function applyTaskDetail(detail: {
  outfits: OutfitRecommendation[];
  result: MatchResultItem[];
  preview: OutfitPreview | null;
  errorMessage: string | null;
  status?: TaskStatus;
}) {
  outfits.value = detail.outfits ?? [];
  taskResult.value = detail.result ?? [];
  preview.value = detail.preview ?? null;
  errorMessage.value = detail.errorMessage ?? '';
  if (detail.status) {
    taskStatus.value = detail.status;
  }
}

async function generateOutfitPreview(outfitNo: number) {
  if (!taskId.value) {
    return;
  }
  previewLoadingOutfitNo.value = outfitNo;
  try {
    const detail = await generateTaskOutfitPreview(taskId.value, outfitNo);
    applyTaskDetail(detail);
    ElMessage.success(t('match.generatePreviewSuccess'));
  } catch (error) {
    ElMessage.error(t('match.generatePreviewFailed'));
  } finally {
    previewLoadingOutfitNo.value = null;
  }
}

async function subscribeTaskStream(id: string) {
  abortController?.abort();
  abortController = new AbortController();

  try {
    await streamTaskEvents(id, {
      signal: abortController.signal,
      onEvent: async (name, payload) => {
        if (name === 'task_started') {
          taskStatus.value = 'RUNNING';
          // 快速增加到15%，然后平滑到20%
          progress.value = 15;
          startSmoothProgress(25, 150);
        }

        if (name === 'task_progress') {
          const progressValue = Number((payload as { progress?: number })?.progress ?? 50);
          // 如果后端报告45%（AI开始），平滑增长到85%
          if (progressValue === 45) {
            startSmoothProgress(75, 300); // 慢速增长，因为AI处理需要时间
          } else if (progressValue === 85) {
            startSmoothProgress(92, 200); // 保存结果阶段
          } else if (progressValue > progress.value) {
            progress.value = progressValue;
          }
        }

        if (name === 'task_completed') {
          stopSmoothProgress();
          taskStatus.value = 'SUCCEEDED';
          progress.value = 100;
          const detail = await fetchTask(id);
          applyTaskDetail(detail);
          ElMessage.success(t('common.success'));
          abortController?.abort();
        }

        if (name === 'task_failed') {
          stopSmoothProgress();
          taskStatus.value = 'FAILED';
          progress.value = 100;
          const detail = await fetchTask(id);
          errorMessage.value = detail.errorMessage ?? t('common.failed');
          ElMessage.error(errorMessage.value);
          abortController?.abort();
        }
      },
      onError: async () => {
        const detail = await fetchTask(id);
        applyTaskDetail(detail);
      }
    });
  } catch (error) {
    const detail = await fetchTask(id);
    applyTaskDetail(detail);
  }
}

onMounted(async () => {
  try {
    await loadInitial();
  } catch (error) {
    // Handled in loadInitial()
  }
});

onBeforeUnmount(() => {
  abortController?.abort();
  stopSmoothProgress();
});
</script>

<style scoped>
.outfit-card p {
  margin: 6px 0;
}

.outfit-visual-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
  margin-bottom: 10px;
}

.outfit-piece {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 10px;
  background: #fafbfd;
}

.piece-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.piece-image {
  width: 100%;
  height: clamp(180px, 28vw, 320px);
  border-radius: 8px;
  display: block;
  background: #f6f7f9;
}

.piece-image-placeholder {
  width: 100%;
  height: clamp(180px, 28vw, 320px);
  border-radius: 8px;
  background: #f2f3f5;
  color: #b7bcc5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.piece-name {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}

.piece-id {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
}

.error-text {
  margin-top: 12px;
  color: #c45656;
  padding: 12px 14px;
  background: #fef0f0;
  border-radius: 10px;
}
</style>
