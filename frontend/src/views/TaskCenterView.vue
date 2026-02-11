<template>
  <section class="panel">
    <div class="section-head">
      <h2>{{ $t('task.title') }}</h2>
      <el-button @click="load">{{ $t('common.refresh') }}</el-button>
    </div>

    <el-form class="inline-form" label-position="top">
      <el-form-item :label="$t('common.member') + ' (' + $t('common.optional') + ')'">
        <el-select v-model="memberIdFilter" clearable :placeholder="$t('task.allMembers')" style="width: 200px" @change="load">
          <el-option
            v-for="member in members"
            :key="member.id"
            :label="member.name"
            :value="member.id"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <el-table :data="tasks" style="width: 100%">
      <el-table-column prop="taskId" :label="$t('common.taskId')" min-width="260" />
      <el-table-column :label="$t('common.member')" width="140">
        <template #default="scope">
          {{ getMemberName(scope.row.memberId) }}
        </template>
      </el-table-column>
      <el-table-column prop="scene" :label="$t('common.scene')" width="140" />
      <el-table-column prop="status" :label="$t('common.status')" width="120" />
      <el-table-column prop="createdAt" :label="$t('common.createdAt')" min-width="180" />
      <el-table-column :label="$t('common.actions')" width="120">
        <template #default="scope">
          <el-button size="small" @click="viewResult(scope.row)">{{ $t('common.viewResult') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="detailVisible" :title="$t('common.viewResult')" width="800px">
      <template v-if="detailTask">
        <el-descriptions border :column="2" class="mb-4">
          <el-descriptions-item :label="$t('common.taskId')">{{ detailTask.taskId }}</el-descriptions-item>
          <el-descriptions-item :label="$t('common.status')">{{ detailTask.status }}</el-descriptions-item>
          <el-descriptions-item :label="$t('common.error')" v-if="detailTask.errorMessage">
            <span style="color: #f56c6c">{{ detailTask.errorMessage }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <template v-if="detailTask.outfits && detailTask.outfits.length > 0">
          <h4>{{ $t('task.recommendations') }}</h4>
          <el-card v-for="outfit in detailTask.outfits" :key="outfit.outfitNo" class="mb-4">
            <template #header>
              <div class="outfit-header">
                <span>{{ $t('match.outfitNo') }} #{{ outfit.outfitNo }}</span>
                <el-tag type="success">{{ $t('common.score') }}: {{ outfit.score }}</el-tag>
              </div>
            </template>
            <p><strong>{{ $t('match.topClothingId') }}:</strong> {{ outfit.topClothingId }}</p>
            <p><strong>{{ $t('match.bottomClothingId') }}:</strong> {{ outfit.bottomClothingId }}</p>
            <p><strong>{{ $t('common.reason') }}:</strong> {{ outfit.reason }}</p>
            <el-button
              size="small"
              :loading="previewLoadingOutfitNo === outfit.outfitNo"
              :disabled="detailTask.status !== 'SUCCEEDED'"
              @click="generateOutfitPreview(outfit.outfitNo)"
            >
              {{ outfit.preview ? $t('match.regeneratePreview') : $t('match.generatePreview') }}
            </el-button>
            <template v-if="outfit.preview">
              <p><strong>{{ $t('task.previewTitle') }}:</strong> {{ outfit.preview.title }}</p>
              <p><strong>{{ $t('task.previewDescription') }}:</strong> {{ outfit.preview.outfitDescription }}</p>
              <p><strong>{{ $t('task.previewImagePrompt') }}:</strong> {{ outfit.preview.imagePrompt }}</p>
            </template>
            <p v-else-if="outfit.warning" class="warning-text">{{ outfit.warning }}</p>
          </el-card>
        </template>
        <template v-else-if="detailTask.result && detailTask.result.length > 0">
          <h4>{{ $t('task.recommendations') }}</h4>
          <el-table :data="detailTask.result" style="width: 100%" class="mb-4">
            <el-table-column prop="clothingId" :label="$t('task.clothingId')" width="120" />
            <el-table-column prop="score" :label="$t('common.score')" width="100">
              <template #default="scope">
                <el-tag :type="getScoreTagType(scope.row.score)">{{ scope.row.score }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reason" :label="$t('common.reason')" />
          </el-table>
        </template>

        <template v-if="detailTask.preview">
          <h4>{{ $t('task.previewTitle') }}</h4>
          <el-card>
            <template #header>
              <span>{{ detailTask.preview.title }}</span>
            </template>
            <p><strong>{{ $t('task.previewDescription') }}:</strong> {{ detailTask.preview.outfitDescription }}</p>
            <p v-if="detailTask.preview.imagePrompt" class="mt-2">
              <strong>{{ $t('task.previewImagePrompt') }}:</strong> {{ detailTask.preview.imagePrompt }}
            </p>
          </el-card>
        </template>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { fetchTask, fetchTaskList, generateTaskOutfitPreview } from '@/api/match';
import { fetchMembers } from '@/api/member';
import type { MatchTaskDetail, MatchTaskSummary, MemberItem } from '@/types/domain';

const { t } = useI18n();

const tasks = ref<MatchTaskSummary[]>([]);
const members = ref<MemberItem[]>([]);
const memberIdFilter = ref<number | undefined>();
const detailVisible = ref(false);
const detailTask = ref<MatchTaskDetail | null>(null);
const previewLoadingOutfitNo = ref<number | null>(null);

function getMemberName(memberId: number): string {
  const member = members.value.find(m => m.id === memberId);
  return member?.name ?? `ID:${memberId}`;
}

async function loadMembers() {
  try {
    const data = await fetchMembers(0, 100);
    members.value = data.items;
  } catch (error) {
    ElMessage.error(t('members.loadFailed'));
  }
}

async function load() {
  try {
    const [taskData] = await Promise.all([
      fetchTaskList(memberIdFilter.value, 0, 50),
      members.value.length === 0 ? loadMembers() : Promise.resolve()
    ]);
    tasks.value = taskData.items;
  } catch (error) {
    ElMessage.error(t('task.loadFailed'));
  }
}

async function viewResult(row: MatchTaskSummary) {
  try {
    const data = await fetchTask(row.taskId);
    detailTask.value = data;
    detailVisible.value = true;
  } catch (error) {
    ElMessage.error(t('task.loadDetailFailed'));
  }
}

async function generateOutfitPreview(outfitNo: number) {
  if (!detailTask.value) {
    return;
  }
  previewLoadingOutfitNo.value = outfitNo;
  try {
    const data = await generateTaskOutfitPreview(detailTask.value.taskId, outfitNo);
    detailTask.value = data;
    ElMessage.success(t('match.generatePreviewSuccess'));
  } catch (error) {
    ElMessage.error(t('match.generatePreviewFailed'));
  } finally {
    previewLoadingOutfitNo.value = null;
  }
}

function getScoreTagType(score: number): '' | 'success' | 'warning' | 'danger' {
  if (score >= 80) return 'success';
  if (score >= 60) return 'warning';
  return 'danger';
}

onMounted(() => {
  loadMembers();
  load();
});
</script>

<style scoped>
.mb-4 {
  margin-bottom: 16px;
}
.mt-2 {
  margin-top: 8px;
}
h4 {
  margin: 16px 0 12px;
  color: #385577;
}

.outfit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.warning-text {
  color: #c45656;
}
</style>
