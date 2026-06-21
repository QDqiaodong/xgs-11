<template>
  <div class="schedule-app-container animate-entrance">
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
            <button @click="$emit('goto-list')" class="nav-btn">
              <span class="nav-icon">📋</span>
              <span>任务列表</span>
            </button>
            <button class="nav-btn nav-btn-active">
              <span class="nav-icon">📅</span>
              <span>日程视图</span>
            </button>
          </div>
        </div>

        <div class="nav-section">
          <p class="section-title">筛选条件</p>
          <div class="sidebar-controls">
            <div class="schedule-filters">
              <div class="filter-item">
                <label>任务角色</label>
                <select v-model="filterRole" class="input select-input">
                  <option value="all">全部任务</option>
                  <option value="mine">我创建的</option>
                  <option value="assigned">受托任务</option>
                </select>
              </div>
              <div class="filter-item">
                <label>优先级</label>
                <select v-model="filterPriority" class="input select-input">
                  <option value="all">全部优先级</option>
                  <option value="high">紧急</option>
                  <option value="medium">普通</option>
                  <option value="low">较低</option>
                </select>
              </div>
              <div class="filter-item">
                <label>完成状态</label>
                <select v-model="filterStatus" class="input select-input">
                  <option value="all">全部状态</option>
                  <option value="incomplete">未完成</option>
                  <option value="completed">已完成</option>
                </select>
              </div>
            </div>
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
            <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round">
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
          <h1>日程视图</h1>
          <p>快速规划本周任务，一目了然。</p>
        </div>
        <div class="schedule-header-controls">
          <div class="week-nav">
            <button @click="prevWeek" class="btn-week-nav" title="上一周">←</button>
            <span class="week-range-text">{{ weekRangeText }}</span>
            <button @click="nextWeek" class="btn-week-nav" title="下一周">→</button>
            <button @click="goToThisWeek" class="btn-today" :disabled="isCurrentWeek">本周</button>
          </div>
        </div>
      </div>

      <div class="viewport-canvas schedule-canvas">
        <div class="week-view-grid">
          <div
            v-for="(day, idx) in weekDates"
            :key="idx"
            class="day-column"
            :class="{ 'is-today': utils.isToday(day), 'is-weekend': idx === 5 || idx === 6 }"
            @dragover.prevent
            @drop="handleDrop($event, day)"
          >
            <div class="day-header" :class="{ 'today-header': utils.isToday(day) }">
              <div class="day-weekday">{{ utils.formatWeekDay(day) }}</div>
              <div class="day-date">{{ utils.formatMonthDay(day) }}</div>
              <div class="day-count" v-if="getTasksForDay(day).length > 0">
                {{ getTasksForDay(day).length }}
              </div>
            </div>
            <div class="day-tasks">
              <transition-group name="fade">
                <div
                  v-for="task in getTasksForDay(day)"
                  :key="task.id"
                  class="schedule-task-card"
                  :class="[
                    `priority-${task.priority}`,
                    { 'is-completed': task.completed, 'is-overdue': isTaskOverdue(task) }
                  ]"
                  draggable="true"
                  @dragstart="handleDragStart($event, task)"
                >
                  <div class="task-top-row">
                    <label class="mini-checkbox">
                      <input type="checkbox" :checked="task.completed" @change="toggleTask(task)" />
                      <span class="mini-check-box"></span>
                    </label>
                    <span :class="['priority-badge', `badge-${task.priority}`]">
                      {{ priorityText(task.priority) }}
                    </span>
                  </div>
                  <div class="task-text-line" :class="{ 'strike-through': task.completed }">
                    {{ task.text }}
                  </div>
                  <div class="task-meta-row">
                    <span v-if="task.dueDate" class="task-due-time">
                      ⏰ {{ utils.formatDateShort(task.dueDate) }}
                    </span>
                    <span v-if="getPersonLabel(task)" class="task-assignee" :title="getPersonLabel(task)">
                      👤 {{ getPersonLabel(task) }}
                    </span>
                  </div>
                  <div class="task-actions-row">
                    <button @click.stop="openScheduleModal(task, day)" class="btn-task-action" title="调整时间">
                      🕐
                    </button>
                    <button @click.stop="clearDueDate(task)" class="btn-task-action" title="移除截止日期">
                      ✕
                    </button>
                  </div>
                </div>
              </transition-group>
              <div v-if="getTasksForDay(day).length === 0" class="day-empty-hint" @click="openScheduleModal(null, day)">
                <span>+ 点击添加</span>
              </div>
            </div>
          </div>
        </div>

        <section class="unscheduled-section">
          <div class="unscheduled-header">
            <div class="unscheduled-title-block">
              <span class="unscheduled-icon">📝</span>
              <h3>待排期任务</h3>
              <span class="unscheduled-count">{{ unscheduledTasks.length }} 项</span>
            </div>
            <p class="unscheduled-hint">拖拽任务卡片到上方日期，或点击"🕐"设置截止日期</p>
          </div>
          <div class="unscheduled-tasks-grid">
            <transition-group name="fade">
              <div
                v-for="task in unscheduledTasks"
                :key="task.id"
                class="schedule-task-card unscheduled-card"
                :class="[`priority-${task.priority}`, { 'is-completed': task.completed }]"
                draggable="true"
                @dragstart="handleDragStart($event, task)"
              >
                <div class="task-top-row">
                  <label class="mini-checkbox">
                    <input type="checkbox" :checked="task.completed" @change="toggleTask(task)" />
                    <span class="mini-check-box"></span>
                  </label>
                  <span :class="['priority-badge', `badge-${task.priority}`]">
                    {{ priorityText(task.priority) }}
                  </span>
                </div>
                <div class="task-text-line" :class="{ 'strike-through': task.completed }">
                  {{ task.text }}
                </div>
                <div class="task-meta-row">
                  <span v-if="getPersonLabel(task)" class="task-assignee" :title="getPersonLabel(task)">
                    👤 {{ getPersonLabel(task) }}
                  </span>
                </div>
                <div class="task-actions-row">
                  <button @click.stop="openScheduleModal(task, null)" class="btn-task-action" title="设置截止日期">
                    🕐 规划
                  </button>
                </div>
              </div>
            </transition-group>
            <div v-if="unscheduledTasks.length === 0" class="unscheduled-empty">
              <span class="empty-icon">🎊</span>
              <p>所有任务都已安排！</p>
            </div>
          </div>
        </section>
      </div>
    </main>

    <Teleport to="body">
      <div v-if="showScheduleModal" class="modal-overlay animate-entrance" @click.self="showScheduleModal = false">
        <div class="modal glass">
          <div class="modal-header-lite">
            <h3>{{ editingTask ? '调整任务时间' : '快速规划任务' }}</h3>
            <button @click="showScheduleModal = false" class="btn-x">×</button>
          </div>
          <form @submit.prevent="handleScheduleTask" class="premium-form-lite">
            <div v-if="editingTask" class="form-group-lite">
              <label>任务内容</label>
              <div class="task-preview-text">{{ editingTask.text }}</div>
            </div>
            <div class="form-group-lite" v-else>
              <label>选择任务</label>
              <select v-model="selectedTaskId" class="input" required>
                <option value="" disabled>请选择要排期的任务...</option>
                <option v-for="t in availableUnscheduledTasks" :key="t.id" :value="t.id">
                  {{ t.text }} ({{ priorityText(t.priority) }})
                </option>
              </select>
            </div>

            <div class="form-row-lite">
              <div class="form-group-lite">
                <label>截止日期</label>
                <input type="date" v-model="scheduleForm.date" class="input" required />
              </div>
              <div class="form-group-lite">
                <label>截止时间</label>
                <input type="time" v-model="scheduleForm.time" class="input" />
              </div>
            </div>

            <div class="modal-btns">
              <button type="button" @click="showScheduleModal = false" class="btn-cancel">取消</button>
              <button type="submit" class="btn btn-primary">确认安排</button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script>
