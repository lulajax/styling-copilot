<template>
  <div class="layout-root">
    <aside class="sidebar">
      <div class="brand">
        <h1>Styling Copilot</h1>
        <p>AI Workbench</p>
      </div>

      <nav class="nav-list">
        <RouterLink v-for="item in menus" :key="item.path" :to="item.path" class="nav-item">
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="operator">
        <el-select v-model="currentLocale" size="small" style="width: 100px" @change="onLocaleChange">
          <el-option label="English" value="en" />
          <el-option label="中文" value="zh" />
          <el-option label="한국어" value="ko" />
        </el-select>
        <span>{{ authStore.username || 'stylist' }}</span>
        <button type="button" @click="logout">{{ $t('nav.logout') }}</button>
      </div>
    </aside>

    <section class="content">
      <RouterView />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { useAuthStore } from '@/stores/auth';
import { setLocale, getLocale, type Locale } from '@/i18n';

const router = useRouter();
const authStore = useAuthStore();
const { t, locale } = useI18n();

const currentLocale = computed({
  get: () => getLocale(),
  set: (val: Locale) => setLocale(val)
});

const menus = computed(() => [
  { label: t('nav.dashboard'), path: '/dashboard' },
  { label: t('nav.members'), path: '/members' },
  { label: t('nav.clothing'), path: '/clothing' },
  { label: t('nav.matchStudio'), path: '/match' },
  { label: t('nav.taskCenter'), path: '/tasks' },
  { label: t('nav.history'), path: '/history' }
]);

function onLocaleChange(val: Locale) {
  setLocale(val);
  locale.value = val;
  // Reload to apply Element Plus locale
  window.location.reload();
}

function logout() {
  authStore.clear();
  router.push('/login');
}
</script>
