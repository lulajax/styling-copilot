<template>
  <section class="login-page">
    <div class="login-card">
      <h2>{{ $t('login.title') }}</h2>
      <p>{{ $t('login.subtitle') }}</p>
      <el-form label-position="top" @submit.prevent>
        <el-form-item :label="$t('login.username')">
          <el-input v-model="username" autocomplete="username" />
        </el-form-item>
        <el-form-item :label="$t('login.password')">
          <el-input v-model="password" type="password" autocomplete="current-password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">{{ $t('common.signIn') }}</el-button>
      </el-form>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { login } from '@/api/auth';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const authStore = useAuthStore();
const { t } = useI18n();

const username = ref('stylist');
const password = ref('stylist123');
const loading = ref(false);

async function submit() {
  loading.value = true;
  try {
    const data = await login(username.value, password.value);
    authStore.setTokens(username.value, data.accessToken, data.refreshToken);
    ElMessage.success(t('login.success'));
    await router.push('/dashboard');
  } catch (error) {
    ElMessage.error(t('login.failed'));
  } finally {
    loading.value = false;
  }
}
</script>
