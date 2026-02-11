import { createRouter, createWebHistory } from 'vue-router';
import AppLayout from '@/layout/AppLayout.vue';
import LoginView from '@/views/LoginView.vue';
import DashboardView from '@/views/DashboardView.vue';
import MembersView from '@/views/MembersView.vue';
import ClothingView from '@/views/ClothingView.vue';
import MatchStudioView from '@/views/MatchStudioView.vue';
import TaskCenterView from '@/views/TaskCenterView.vue';
import HistoryView from '@/views/HistoryView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { public: true } },
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', component: DashboardView },
        { path: 'members', component: MembersView },
        { path: 'clothing', component: ClothingView },
        { path: 'match', component: MatchStudioView },
        { path: 'tasks', component: TaskCenterView },
        { path: 'history', component: HistoryView }
      ]
    }
  ]
});

router.beforeEach((to) => {
  const accessToken = localStorage.getItem('fashion_access_token');
  if (to.meta.public) {
    return true;
  }
  if (!accessToken) {
    return '/login';
  }
  return true;
});

export default router;
