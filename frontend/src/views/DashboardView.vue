<template>
  <section class="page-grid">
    <div class="panel stat-card">
      <p>{{ $t('dashboard.totalMembers') }}</p>
      <h3>{{ memberTotal }}</h3>
    </div>
    <div class="panel stat-card">
      <p>{{ $t('dashboard.totalClothing') }}</p>
      <h3>{{ clothingTotal }}</h3>
    </div>
    <div class="panel stat-card">
      <p>{{ $t('dashboard.recentTasks') }}</p>
      <h3>{{ recentTasks.length }}</h3>
    </div>

    <div class="panel wide">
      <div class="section-head">
        <h2>{{ $t('dashboard.recentMatchTasks') }}</h2>
        <el-button size="small" @click="load">{{ $t('common.refresh') }}</el-button>
      </div>
      <el-table :data="recentTasks" style="width: 100%">
        <el-table-column prop="taskId" :label="$t('common.taskId')" min-width="260" />
        <el-table-column :label="$t('common.member')" width="140">
          <template #default="scope">
            {{ getMemberName(scope.row.memberId) }}
          </template>
        </el-table-column>
        <el-table-column prop="scene" :label="$t('common.scene')" width="140" />
        <el-table-column prop="status" :label="$t('common.status')" width="120" />
        <el-table-column prop="createdAt" :label="$t('common.createdAt')" min-width="180" />
      </el-table>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { fetchMembers } from '@/api/member';
import { fetchAllClothing } from '@/api/clothing';
import { fetchTaskList } from '@/api/match';
import type { MatchTaskSummary } from '@/types/domain';

const { t } = useI18n();

const memberTotal = ref(0);
const clothingTotal = ref(0);
const recentTasks = ref<MatchTaskSummary[]>([]);
const memberNameMap = ref<Record<number, string>>({});

function getMemberName(memberId: number): string {
  return memberNameMap.value[memberId] ?? `ID:${memberId}`;
}

async function load() {
  try {
    const [members, clothing, tasks] = await Promise.all([
      fetchMembers(0, 100),
      fetchAllClothing(0, 1),
      fetchTaskList(undefined, 0, 6)
    ]);
    memberTotal.value = members.total;
    clothingTotal.value = clothing.total;
    recentTasks.value = tasks.items;
    memberNameMap.value = members.items.reduce<Record<number, string>>((acc, member) => {
      acc[member.id] = member.name;
      return acc;
    }, {});
  } catch (error) {
    ElMessage.error(t('dashboard.loadFailed'));
  }
}

onMounted(load);
</script>
