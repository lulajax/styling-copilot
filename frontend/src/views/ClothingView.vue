<template>
  <section class="panel">
    <div class="section-head">
      <h2>{{ $t('clothing.title') }}</h2>
      <div class="head-actions">
        <el-button type="primary" @click="openCreate">{{ $t('clothing.addClothing') }}</el-button>
        <el-button @click="load">{{ $t('common.refresh') }}</el-button>
      </div>
    </div>

    <el-table :data="clothing" style="width: 100%">
      <el-table-column prop="id" :label="$t('common.id')" width="90" />
      <el-table-column :label="$t('common.image')" width="110">
        <template #default="scope">
          <el-image
            v-if="scope.row.imageUrl"
            :src="scope.row.imageUrl"
            style="width: 56px; height: 56px; border-radius: 8px"
            fit="cover"
          />
          <span v-else class="image-empty">{{ $t('clothing.noImage') }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" :label="$t('common.name')" min-width="180" />
      <el-table-column :label="$t('members.styleTags')" min-width="240">
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
      <el-table-column :label="$t('common.actions')" width="300">
        <template #default="scope">
          <el-button size="small" @click="openEdit(scope.row)">{{ $t('common.edit') }}</el-button>
          <el-button size="small" @click="toggle(scope.row)">
            {{ scope.row.status === 'ON_SHELF' ? $t('clothing.setOff') : $t('clothing.setOn') }}
          </el-button>
          <el-button size="small" type="danger" @click="remove(scope.row.id)">{{ $t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? $t('clothing.addClothing') : $t('clothing.editClothing')" width="620px">
      <el-form label-position="top">
        <el-form-item :label="$t('common.name')">
          <el-input v-model="dialogForm.name" :placeholder="$t('clothing.clothingName')" />
        </el-form-item>

        <el-form-item :label="$t('common.image')">
          <div class="upload-area">
            <el-upload
              :show-file-list="false"
              :http-request="uploadClothingImage"
              :before-upload="beforeImageUpload"
              accept=".jpg,.jpeg,.png,.webp"
            >
              <el-button :loading="uploadingImage">{{ $t('clothing.uploadImage') }}</el-button>
            </el-upload>
            <el-button v-if="dialogForm.imageUrl" link type="danger" @click="dialogForm.imageUrl = ''">{{ $t('common.remove') }}</el-button>
          </div>
          <el-image
            v-if="dialogForm.imageUrl"
            :src="dialogForm.imageUrl"
            style="margin-top: 10px; width: 96px; height: 96px; border-radius: 8px"
            fit="cover"
          />
        </el-form-item>

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

        <el-form-item :label="$t('common.type')">
          <el-select v-model="dialogForm.clothingType" style="width: 100%">
            <el-option
              v-for="option in clothingTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="$t('common.status')">
          <el-select v-model="dialogForm.status" style="width: 200px">
            <el-option :label="$t('clothingStatus.ON_SHELF')" value="ON_SHELF" />
            <el-option :label="$t('clothingStatus.OFF_SHELF')" value="OFF_SHELF" />
          </el-select>
        </el-form-item>

        <h4 class="sub-title">{{ $t('clothing.sizeInfo') }}</h4>

        <!-- TOP size fields -->
        <template v-if="dialogForm.clothingType === 'TOP' || dialogForm.clothingType === 'ONE_PIECE' || dialogForm.clothingType === 'SET'">
          <el-row :gutter="12">
            <el-col :span="8">
              <el-form-item :label="$t('members.shoulderWidth')">
                <el-input-number v-model="dialogForm.sizeData.shoulderWidthCm" :step="0.5" :min="20" :max="60" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="$t('members.bust')">
                <el-input-number v-model="dialogForm.sizeData.bustCm" :step="0.5" :min="60" :max="150" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="$t('clothing.topSize')">
                <el-select v-model="dialogForm.sizeData.topSize" style="width: 100%">
                  <el-option v-for="size in sizeOptions" :key="size" :label="size" :value="size" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="12">
            <el-col :span="8">
              <el-form-item :label="$t('clothing.length')">
                <el-input-number v-model="dialogForm.sizeData.lengthCm" :step="0.5" :min="30" :max="120" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="$t('clothing.sleeveLength')">
                <el-input-number v-model="dialogForm.sizeData.sleeveLengthCm" :step="0.5" :min="0" :max="80" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <!-- BOTTOM size fields -->
        <template v-if="dialogForm.clothingType === 'BOTTOM' || dialogForm.clothingType === 'ONE_PIECE' || dialogForm.clothingType === 'SET'">
          <el-row :gutter="12">
            <el-col :span="8">
              <el-form-item :label="$t('members.waist')">
                <el-input-number v-model="dialogForm.sizeData.waistCm" :step="0.5" :min="40" :max="120" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="$t('members.hip')">
                <el-input-number v-model="dialogForm.sizeData.hipCm" :step="0.5" :min="60" :max="150" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="$t('clothing.bottomSize')">
                <el-select v-model="dialogForm.sizeData.bottomSize" style="width: 100%">
                  <el-option v-for="size in sizeOptions" :key="size" :label="size" :value="size" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="12">
            <el-col :span="8">
              <el-form-item :label="$t('clothing.length')">
                <el-input-number v-model="dialogForm.sizeData.lengthCm" :step="0.5" :min="20" :max="120" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="$t('clothing.inseam')">
                <el-input-number v-model="dialogForm.sizeData.inseamCm" :step="0.5" :min="20" :max="110" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDialog">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import type { UploadRequestOptions } from 'element-plus';
import {
  createClothing,
  deleteClothing,
  fetchAllClothing,
  updateClothing,
  updateClothingStatus
} from '@/api/clothing';
import { uploadFile } from '@/api/file';
import { STYLE_TAG_OPTIONS } from '@/constants/styleTags';
import { formatLocalizedStyleTags, splitStyleTags } from '@/utils/styleTags';
import type { ClothingItem, ClothingType, ClothingSizeData } from '@/types/domain';

const { t } = useI18n();

type DialogMode = 'create' | 'edit';

const clothing = ref<ClothingItem[]>([]);
const dialogVisible = ref(false);
const dialogMode = ref<DialogMode>('create');
const editingId = ref<number | null>(null);
const originalStatus = ref<'ON_SHELF' | 'OFF_SHELF'>('ON_SHELF');
const submitting = ref(false);
const uploadingImage = ref(false);
const styleTagOptions = STYLE_TAG_OPTIONS;
const clothingTypeOptions: Array<{ label: string; value: ClothingType }> = [
  { label: 'TOP', value: 'TOP' },
  { label: 'BOTTOM', value: 'BOTTOM' }
];

const sizeOptions = ['XS', 'S', 'M', 'L', 'XL'] as const;

const dialogForm = reactive({
  name: '',
  imageUrl: '',
  styleTags: [] as string[],
  clothingType: 'TOP' as ClothingType,
  status: 'ON_SHELF' as 'ON_SHELF' | 'OFF_SHELF',
  sizeData: {
    shoulderWidthCm: null as number | null,
    bustCm: null as number | null,
    waistCm: null as number | null,
    hipCm: null as number | null,
    lengthCm: null as number | null,
    sleeveLengthCm: null as number | null,
    inseamCm: null as number | null,
    topSize: '' as string,
    bottomSize: '' as string
  }
});

function resetDialogForm() {
  dialogForm.name = '';
  dialogForm.imageUrl = '';
  dialogForm.styleTags = [];
  dialogForm.clothingType = 'TOP';
  dialogForm.status = 'ON_SHELF';
  dialogForm.sizeData = {
    shoulderWidthCm: null,
    bustCm: null,
    waistCm: null,
    hipCm: null,
    lengthCm: null,
    sleeveLengthCm: null,
    inseamCm: null,
    topSize: '',
    bottomSize: ''
  };
}

function normalizeWritableType(type: ClothingType): ClothingType {
  return type === 'BOTTOM' ? 'BOTTOM' : 'TOP';
}

function beforeImageUpload(file: File): boolean {
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error(t('clothing.imageTypeError'));
    return false;
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error(t('clothing.imageSizeError'));
    return false;
  }
  return true;
}

async function uploadClothingImage(options: UploadRequestOptions) {
  uploadingImage.value = true;
  try {
    const response = await uploadFile(options.file as File, 'clothing');
    dialogForm.imageUrl = response.url;
    options.onSuccess?.(response as never);
    ElMessage.success(t('clothing.uploadSuccess'));
  } catch (error) {
    options.onError?.(error as never);
    ElMessage.error(t('clothing.uploadFailed'));
  } finally {
    uploadingImage.value = false;
  }
}

async function load() {
  try {
    const data = await fetchAllClothing(0, 100);
    clothing.value = data.items;
  } catch (error) {
    ElMessage.error(t('clothing.loadFailed'));
  }
}

function openCreate() {
  dialogMode.value = 'create';
  editingId.value = null;
  resetDialogForm();
  dialogVisible.value = true;
}

function parseSizeData(sizeData?: string | ClothingItem['sizeData']) {
  if (!sizeData) return {};
  if (typeof sizeData === 'object') return sizeData;
  try {
    return JSON.parse(sizeData);
  } catch {
    return {};
  }
}

function openEdit(row: ClothingItem) {
  dialogMode.value = 'edit';
  editingId.value = row.id;
  originalStatus.value = row.status;
  dialogForm.name = row.name;
  dialogForm.imageUrl = row.imageUrl ?? '';
  dialogForm.styleTags = splitStyleTags(row.styleTags ?? '');
  dialogForm.clothingType = normalizeWritableType(row.clothingType);
  dialogForm.status = row.status;

  const parsed = parseSizeData(row.sizeData) as Partial<typeof dialogForm.sizeData>;
  dialogForm.sizeData.shoulderWidthCm = parsed.shoulderWidthCm ?? null;
  dialogForm.sizeData.bustCm = parsed.bustCm ?? null;
  dialogForm.sizeData.waistCm = parsed.waistCm ?? null;
  dialogForm.sizeData.hipCm = parsed.hipCm ?? null;
  dialogForm.sizeData.lengthCm = parsed.lengthCm ?? null;
  dialogForm.sizeData.sleeveLengthCm = parsed.sleeveLengthCm ?? null;
  dialogForm.sizeData.inseamCm = parsed.inseamCm ?? null;
  dialogForm.sizeData.topSize = parsed.topSize ?? '';
  dialogForm.sizeData.bottomSize = parsed.bottomSize ?? '';

  dialogVisible.value = true;
}

function buildSizeDataJson(): string {
  const sd = dialogForm.sizeData;
  const obj: Record<string, string | number | undefined> = {};

  if (sd.shoulderWidthCm != null) obj.shoulderWidthCm = sd.shoulderWidthCm;
  if (sd.bustCm != null) obj.bustCm = sd.bustCm;
  if (sd.waistCm != null) obj.waistCm = sd.waistCm;
  if (sd.hipCm != null) obj.hipCm = sd.hipCm;
  if (sd.lengthCm != null) obj.lengthCm = sd.lengthCm;
  if (sd.sleeveLengthCm != null) obj.sleeveLengthCm = sd.sleeveLengthCm;
  if (sd.inseamCm != null) obj.inseamCm = sd.inseamCm;
  if (sd.topSize) obj.topSize = sd.topSize;
  if (sd.bottomSize) obj.bottomSize = sd.bottomSize;

  return Object.keys(obj).length > 0 ? JSON.stringify(obj) : '';
}

async function submitDialog() {
  if (!dialogForm.name.trim()) {
    ElMessage.warning(t('members.validateName'));
    return;
  }
  if (!dialogForm.clothingType) {
    ElMessage.warning(t('common.selectPlaceholder'));
    return;
  }

  submitting.value = true;
  const sizeDataJson = buildSizeDataJson();
  const payload = {
    name: dialogForm.name.trim(),
    imageUrl: dialogForm.imageUrl || '',
    styleTags: dialogForm.styleTags.join(','),
    clothingType: dialogForm.clothingType,
    sizeData: sizeDataJson || undefined
  };

  try {
    if (dialogMode.value === 'create') {
      await createClothing({
        ...payload,
        status: dialogForm.status
      });
      ElMessage.success(t('clothing.createSuccess'));
    } else if (editingId.value) {
      await updateClothing(editingId.value, payload);
      if (dialogForm.status !== originalStatus.value) {
        await updateClothingStatus(editingId.value, dialogForm.status);
      }
      ElMessage.success(t('clothing.updateSuccess'));
    }
    dialogVisible.value = false;
    await load();
  } catch (error) {
    ElMessage.error(dialogMode.value === 'create' ? t('clothing.createFailed') : t('clothing.updateFailed'));
  } finally {
    submitting.value = false;
  }
}

async function toggle(item: ClothingItem) {
  try {
    const next = item.status === 'ON_SHELF' ? 'OFF_SHELF' : 'ON_SHELF';
    await updateClothingStatus(item.id, next);
    ElMessage.success(t('clothing.toggleSuccess'));
    await load();
  } catch (error) {
    ElMessage.error(t('clothing.toggleFailed'));
  }
}

async function remove(id: number) {
  try {
    await deleteClothing(id);
    ElMessage.success(t('clothing.deleteSuccess'));
    await load();
  } catch (error) {
    ElMessage.error(t('clothing.deleteFailed'));
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
