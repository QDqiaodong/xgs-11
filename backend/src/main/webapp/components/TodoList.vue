<template>
  <div class="todo-app-container animate-entrance">
    <div class="app-sidebar glass">
      <div class="sidebar-top">
        <div class="app-logo">
          <span class="logo-icon">🎯</span>
          <span class="logo-text">FocusFlow</span>
        </div>
      </div>
      
      <nav class="sidebar-nav">
        <div class="nav-section">
          <p class="section-title">页面导航</p>
          <div class="nav-buttons">
            <button class="nav-btn nav-btn-active">
              <span class="nav-icon">📋</span>
              <span>任务列表</span>
            </button>
            <button @click="$emit('goto-schedule')" class="nav-btn">
              <span class="nav-icon">📅</span>
              <span>日程视图</span>
            </button>
          </div>
        </div>

        <div class="nav-section">
          <p class="section-title">任务控制</p>
          <div class="sidebar-controls">
            <TaskFilter v-model="filters" @add="showAddModal = true" />
          </div>
        </div>

        <div class="nav-section">
          <p class="section-title">排列视图</p>
          <div class="sort-controls">
            <select v-model="sortBy" class="input input-sm">
              <option value="created_at">创建时间</option>
              <option value="due_date">截止日期</option>
              <option value="priority">优先级</option>
            </select>
            <button @click="sortOrder = sortOrder === 'asc' ? 'desc' : 'asc'" class="btn-sort-circle" :title="sortOrder === 'asc' ? '升序' : '降序'">
              {{ sortOrder === 'asc' ? '↑' : '↓' }}
            </button>
          </div>
        </div>
      </nav>

      <div class="sidebar-footer" v-if="currentUser">
        <div class="user-strip">
          <div class="avatar-circle">{{ currentUser.username[0].toUpperCase() }}</div>
          <div class="user-info">
            <span class="u-name">{{ currentUser.username }}</span>
            <span class="u-badge">专业版</span>
          </div>
          <button @click="$emit('logout')" class="btn-logout-icon" title="安全退出">
            <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round" class="icon-logout">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
              <polyline points="16 17 21 12 16 7"></polyline>
              <line x1="21" y1="12" x2="9" y2="12"></line>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <main class="app-viewport">
      <div class="viewport-header">
        <div class="welcome-block">
          <h1>工作台首页</h1>
          <p>今天是有效的一天，{{ currentUser?.username }}。</p>
        </div>
        <div class="quick-actions">
          <button @click="showAddModal = true" class="btn btn-primary">
            <span>+</span> 创建新任务
          </button>
        </div>
      </div>

      <div class="viewport-canvas">
        <section class="today-focus-section">
          <TodayFocus :tasks="tasks" :currentUser="currentUser" @update="fetchTasks" />
        </section>

        <section class="overview-stats">
          <TaskStats :tasks="tasks" :currentUser="currentUser" @filter="handleStatFilter" />
        </section>

        <section class="task-board">
          <div class="board-header">
            <div class="board-header-left">
              <h2>待办清单 <span class="count-chip">{{ filteredTasks.length }}</span></h2>
              <button v-if="!batchMode" @click="enterBatchMode" class="btn-batch-toggle" title="批量操作">
                <span>☑️</span> 批量操作
              </button>
              <button v-else @click="exitBatchMode" class="btn-batch-cancel" title="取消批量操作">
                <span>✕</span> 取消
              </button>
            </div>
          </div>

          <div v-if="batchMode" class="batch-action-bar glass">
            <div class="batch-select-all">
              <label class="batch-checkbox batch-checkbox-lg">
                <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll" :disabled="filteredTasks.length === 0">
                <span class="batch-checkbox-box">
                  <span class="batch-checkbox-tick"></span>
                </span>
              </label>
              <span class="batch-select-text">全选 (已选 {{ selectedTaskIds.length }} 项)</span>
            </div>
            <div class="batch-actions">
              <button @click="openBatchModal('priority')" :disabled="selectedTaskIds.length === 0" class="btn-batch-op">
                <span>🚩</span> 调整优先级
              </button>
              <button @click="openBatchModal('status')" :disabled="selectedTaskIds.length === 0" class="btn-batch-op">
                <span>✅</span> 完成状态
              </button>
              <button @click="openBatchModal('dueDate')" :disabled="selectedTaskIds.length === 0" class="btn-batch-op">
                <span>📅</span> 截止日期
              </button>
              <button @click="openBatchModal('assignee')" :disabled="selectedTaskIds.length === 0" class="btn-batch-op">
                <span>👤</span> 指派受托人
              </button>
            </div>
          </div>

          <div class="task-grid-feed">
            <transition-group name="fade">
              <TodoItem 
                v-for="task in paginatedTasks" 
                :key="task.id" 
                :task="task" 
                :currentUser="currentUser"
                :batchMode="batchMode"
                :selected="selectedTaskIds.includes(task.id)"
                @update="fetchTasks"
                @delete="confirmDelete"
                @toggle-select="toggleTaskSelect"
                @request-complete="requestComplete"
                @show-detail="showDetail"
              />
            </transition-group>
            
            <div v-if="filteredTasks.length === 0" class="empty-view">
              <div class="empty-art">✨</div>
              <h3>所有任务已清零</h3>
              <p>点击上方按钮开启您的第一个高效任务。</p>
            </div>
          </div>

          <footer class="pagination-bar" v-if="filteredTasks.length > pageSize">
            <div class="page-stat">
              第 {{ currentPage }} / {{ totalPages }} 页
            </div>
            <div class="page-nav">
              <button @click="currentPage--" :disabled="currentPage === 1" class="btn-p-nav">上一页</button>
              <button @click="currentPage++" :disabled="currentPage === totalPages" class="btn-p-nav">下一页</button>
            </div>
          </footer>
        </section>
      </div>
    </main>

    <!-- Modals -->
    <Teleport to="body">
      <div v-if="showAddModal" class="modal-overlay animate-entrance" @click.self="showAddModal = false">
        <div class="modal glass">
          <div class="modal-header-lite">
            <h3>新任务发布</h3>
            <button @click="showAddModal = false" class="btn-x">×</button>
          </div>
          <form @submit.prevent="handleAddTask" class="premium-form-lite">
            <div class="form-group-lite">
              <label>任务描述</label>
              <textarea v-model="newTask.text" class="input input-text-area" placeholder="请输入任务详情..." required></textarea>
            </div>
            
            <div class="form-row-lite">
              <div class="form-group-lite">
                <label>优先级</label>
                <select v-model="newTask.priority" class="input">
                  <option value="high">紧急</option>
                  <option value="medium">普通</option>
                  <option value="low">较低</option>
                </select>
              </div>
              <div class="form-group-lite">
                <label>截止日</label>
                <input type="datetime-local" v-model="newTask.due_date" class="input">
              </div>
            </div>

            <div class="form-group-lite">
              <label>执行指派</label>
              <UserSelect v-model="newTask.assigneeId" />
            </div>

            <div class="modal-btns">
              <button type="button" @click="showAddModal = false" class="btn-cancel">取消</button>
              <button type="submit" class="btn btn-primary">发布</button>
            </div>
          </form>
        </div>
      </div>

      <div v-if="deleteConfirmId" class="modal-overlay" @click.self="deleteConfirmId = null">
        <div class="modal glass mini-alert text-center">
          <div class="warning-icon">🫵</div>
          <h3>确定要删除吗?</h3>
          <p class="text-sub">此操作无法撤销，请谨慎操作。</p>
          <div class="modal-btns-center">
            <button @click="deleteConfirmId = null" class="btn-cancel">保留</button>
            <button @click="handleDelete" class="btn btn-danger">确认删除</button>
          </div>
        </div>
      </div>

      <div v-if="showBatchModal" class="modal-overlay animate-entrance" @click.self="closeBatchModal">
        <div class="modal glass batch-modal">
          <div class="modal-header-lite">
            <h3>批量{{ batchOperationLabel }}</h3>
            <button @click="closeBatchModal" class="btn-x">×</button>
          </div>
          
          <div class="batch-summary-card">
            <div class="summary-icon-box">
              <span class="summary-icon">{{ batchOperationIcon }}</span>
            </div>
            <div class="summary-info">
              <p class="summary-title">即将对以下任务执行操作</p>
              <p class="summary-count">共 <strong>{{ selectedTaskIds.length }}</strong> 个任务</p>
            </div>
          </div>

          <div class="batch-form">
            <div v-if="batchOperation === 'priority'" class="form-group-lite">
              <label>设置优先级</label>
              <select v-model="batchFormData.priority" class="input">
                <option value="high">紧急</option>
                <option value="medium">普通</option>
                <option value="low">较低</option>
              </select>
            </div>

            <div v-if="batchOperation === 'status'" class="form-group-lite">
              <label>设置状态</label>
              <select v-model="batchFormData.status" class="input">
                <option value="completed">标记为已完成</option>
                <option value="pending">标记为未完成</option>
              </select>
            </div>

            <div v-if="batchOperation === 'dueDate'" class="form-group-lite">
              <label>设置截止日期</label>
              <input type="datetime-local" v-model="batchFormData.due_date" class="input">
              <p class="form-hint">留空则清除截止日期</p>
            </div>

            <div v-if="batchOperation === 'assignee'" class="form-group-lite">
              <label>指派受托人</label>
              <UserSelect v-model="batchFormData.assigneeId" />
            </div>
          </div>

          <div class="batch-preview-list">
            <p class="preview-title">影响任务预览</p>
            <div class="preview-tasks">
              <div v-for="task in selectedTasksPreview" :key="task.id" class="preview-task-item">
                <span class="preview-dot" :class="'dot-' + task.priority"></span>
                <span class="preview-text">{{ task.text.length > 40 ? task.text.substring(0, 40) + '...' : task.text }}</span>
              </div>
              <div v-if="selectedTaskIds.length > 5" class="preview-more">
                ... 还有 {{ selectedTaskIds.length - 5 }} 个任务
              </div>
            </div>
          </div>

          <div class="modal-btns">
            <button type="button" @click="closeBatchModal" class="btn-cancel">取消</button>
            <button @click="executeBatchOperation" :disabled="batchSubmitting" class="btn btn-primary">
              <span v-if="batchSubmitting" class="btn-loader"></span>
              <span v-else>确认执行</span>
            </button>
          </div>
        </div>
      </div>

      <OverdueReasonModal
        v-if="overdueTask"
        :task="overdueTask"
        @submit="submitOverdueReason"
        @cancel="overdueTask = null"
      />

      <TaskDetail
        v-if="detailTask"
        :task="detailTask"
        :currentUser="currentUser"
        @close="detailTask = null"
      />
    </Teleport>
  </div>
