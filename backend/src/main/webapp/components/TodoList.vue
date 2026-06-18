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
          <TaskStats :tasks="tasks" :currentUser="currentUser" />
        </section>

        <section class="task-board">
          <div class="board-header">
            <h2>待办清单 <span class="count-chip">{{ filteredTasks.length }}</span></h2>
          </div>

          <div class="task-grid-feed">
            <transition-group name="fade">
              <TodoItem 
                v-for="task in paginatedTasks" 
                :key="task.id" 
                :task="task" 
                :currentUser="currentUser"
                @update="fetchTasks"
                @delete="confirmDelete"
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
    </Teleport>
  </div>
</template>

<script>
const { ref, onMounted, computed, reactive, inject } = Vue;

export default {
  emits: ['logout'],
  inject: ['showToast'],
  setup() {
    const tasks = ref([]);
    const currentUser = ref(UserAuth.user);
    const showAddModal = ref(false);
    const deleteConfirmId = ref(null);
    const showToast = inject('showToast');

    const filters = ref({
      keyword: '',
      status: 'all',
      priority: 'all',
      role: 'all'
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
        showToast('数据同步失败', 'danger');
      }
    };

    const handleAddTask = async () => {
      if (!newTask.text.trim()) return;
      try {
        const payload = {
            text: newTask.text.trim(),
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
        showToast('添加失败', 'danger');
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
        showToast('删除失败', 'danger');
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
        return matchKeyword && matchStatus && matchPriority && matchRole;
      });

      result.sort((a, b) => {
        let valA = a[sortBy.value];
        let valB = b[sortBy.value];
        
        if (sortBy.value === 'priority') {
            const weights = { high: 3, medium: 2, low: 1 };
            valA = weights[a.priority];
            valB = weights[b.priority];
        }

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

    onMounted(fetchTasks);

    return {
      tasks, currentUser, filters, newTask, showAddModal, handleAddTask, fetchTasks,
      confirmDelete, deleteConfirmId, handleDelete, filteredTasks, sortBy, sortOrder,
      currentPage, pageSize, totalPages, paginatedTasks
    };
  }
}
</script>
