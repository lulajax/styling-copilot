<template>
  <section class="panel">
    <div class="section-head">
      <h2>{{ $t('history.title') }}</h2>
      <div class="head-actions">
        <el-button type="primary" @click="openManualDialog">{{ $t('history.addManual') }}</el-button>
        <el-button @click="load">{{ $t('common.refresh') }}</el-button>
      </div>
    </div>

    <el-form class="inline-form" label-position="top">
      <el-form-item :label="$t('common.member')">
        <el-select v-model="memberId" filterable style="width: 220px">
          <el-option v-for="member in members" :key="member.id" :label="member.name" :value="member.id" />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('common.limit')">
        <el-input-number v-model="limit" :min="1" :max="100" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">{{ $t('common.reload') }}</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="records" style="width: 100%">
      <el-table-column prop="id" :label="$t('common.id')" width="110" />
      <el-table-column prop="memberName" :label="$t('common.member')" min-width="140">
        <template #default="scope">
          {{ scope.row.memberName || '-' }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('common.image')" width="110">
        <template #default="scope">
          <el-image
            v-if="getClothingImageUrl(scope.row.clothingId)"
            :src="getClothingImageUrl(scope.row.clothingId)"
            style="width: 56px; height: 56px; border-radius: 8px"
            fit="cover"
          />
          <span v-else class="image-empty">{{ $t('clothing.noImage') }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="clothingName" :label="$t('clothing.clothingName')" min-width="180">
        <template #default="scope">
          {{ scope.row.clothingName || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" :label="$t('common.status')" width="130" />
      <el-table-column prop="performanceScore" :label="$t('history.performanceScore')" width="120" />
      <el-table-column prop="broadcastDate" :label="$t('history.broadcastDate')" min-width="180" />
      <el-table-column :label="$t('common.actions')" width="220">
        <template #default="scope">
          <el-button
            v-if="scope.row.status !== 'BROADCASTED'"
            size="small"
            :disabled="!memberId"
            @click="markAsWorn(scope.row.id)"
          >
            {{ $t('history.markWorn') }}
          </el-button>
          <el-button
            v-else
            size="small"
            type="warning"
            :disabled="!memberId"
            @click="markAsUnworn(scope.row.id)"
          >
            {{ $t('history.markUnworn') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="manualDialogVisible" :title="$t('history.addManual')" width="560px">
      <el-form label-position="top">
        <el-form-item :label="$t('common.member')">
          <el-select v-model="manualForm.memberId" filterable style="width: 100%">
            <el-option v-for="member in members" :key="member.id" :label="member.name" :value="member.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('task.clothingId')">
          <el-select v-model="manualForm.clothingId" filterable style="width: 100%">
            <el-option v-for="item in clothingOptions" :key="item.id" :label="`${item.id} - ${item.name}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('history.broadcastDate')">
          <el-date-picker
            v-model="manualForm.broadcastDate"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="$t('history.performanceScore')">
          <el-input-number v-model="manualForm.performanceScore" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="manualDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submittingManual" @click="submitManual">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { fetchMembers } from '@/api/member';
import { fetchAllClothing } from '@/api/clothing';
import { createManualHistory, fetchHistory, updateHistoryStatus } from '@/api/match';
import type { ClothingItem, MatchHistoryItem, MemberItem } from '@/types/domain';

const { t } = useI18n();

const members = ref<MemberItem[]>([]);
const clothingOptions = ref<ClothingItem[]>([]);
const memberId = ref<number | null>(null);
const limit = ref(10);
const records = ref<MatchHistoryItem[]>([]);
const manualDialogVisible = ref(false);
const submittingManual = ref(false);
const manualForm = reactive({
  memberId: null as number | null,
  clothingId: null as number | null,
  broadcastDate: '',
  performanceScore: null as number | null
});

function getClothingImageUrl(clothingId: number): string | null {
  const clothing = clothingOptions.value.find((item) => item.id === clothingId);
  return clothing?.imageUrl ?? null;
}

async function loadMembers() {
  try {
    const data = await fetchMembers(0, 100);
    members.value = data.items;
    if (!memberId.value && data.items.length > 0) {
      memberId.value = data.items[0].id;
    }
  } catch (error) {
    ElMessage.error(t('members.loadFailed'));
  }
}

async function loadClothingOptions() {
  try {
    const data = await fetchAllClothing(0, 200);
    clothingOptions.value = data.items;
  } catch (error) {
    ElMessage.error(t('matchStudio.loadClothingFailed'));
  }
}

async function load() {
  if (!memberId.value) {
    return;
  }
  try {
    const data = await fetchHistory(memberId.value, limit.value);
    records.value = data.records;
  } catch (error) {
    ElMessage.error(t('history.loadFailed'));
  }
}

function openManualDialog() {
  manualForm.memberId = memberId.value;
  manualForm.clothingId = null;
  manualForm.broadcastDate = '';
  manualForm.performanceScore = null;
  manualDialogVisible.value = true;
}

async function submitManual() {
  if (!manualForm.memberId || !manualForm.clothingId) {
    ElMessage.warning(t('history.validateManual'));
    return;
  }

  submittingManual.value = true;
  try {
    await createManualHistory(manualForm.memberId, {
      clothingId: manualForm.clothingId,
      broadcastDate: manualForm.broadcastDate || undefined,
      performanceScore: manualForm.performanceScore ?? undefined
    });
    ElMessage.success(t('history.manualSuccess'));
    manualDialogVisible.value = false;
    if (memberId.value === manualForm.memberId) {
      await load();
    }
  } catch (error) {
    ElMessage.error(t('history.manualFailed'));
  } finally {
    submittingManual.value = false;
  }
}

async function markAsWorn(recordId: number) {
  if (!memberId.value) {
    return;
  }
  try {
    await updateHistoryStatus(memberId.value, recordId, 'BROADCASTED');
    ElMessage.success(t('history.markWornSuccess'));
    await load();
  } catch (error) {
    ElMessage.error(t('history.markWornFailed'));
  }
}

async function markAsUnworn(recordId: number) {
  if (!memberId.value) {
    return;
  }
  try {
    await updateHistoryStatus(memberId.value, recordId, 'DRAFT');
    ElMessage.success(t('history.markUnwornSuccess'));
    await load();
  } catch (error) {
    ElMessage.error(t('history.markUnwornFailed'));
  }
}

onMounted(async () => {
  try {
    await Promise.all([loadMembers(), loadClothingOptions()]);
    await load();
  } catch (error) {
    // handled by sub-functions
  }
});
</script>
