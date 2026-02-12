<template>
  <section class="panel">
    <div class="section-head">
      <h2>{{ $t('members.title') }}</h2>
      <div class="head-actions">
        <el-button type="primary" @click="openCreate">{{ $t('members.addMember') }}</el-button>
        <el-button @click="load">{{ $t('common.refresh') }}</el-button>
      </div>
    </div>

    <el-table :data="members" style="width: 100%">
      <el-table-column prop="id" :label="$t('common.id')" width="90" />
      <el-table-column prop="name" :label="$t('common.name')" width="160" />
      <el-table-column :label="$t('common.photo')" width="110">
        <template #default="scope">
          <el-image
            v-if="scope.row.photoUrl"
            :src="scope.row.photoUrl"
            class="member-photo-thumb"
            fit="contain"
            :preview-src-list="[scope.row.photoUrl]"
            :preview-teleported="true"
          />
          <span v-else class="image-empty">{{ $t('members.noPhoto') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('members.bodySummary')" min-width="340">
        <template #default="scope">
          {{ formatBodySummary(scope.row.bodyData) }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('members.styleTags')" min-width="220">
        <template #default="scope">
          {{ formatLocalizedStyleTags(scope.row.styleTags, t) }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('common.actions')" width="280">
        <template #default="scope">
          <el-button size="small" @click="openEdit(scope.row)">{{ $t('common.edit') }}</el-button>
          <el-button size="small" @click="openDetail(scope.row)">{{ $t('common.detail') }}</el-button>
          <el-button size="small" type="danger" @click="remove(scope.row.id)">{{ $t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? $t('members.addMember') : $t('members.editMember')" width="900px">
      <el-form label-position="top">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item :label="$t('common.name')">
              <el-input v-model="dialogForm.name" :placeholder="$t('members.memberPhoto')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('members.styleTags')">
              <el-select v-model="dialogForm.styleTags" multiple filterable :placeholder="$t('common.selectPlaceholder')" style="width: 100%">
                <el-option
                  v-for="option in styleTagOptions"
                  :key="option.value"
                  :label="t(option.labelKey)"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item :label="$t('common.photo')">
          <div class="upload-area">
            <el-upload
              :show-file-list="false"
              :http-request="uploadMemberPhoto"
              :before-upload="beforeImageUpload"
              accept=".jpg,.jpeg,.png,.webp"
            >
              <el-button :loading="uploadingPhoto">{{ $t('members.uploadPhoto') }}</el-button>
            </el-upload>
            <el-button v-if="dialogForm.photoUrl" link type="danger" @click="dialogForm.photoUrl = ''">{{ $t('common.remove') }}</el-button>
          </div>
          <el-image
            v-if="dialogForm.photoUrl"
            :src="dialogForm.photoUrl"
            class="member-photo-preview"
            fit="contain"
            :preview-src-list="[dialogForm.photoUrl]"
            :preview-teleported="true"
          />
        </el-form-item>

        <h4 class="sub-title">{{ $t('members.coreMeasurements') }}</h4>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item :label="$t('members.height')">
              <el-input-number v-model="dialogForm.body.heightCm" :step="0.1" :min="130" :max="200" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('members.weight')">
              <el-input-number v-model="dialogForm.body.weightKg" :step="0.1" :min="30" :max="120" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('members.shoulderWidth')">
              <el-input-number v-model="dialogForm.body.shoulderWidthCm" :step="0.1" :min="30" :max="55" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item :label="$t('members.bust')">
              <el-input-number v-model="dialogForm.body.bustCm" :step="0.1" :min="60" :max="140" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('members.waist')">
              <el-input-number v-model="dialogForm.body.waistCm" :step="0.1" :min="45" :max="120" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('members.hip')">
              <el-input-number v-model="dialogForm.body.hipCm" :step="0.1" :min="70" :max="150" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item :label="$t('members.bodyShape')">
              <el-select v-model="dialogForm.body.bodyShape" style="width: 100%">
                <el-option label="X" value="X" />
                <el-option label="H" value="H" />
                <el-option label="A" value="A" />
                <el-option label="O" value="O" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('members.legRatio')">
              <el-select v-model="dialogForm.body.legRatio" style="width: 100%">
                <el-option label="short" value="short" />
                <el-option label="regular" value="regular" />
                <el-option label="long" value="long" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item :label="$t('clothing.topSize')">
              <el-select v-model="dialogForm.body.topSize" style="width: 100%">
                <el-option label="XS" value="XS" />
                <el-option label="S" value="S" />
                <el-option label="M" value="M" />
                <el-option label="L" value="L" />
                <el-option label="XL" value="XL" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('clothing.bottomSize')">
              <el-select v-model="dialogForm.body.bottomSize" style="width: 100%">
                <el-option label="XS" value="XS" />
                <el-option label="S" value="S" />
                <el-option label="M" value="M" />
                <el-option label="L" value="L" />
                <el-option label="XL" value="XL" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDialog">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="$t('members.bodyProfileDetail')" width="760px">
      <template v-if="detailProfile">
        <h4>{{ detailName }}</h4>
        <el-descriptions border :column="2">
          <el-descriptions-item :label="$t('members.height')">{{ detailProfile.measurements.heightCm }} cm</el-descriptions-item>
          <el-descriptions-item :label="$t('members.weight')">{{ detailProfile.measurements.weightKg }} kg</el-descriptions-item>
          <el-descriptions-item :label="$t('members.shoulderWidth')">{{ detailProfile.measurements.shoulderWidthCm }} cm</el-descriptions-item>
          <el-descriptions-item :label="$t('members.bust')">{{ detailProfile.measurements.bustCm }} cm</el-descriptions-item>
          <el-descriptions-item :label="$t('members.waist')">{{ detailProfile.measurements.waistCm }} cm</el-descriptions-item>
          <el-descriptions-item :label="$t('members.hip')">{{ detailProfile.measurements.hipCm }} cm</el-descriptions-item>
          <el-descriptions-item :label="$t('members.bodyShape')">{{ detailProfile.measurements.bodyShape }}</el-descriptions-item>
          <el-descriptions-item :label="$t('members.legRatio')">{{ detailProfile.measurements.legRatio }}</el-descriptions-item>
          <el-descriptions-item label="BMI">{{ detailProfile.derived.bmi }}</el-descriptions-item>
          <el-descriptions-item label="WHR">{{ detailProfile.derived.whr }}</el-descriptions-item>
          <el-descriptions-item :label="$t('common.title')">{{ detailProfile.derived.shapeClass }}</el-descriptions-item>
          <el-descriptions-item :label="$t('clothing.topSize')">{{ detailProfile.measurements.topSize }}</el-descriptions-item>
          <el-descriptions-item :label="$t('clothing.bottomSize')">{{ detailProfile.measurements.bottomSize }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import type { UploadRequestOptions } from 'element-plus';
import { createMember, deleteMember, fetchMembers, updateMember } from '@/api/member';
import { uploadFile } from '@/api/file';
import { STYLE_TAG_OPTIONS } from '@/constants/styleTags';
import { formatLocalizedStyleTags, splitStyleTags } from '@/utils/styleTags';
import type { BodyDerivedMetrics, BodyMeasurements, BodyProfileV2, MemberItem } from '@/types/domain';

const { t, tm } = useI18n();

type DialogMode = 'create' | 'edit';

type BodyShape = 'X' | 'H' | 'A' | 'O';
type LegRatio = 'short' | 'regular' | 'long';
type SizeLevel = 'XS' | 'S' | 'M' | 'L' | 'XL';

interface BodyForm {
  heightCm: number | null;
  weightKg: number | null;
  shoulderWidthCm: number | null;
  bustCm: number | null;
  waistCm: number | null;
  hipCm: number | null;
  bodyShape: BodyShape;
  legRatio: LegRatio;
  topSize: SizeLevel;
  bottomSize: SizeLevel;
}

const members = ref<MemberItem[]>([]);
const dialogVisible = ref(false);
const dialogMode = ref<DialogMode>('create');
const editingId = ref<number | null>(null);
const submitting = ref(false);
const uploadingPhoto = ref(false);
const detailVisible = ref(false);
const detailProfile = ref<BodyProfileV2 | null>(null);
const detailName = ref('');
const styleTagOptions = STYLE_TAG_OPTIONS;

const dialogForm = reactive({
  name: '',
  photoUrl: '',
  styleTags: [] as string[],
  body: createDefaultBodyForm()
});

function createDefaultBodyForm(): BodyForm {
  return {
    heightCm: null,
    weightKg: null,
    shoulderWidthCm: null,
    bustCm: null,
    waistCm: null,
    hipCm: null,
    bodyShape: 'H',
    legRatio: 'regular',
    topSize: 'M',
    bottomSize: 'M'
  };
}

function createDefaultMeasurements(): BodyMeasurements {
  return {
    heightCm: 165.0,
    weightKg: 50.0,
    shoulderWidthCm: 38.0,
    bustCm: 84.0,
    waistCm: 64.0,
    hipCm: 90.0,
    bodyShape: 'H',
    legRatio: 'regular',
    topSize: 'M',
    bottomSize: 'M'
  };
}

function round(value: number, scale: number): number {
  const ratio = Math.pow(10, scale);
  return Math.round(value * ratio) / ratio;
}

function inferShapeClass(m: BodyMeasurements): BodyShape {
  const bustWaist = m.bustCm - m.waistCm;
  const hipWaist = m.hipCm - m.waistCm;
  const bustHipGap = Math.abs(m.bustCm - m.hipCm);
  if (m.waistCm >= m.bustCm && m.waistCm >= m.hipCm) {
    return 'O';
  }
  if (bustWaist >= 20 && hipWaist >= 20 && bustHipGap <= 10) {
    return 'X';
  }
  if (hipWaist - bustWaist >= 8) {
    return 'A';
  }
  return 'H';
}

function parseBodyProfile(bodyData: string): BodyProfileV2 {
  const defaults = createDefaultMeasurements();
  try {
    const parsed = JSON.parse(bodyData ?? '{}') as Record<string, unknown>;
    const source = isRecord(parsed.measurements) ? parsed.measurements : parsed;

    const measurements: BodyMeasurements = {
      heightCm: readNumber(source, ['heightCm', 'height'], defaults.heightCm),
      weightKg: readNumber(source, ['weightKg', 'weight'], defaults.weightKg),
      shoulderWidthCm: readNumber(source, ['shoulderWidthCm', 'shoulderWidth'], defaults.shoulderWidthCm),
      bustCm: readNumber(source, ['bustCm', 'bust'], defaults.bustCm),
      waistCm: readNumber(source, ['waistCm', 'waist'], defaults.waistCm),
      hipCm: readNumber(source, ['hipCm', 'hip'], defaults.hipCm),
      bodyShape: readBodyShape(source, defaults.bodyShape),
      legRatio: readLegRatio(source, defaults.legRatio),
      topSize: readSize(source, ['topSize'], defaults.topSize),
      bottomSize: readSize(source, ['bottomSize'], defaults.bottomSize)
    };

    const shapeClass = inferShapeClass(measurements);
    if (!measurements.bodyShape) {
      measurements.bodyShape = shapeClass;
    }

    const derivedFallback: BodyDerivedMetrics = {
      bmi: round(measurements.weightKg / Math.pow(measurements.heightCm / 100, 2), 1),
      whr: round(measurements.waistCm / measurements.hipCm, 2),
      shapeClass
    };

    const derivedSource = isRecord(parsed.derived) ? parsed.derived : {};
    return {
      version: 2,
      measurements,
      derived: {
        bmi: readNumber(derivedSource, ['bmi'], derivedFallback.bmi),
        whr: readNumber(derivedSource, ['whr'], derivedFallback.whr),
        shapeClass: readBodyShape(derivedSource, derivedFallback.shapeClass, ['shapeClass'])
      }
    };
  } catch (error) {
    return {
      version: 2,
      measurements: defaults,
      derived: {
        bmi: round(defaults.weightKg / Math.pow(defaults.heightCm / 100, 2), 1),
        whr: round(defaults.waistCm / defaults.hipCm, 2),
        shapeClass: inferShapeClass(defaults)
      }
    };
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function readNumber(source: Record<string, unknown>, keys: string[], fallback: number): number {
  for (const key of keys) {
    const raw = source[key];
    if (typeof raw === 'number' && Number.isFinite(raw)) {
      return round(raw, 1);
    }
    if (typeof raw === 'string') {
      const parsed = Number(raw);
      if (Number.isFinite(parsed)) {
        return round(parsed, 1);
      }
    }
  }
  return fallback;
}

function readBodyShape(source: Record<string, unknown>, fallback: BodyShape, keys = ['bodyShape', 'shape']): BodyShape {
  for (const key of keys) {
    const raw = source[key];
    if (typeof raw === 'string') {
      const normalized = raw.trim().toUpperCase();
      if (normalized === 'X' || normalized === 'H' || normalized === 'A' || normalized === 'O') {
        return normalized;
      }
    }
  }
  return fallback;
}

function readLegRatio(source: Record<string, unknown>, fallback: LegRatio): LegRatio {
  const raw = source.legRatio;
  if (typeof raw === 'string') {
    const normalized = raw.trim().toLowerCase();
    if (normalized === 'short' || normalized === 'regular' || normalized === 'long') {
      return normalized;
    }
  }
  return fallback;
}

function readSize(source: Record<string, unknown>, keys: string[], fallback: SizeLevel): SizeLevel {
  for (const key of keys) {
    const raw = source[key];
    if (typeof raw === 'string') {
      const normalized = raw.trim().toUpperCase();
      if (normalized === 'XS' || normalized === 'S' || normalized === 'M' || normalized === 'L' || normalized === 'XL') {
        return normalized;
      }
    }
  }
  return fallback;
}

function resetDialogForm() {
  dialogForm.name = '';
  dialogForm.photoUrl = '';
  dialogForm.styleTags = [];
  dialogForm.body = createDefaultBodyForm();
}

function beforeImageUpload(file: File): boolean {
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error(t('members.imageTypeError'));
    return false;
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error(t('members.imageSizeError'));
    return false;
  }
  return true;
}

async function uploadMemberPhoto(options: UploadRequestOptions) {
  uploadingPhoto.value = true;
  try {
    const response = await uploadFile(options.file as File, 'member');
    dialogForm.photoUrl = response.url;
    options.onSuccess?.(response as never);
    ElMessage.success(t('members.uploadSuccess'));
  } catch (error) {
    options.onError?.(error as never);
    ElMessage.error(t('members.uploadFailed'));
  } finally {
    uploadingPhoto.value = false;
  }
}

function formatNumber(value: number): string {
  return Number.isInteger(value) ? String(value) : value.toFixed(1);
}

function formatBodySummary(bodyData: string): string {
  const profile = parseBodyProfile(bodyData);
  const m = profile.measurements;
  const labels = tm('members.bodySummaryLabels') as { shoulder: string; bust: string; waist: string; hip: string };
  return `${formatNumber(m.heightCm)}cm/${formatNumber(m.weightKg)}kg, `
      + `${labels.shoulder}${formatNumber(m.shoulderWidthCm)} `
      + `${labels.bust}${formatNumber(m.bustCm)}-${labels.waist}${formatNumber(m.waistCm)}-${labels.hip}${formatNumber(m.hipCm)}, `
      + `${m.bodyShape}, ${m.legRatio}, ${m.topSize}/${m.bottomSize}`;
}

async function load() {
  try {
    const data = await fetchMembers(0, 100);
    members.value = data.items;
  } catch (error) {
    ElMessage.error(t('members.loadFailed'));
  }
}

function openCreate() {
  dialogMode.value = 'create';
  editingId.value = null;
  resetDialogForm();
  dialogVisible.value = true;
}

function openEdit(row: MemberItem) {
  const profile = parseBodyProfile(row.bodyData);
  dialogMode.value = 'edit';
  editingId.value = row.id;
  dialogForm.name = row.name;
  dialogForm.photoUrl = row.photoUrl ?? '';
  dialogForm.styleTags = splitStyleTags(row.styleTags ?? '');
  dialogForm.body = {
    heightCm: profile.measurements.heightCm,
    weightKg: profile.measurements.weightKg,
    shoulderWidthCm: profile.measurements.shoulderWidthCm,
    bustCm: profile.measurements.bustCm,
    waistCm: profile.measurements.waistCm,
    hipCm: profile.measurements.hipCm,
    bodyShape: profile.measurements.bodyShape,
    legRatio: profile.measurements.legRatio,
    topSize: profile.measurements.topSize,
    bottomSize: profile.measurements.bottomSize
  };
  dialogVisible.value = true;
}

function openDetail(row: MemberItem) {
  detailName.value = row.name;
  detailProfile.value = parseBodyProfile(row.bodyData);
  detailVisible.value = true;
}

function validateCoreFields(): boolean {
  if (dialogForm.body.heightCm == null
      || dialogForm.body.weightKg == null
      || dialogForm.body.shoulderWidthCm == null
      || dialogForm.body.bustCm == null
      || dialogForm.body.waistCm == null
      || dialogForm.body.hipCm == null
      || !dialogForm.body.topSize
      || !dialogForm.body.bottomSize) {
    ElMessage.warning(t('members.validateCore'));
    return false;
  }
  return true;
}

function serializeBodyData(): string {
  return JSON.stringify({
    version: 2,
    measurements: {
      heightCm: dialogForm.body.heightCm,
      weightKg: dialogForm.body.weightKg,
      shoulderWidthCm: dialogForm.body.shoulderWidthCm,
      bustCm: dialogForm.body.bustCm,
      waistCm: dialogForm.body.waistCm,
      hipCm: dialogForm.body.hipCm,
      bodyShape: dialogForm.body.bodyShape,
      legRatio: dialogForm.body.legRatio,
      topSize: dialogForm.body.topSize,
      bottomSize: dialogForm.body.bottomSize
    }
  });
}

async function submitDialog() {
  if (!dialogForm.name.trim()) {
    ElMessage.warning(t('members.validateName'));
    return;
  }
  if (!validateCoreFields()) {
    return;
  }

  submitting.value = true;
  const payload = {
    name: dialogForm.name.trim(),
    photoUrl: dialogForm.photoUrl || '',
    bodyData: serializeBodyData(),
    styleTags: dialogForm.styleTags.join(',')
  };

  try {
    if (dialogMode.value === 'create') {
      await createMember(payload);
      ElMessage.success(t('members.createSuccess'));
    } else if (editingId.value) {
      await updateMember(editingId.value, payload);
      ElMessage.success(t('members.updateSuccess'));
    }
    dialogVisible.value = false;
    await load();
  } catch (error) {
    ElMessage.error(dialogMode.value === 'create' ? t('members.createFailed') : t('members.updateFailed'));
  } finally {
    submitting.value = false;
  }
}

async function remove(id: number) {
  try {
    await deleteMember(id);
    ElMessage.success(t('members.deleteSuccess'));
    await load();
  } catch (error) {
    ElMessage.error(t('members.deleteFailed'));
  }
}

onMounted(async () => {
  try {
    await load();
  } catch (error) {
    // Handled in load()
  }
});
</script>

<style scoped>
.member-photo-thumb {
  width: 56px;
  height: 78px;
  border-radius: 8px;
  display: block;
  background: #f6f7f9;
}

.member-photo-preview {
  margin-top: 10px;
  width: 160px;
  height: 240px;
  border-radius: 8px;
  display: block;
  background: #f6f7f9;
}

.image-empty {
  color: #909399;
}
</style>