</template>

<script>
const { ref, onMounted, computed, reactive, inject, watch } = Vue;

export default {
  emits: ['logout', 'goto-schedule'],
  inject: ['showToast'],
  setup() {
    const tasks = ref([]);
    const currentUser = ref(UserAuth.user);
    const showAddModal = ref(false);
    const deleteConfirmId = ref(null);
    const showToast = inject('showToast');

    const overdueTask = ref(null);
    const detailTask = ref(null);

    const batchMode = ref(false);
    const selectedTaskIds = ref([]);
    const showBatchModal = ref(false);
    const batchOperation = ref('');
    const batchSubmitting = ref(false);
    const batchFormData = reactive({
      priority: 'medium',
      status: 'completed',
      due_date: '',
      assigneeId: null
    });

    const filters = ref({
      keyword: '',
      status: 'all',
      priority: 'all',
      role: 'all',
      dueDate: 'all',
      responsibility: 'all'
    });

    const newTask = reactive({
      text: '',
      priority: 'medium',
      assigneeId: null,
      due_date: ''
    });

    const sortBy = ref('created_at');
    const sortOrder = ref('desc');
    const currentPage = ref(1);
    const pageSize = 6;

    const fetchTasks = async () => {
      try {
        const res = await axios.get('/api/tasks');
        tasks.value = res.data;
      } catch (e) {
        let msg = '数据同步失败';
        if (e.response && e.response.status === 401) {
          msg = '登录已过期，请重新登录';
        }
        showToast(msg, 'danger');
      }
    };

    const validPriorities = ['high', 'medium', 'low'];
    const maxTextLength = 500;

    const handleAddTask = async () => {
      const text = newTask.text.trim();
      if (!text) {
        showToast('任务内容不能为空', 'danger');
        return;
      }
      if (text.length > maxTextLength) {
        showToast(`任务内容不能超过 ${maxTextLength} 个字符`, 'danger');
        return;
      }
      if (!validPriorities.includes(newTask.priority)) {
        showToast('优先级不合法', 'danger');
        return;
      }
      if (newTask.due_date && isNaN(new Date(newTask.due_date).getTime())) {
        showToast('截止时间不合法', 'danger');
        return;
      }
      try {
        const payload = {
            text: text,
            priority: newTask.priority,
            dueDate: null,
            assigneeId: null
        };
        
        if (newTask.due_date) {
            payload.dueDate = new Date(newTask.due_date).getTime();
        }
        
        if (newTask.assigneeId) {
            payload.assigneeId = parseInt(newTask.assigneeId, 10);
        }
        
        await axios.post('/api/tasks', payload);
        showToast('添加成功', 'success');
        showAddModal.value = false;
        newTask.text = '';
        newTask.due_date = '';
        newTask.assigneeId = null;
        fetchTasks();
      } catch (e) {
        let msg = '添加失败';
        if (e.response) {
          if (e.response.status === 401) {
            msg = '登录已过期，请重新登录';
          } else if (e.response.data && e.response.data.message) {
            msg = e.response.data.message;
          }
        }
        showToast(msg, 'danger');
      }
    };

    const confirmDelete = (id) => {
      deleteConfirmId.value = id;
    };

    const handleDelete = async () => {
      try {
        await axios.delete(`/api/tasks/${deleteConfirmId.value}`);
        showToast('已删除', 'success');
        deleteConfirmId.value = null;
        fetchTasks();
      } catch (e) {
        let msg = '删除失败';
        if (e.response) {
          if (e.response.status === 401) {
            msg = '登录已过期，请重新登录';
          } else if (e.response.data && e.response.data.message) {
            msg = e.response.data.message;
          }
        }
        showToast(msg, 'danger');
      }
    };

    const requestComplete = (task) => {
      overdueTask.value = task;
    };

    const submitOverdueReason = async (reason) => {
      const task = overdueTask.value;
      if (!task) return;
      try {
        await axios.put(`/api/tasks/${task.id}`, {
          ...task,
          completed: true,
          overdueReason: reason
        });
        showToast('任务已完成 🎉', 'success');
        overdueTask.value = null;
        fetchTasks();
      } catch (e) {
        let msg = '完成失败';
        if (e.response && e.response.status === 401) {
          msg = '登录已过期，请重新登录';
        } else if (e.response && e.response.data && e.response.data.message) {
          msg = e.response.data.message;
        }
        showToast(msg, 'danger');
      }
    };

    const showDetail = (task) => {
      detailTask.value = task;
    };

    const enterBatchMode = () => {
      batchMode.value = true;
      selectedTaskIds.value = [];
    };

    const exitBatchMode = () => {
      batchMode.value = false;
      selectedTaskIds.value = [];
    };

    const toggleTaskSelect = (taskId) => {
      const index = selectedTaskIds.value.indexOf(taskId);
      if (index === -1) {
        selectedTaskIds.value.push(taskId);
      } else {
        selectedTaskIds.value.splice(index, 1);
      }
    };

    const isAllSelected = computed(() => {
      if (filteredTasks.value.length === 0) return false;
      return filteredTasks.value.every(t => selectedTaskIds.value.includes(t.id));
    });

    const toggleSelectAll = () => {
      if (isAllSelected.value) {
        selectedTaskIds.value = [];
      } else {
        selectedTaskIds.value = filteredTasks.value.map(t => t.id);
      }
    };

    const batchOperationLabel = computed(() => {
      const labels = {
        priority: '调整优先级',
        status: '更新状态',
        dueDate: '设置截止日期',
        assignee: '指派受托人'
      };
      return labels[batchOperation.value] || '';
    });

    const batchOperationIcon = computed(() => {
      const icons = {
        priority: '🚩',
        status: '✅',
        dueDate: '📅',
        assignee: '👤'
      };
      return icons[batchOperation.value] || '📋';
    });

    const selectedTasksPreview = computed(() => {
      return tasks.value.filter(t => selectedTaskIds.value.includes(t.id)).slice(0, 5);
    });

    const openBatchModal = (operation) => {
      batchOperation.value = operation;
      batchFormData.priority = 'medium';
      batchFormData.status = 'completed';
      batchFormData.due_date = '';
      batchFormData.assigneeId = null;
      showBatchModal.value = true;
    };

    const closeBatchModal = () => {
      if (!batchSubmitting.value) {
        showBatchModal.value = false;
      }
    };

    const executeBatchOperation = async () => {
      if (selectedTaskIds.value.length === 0) {
        showToast('请先选择任务', 'danger');
        return;
      }

      batchSubmitting.value = true;
      try {
        let value = null;
        if (batchOperation.value === 'priority') {
          value = batchFormData.priority;
        } else if (batchOperation.value === 'status') {
          value = batchFormData.status;
        } else if (batchOperation.value === 'dueDate') {
          if (batchFormData.due_date) {
            value = new Date(batchFormData.due_date).getTime().toString();
          } else {
            value = '';
          }
        } else if (batchOperation.value === 'assignee') {
          value = batchFormData.assigneeId ? batchFormData.assigneeId.toString() : '';
        }

        const res = await axios.post('/api/tasks/batch', {
          taskIds: selectedTaskIds.value,
          operation: batchOperation.value,
          value: value
        });

        if (res.data && res.data.success) {
          showToast(`批量操作成功，共更新 ${res.data.updatedCount || res.data.totalCount} 个任务`, 'success');
          showBatchModal.value = false;
          exitBatchMode();
          fetchTasks();
        } else {
          showToast(res.data?.message || '批量操作失败', 'danger');
        }
      } catch (e) {
        let msg = '批量操作失败';
        if (e.response) {
          if (e.response.status === 401) {
            msg = '登录已过期，请重新登录';
          } else if (e.response.data && e.response.data.message) {
            msg = e.response.data.message;
          }
        }
        showToast(msg, 'danger');
      } finally {
        batchSubmitting.value = false;
      }
    };

    const filteredTasks = computed(() => {
      const f = filters.value;
      let result = tasks.value.filter(t => {
        const keyword = f.keyword || '';
        const matchKeyword = (t.text || '').toLowerCase().includes(keyword.toLowerCase());
        const matchStatus = f.status === 'all' || (f.status === 'completed' ? t.completed : !t.completed);
        const matchPriority = f.priority === 'all' || t.priority === f.priority;
        const matchRole = f.role === 'all' || (f.role === 'mine' ? t.userId === currentUser.value.id : t.assigneeId === currentUser.value.id);
        
        let matchDueDate = true;
        if (f.dueDate !== 'all') {
          if (f.dueDate === 'overdue') {
            matchDueDate = !t.completed && utils.isOverdue(t.dueDate);
          } else if (f.dueDate === 'dueToday') {
            matchDueDate = !t.completed && utils.isDueToday(t.dueDate);
          } else if (f.dueDate === 'within24h') {
            matchDueDate = !t.completed && utils.isDueWithin24hButNotToday(t.dueDate);
          } else if (f.dueDate === 'within3d') {
            matchDueDate = !t.completed && utils.isDueWithin3DaysButNot24h(t.dueDate);
          } else if (f.dueDate === 'normal') {
            matchDueDate = !t.completed && !utils.isOverdue(t.dueDate) && !utils.isDueWithin3Days(t.dueDate);
          }
        }
        
        let matchResponsibility = true;
        if (f.responsibility !== 'all') {
          const resp = utils.getTaskResponsibility(t, currentUser.value);
          matchResponsibility = resp === f.responsibility;
        }
        
        return matchKeyword && matchStatus && matchPriority && matchRole && matchDueDate && matchResponsibility;
      });

      result.sort((a, b) => {
        let valA, valB;
        
        if (sortBy.value === 'priority') {
            const weights = { high: 3, medium: 2, low: 1 };
            valA = weights[a.priority] || 0;
            valB = weights[b.priority] || 0;
        } else if (sortBy.value === 'due_date') {
            const aHas = a.dueDate != null;
            const bHas = b.dueDate != null;
            if (!aHas && !bHas) return 0;
            if (!aHas) return 1;
            if (!bHas) return -1;
            valA = new Date(a.dueDate).getTime();
            valB = new Date(b.dueDate).getTime();
        } else {
            valA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
            valB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        }

        if (valA === valB) return 0;
        if (sortOrder.value === 'asc') return valA > valB ? 1 : -1;
        return valA < valB ? 1 : -1;
      });

      return result;
    });

    const totalPages = computed(() => Math.max(1, Math.ceil(filteredTasks.value.length / pageSize)));
    const paginatedTasks = computed(() => {
        const start = (currentPage.value - 1) * pageSize;
        return filteredTasks.value.slice(start, start + pageSize);
    });

    const handleStatFilter = (filterInfo) => {
      const type = filterInfo.type;
      const dueDateTypes = ['all', 'completed', 'overdue', 'dueToday', 'within24h', 'within3d', 'normal'];
      const responsibilityTypes = ['creator', 'assignee', 'self'];
      
      if (type === 'all') {
        filters.value = { ...filters.value, status: 'all', dueDate: 'all', responsibility: 'all' };
      } else if (type === 'completed') {
        filters.value = { ...filters.value, status: 'completed', dueDate: 'all', responsibility: 'all' };
      } else if (dueDateTypes.includes(type)) {
        filters.value = { ...filters.value, status: 'pending', dueDate: type, responsibility: 'all' };
      } else if (responsibilityTypes.includes(type)) {
        filters.value = { ...filters.value, status: 'all', dueDate: 'all', responsibility: type };
      }
    };

    watch(
      [() => filters.value.status, () => filters.value.priority, () => filters.value.role, () => filters.value.keyword, () => filters.value.dueDate, () => filters.value.responsibility, sortBy, sortOrder],
      () => {
        const maxPage = Math.max(1, Math.ceil(filteredTasks.value.length / pageSize));
        if (currentPage.value > maxPage) {
          currentPage.value = maxPage;
        }
      }
    );

    onMounted(fetchTasks);

    return {
      tasks, currentUser, filters, newTask, showAddModal, handleAddTask, fetchTasks,
      confirmDelete, deleteConfirmId, handleDelete, filteredTasks, sortBy, sortOrder,
      currentPage, pageSize, totalPages, paginatedTasks, handleStatFilter,
      batchMode, selectedTaskIds, enterBatchMode, exitBatchMode, toggleTaskSelect,
      isAllSelected, toggleSelectAll, showBatchModal, batchOperation, batchFormData,
      batchOperationLabel, batchOperationIcon, selectedTasksPreview,
      openBatchModal, closeBatchModal, executeBatchOperation, batchSubmitting,
      overdueTask, detailTask, requestComplete, submitOverdueReason, showDetail
    };
  }
}
</script>
