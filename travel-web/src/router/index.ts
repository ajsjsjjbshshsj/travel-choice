import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('@/layout/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'Dashboard',
        meta: { title: '数据看板' },
        component: () => import('@/views/Dashboard.vue')
      },
      {
        path: 'recommend',
        name: 'RecommendPage',
        meta: { title: '智能推荐' },
        component: () => import('@/views/RecommendPage.vue')
      },
      {
        path: 'result',
        name: 'RecommendResult',
        meta: { title: '推荐结果' },
        component: () => import('@/views/RecommendResult.vue')
      },
      {
        path: 'destinations',
        name: 'DestinationList',
        meta: { title: '目的地列表' },
        component: () => import('@/views/DestinationList.vue')
      },
      {
        path: 'destinations/:code',
        name: 'DestinationDetail',
        meta: { title: '目的地详情' },
        component: () => import('@/views/DestinationDetail.vue')
      },
      {
        path: 'data-collect',
        name: 'DataCollect',
        meta: { title: '数据采集' },
        component: () => import('@/views/DataCollect.vue')
      },
      {
        path: 'etl-monitor',
        name: 'EtlJobMonitor',
        meta: { title: 'ETL 监控' },
        component: () => import('@/views/EtlJobMonitor.vue')
      },
      {
        path: 'data-quality',
        name: 'DataQuality',
        meta: { title: '数据质量' },
        component: () => import('@/views/DataQuality.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