const { ref, onMounted, computed, inject } = Vue;

export default {
  emits: ['logout', 'goto-list'],
  inject: ['showToast'],
  setup() {
    const tasks = ref([]);
    const currentUser = ref(UserAuth.user);
    const showToast = inject('showToast');
    const weekRef = ref(new Date());

    const filterRole = ref('all');
    const filterPriority = ref('all');
    const filterStatus = ref('all');

    const showScheduleModal = ref(false);
    const editingTask = ref(null);
    const selectedTaskId = ref('');
    const scheduleForm = ref({ date: '', time: '' });
    const dragTaskId = ref(null);

    const utils = window.utils;

    const fetchTasks = async () => {
      try {
        const res = await axios.get('/api/tasks');
        tasks.value = res.data;
      } catch (e) {
        showToast('数据同步失败', 'danger');
      }
    };

    const weekDates = computed(() => utils.getWeekDates(weekRef.value));
    const weekRangeText = computed(() => utils.getWeekRangeText(weekDates.value));
    const isCurrentWeek = computed(() => {
      const thisWeek = utils.getWeekDates(new Date());
      return thisWeek[0].getTime() === weekDates.value[0].getTime();
    });

    const filteredTasks = computed(() => {
      return tasks.value.filter(t => {
        const matchRole = filterRole.value === 'all' ||
          (filterRole.value === 'mine' ? t.userId === currentUser.value.id : t.assigneeId === currentUser.value.id);
        const matchPriority = filterPriority.value === 'all' || t.priority === filterPriority.value;
        const matchStatus = filterStatus.value === 'all' ||
          (filterStatus.value === 'completed' ? t.completed : !t.completed);
        return matchRole && matchPriority && matchStatus;
      });
    });

    const unscheduledTasks = computed(() => {
      return filteredTasks.value.filter(t => !t.dueDate);
    });

    const availableUnscheduledTasks = computed(() => {
      return unscheduledTasks.value.filter(t => !t.completed);
    });

    const getTasksForDay = (day) => {
      return filteredTasks.value.filter(t => {
        if (!t.dueDate) return false;
        return utils.isSameDay(t.dueDate, day);
      }).sort((a, b) => {
        const weights = { high: 3, medium: 2, low: 1 };
        if (weights[b.priority] !== weights[a.priority]) {
          return weights[b.priority] - weights[a.priority];
        }
        return new Date(a.dueDate) - new Date(b.dueDate);
      });
    };

    const isTaskOverdue = (task) => {
      if (!task.dueDate || task.completed) return false;
      return new Date(task.dueDate) < new Date();
    };

    const priorityText = (p) => ({ high: '紧急', medium: '普通', low: '较低' }[p] || '普通');

    const getPersonLabel = (task) => utils.formatTaskPerson(task, currentUser.value);

    const prevWeek = () => { weekRef.value = utils.addWeeks(weekRef.value, -1); };
    const nextWeek = () => { weekRef.value = utils.addWeeks(weekRef.value, 1); };
    const goToThisWeek = () => { weekRef.value = new Date(); };

    const toggleTask = async (task) => {
      try {
        await axios.put(`/api/tasks/${task.id}`, {
          ...task,
          completed: !task.completed,
          dueDate: task.dueDate ? task.dueDate : null
        });
        showToast(task.completed ? '已标记为未完成' : '已完成！', 'success');
        fetchTasks();
      } catch (e) {
        showToast('操作失败', 'danger');
      }
    };

    const clearDueDate = async (task) => {
      try {
        await axios.put(`/api/tasks/${task.id}`, {
          ...task,
          dueDate: null
        });
        showToast('已移至待排期', 'success');
        fetchTasks();
      } catch (e) {
        showToast('操作失败', 'danger');
      }
    };

    const openScheduleModal = (task, day) => {
      editingTask.value = task;
      selectedTaskId.value = task ? task.id : '';
      if (task && task.dueDate) {
        const d = new Date(task.dueDate);
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const dd = String(d.getDate()).padStart(2, '0');
        const h = String(d.getHours()).padStart(2, '0');
        const min = String(d.getMinutes()).padStart(2, '0');
        scheduleForm.value = { date: `${y}-${m}-${dd}`, time: `${h}:${min}` };
      } else if (day) {
        const y = day.getFullYear();
        const m = String(day.getMonth() + 1).padStart(2, '0');
        const d = String(day.getDate()).padStart(2, '0');
        scheduleForm.value = { date: `${y}-${m}-${d}`, time: '18:00' };
      } else {
        const today = new Date();
        const y = today.getFullYear();
        const m = String(today.getMonth() + 1).padStart(2, '0');
        const d = String(today.getDate()).padStart(2, '0');
        scheduleForm.value = { date: `${y}-${m}-${d}`, time: '18:00' };
      }
      showScheduleModal.value = true;
    };

    const handleScheduleTask = async () => {
      const taskId = editingTask.value ? editingTask.value.id : parseInt(selectedTaskId.value);
      if (!taskId) return;
      const task = tasks.value.find(t => t.id === taskId);
      if (!task) return;

      let dueDateTimestamp = null;
      if (scheduleForm.value.date) {
        const dateStr = scheduleForm.value.time
          ? `${scheduleForm.value.date}T${scheduleForm.value.time}:00`
          : `${scheduleForm.value.date}T23:59:00`;
        dueDateTimestamp = new Date(dateStr).getTime();
      }

      try {
        await axios.put(`/api/tasks/${taskId}`, {
          ...task,
          dueDate: dueDateTimestamp
        });
        showToast('已安排任务', 'success');
        showScheduleModal.value = false;
        fetchTasks();
      } catch (e) {
        showToast('安排失败', 'danger');
      }
    };

    const handleDragStart = (e, task) => {
      dragTaskId.value = task.id;
      e.dataTransfer.effectAllowed = 'move';
    };

    const handleDrop = async (e, day) => {
      e.preventDefault();
      if (!dragTaskId.value) return;
      const task = tasks.value.find(t => t.id === dragTaskId.value);
      if (!task) return;

      const y = day.getFullYear();
      const m = String(day.getMonth() + 1).padStart(2, '0');
      const d = String(day.getDate()).padStart(2, '0');
      const time = task.dueDate ? utils.formatDateShort(task.dueDate) : '18:00';
      const dateStr = `${y}-${m}-${d}T${time}:00`;
      const dueDateTimestamp = new Date(dateStr).getTime();

      try {
        await axios.put(`/api/tasks/${task.id}`, {
          ...task,
          dueDate: dueDateTimestamp
        });
        showToast(`已安排到 ${utils.formatMonthDay(day)}`, 'success');
        dragTaskId.value = null;
        fetchTasks();
      } catch (e) {
        showToast('安排失败', 'danger');
      }
    };

    onMounted(fetchTasks);

    return {
      tasks, currentUser, weekDates, weekRangeText, isCurrentWeek,
      filterRole, filterPriority, filterStatus,
      unscheduledTasks, availableUnscheduledTasks,
      getTasksForDay, isTaskOverdue, priorityText, getPersonLabel,
      prevWeek, nextWeek, goToThisWeek,
      toggleTask, clearDueDate,
      showScheduleModal, editingTask, selectedTaskId, scheduleForm,
      openScheduleModal, handleScheduleTask,
      handleDragStart, handleDrop,
      utils
    };
  }
};
</script>
