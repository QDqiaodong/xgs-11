<template>
  <div class="today-focus-section">
    <div class="focus-header">
      <div class="focus-title-block">
        <span class="focus-icon">🔥</span>
        <div>
          <h2>今日焦点</h2>
          <p class="focus-subtitle">优先处理最紧急的事项</p>
        </div>
      </div>
      <div class="focus-summary">
        <span class="summary-badge" :class="{ 'badge-danger': overdueTasks.length > 0, 'badge-urgent': overdueTasks.length === 0 && urgentTasks.length > 0, 'badge-soon': overdueTasks.length === 0 && urgentTasks.length === 0 && soonTasks.length > 0 }">
          {{ totalPending }} 项待处理
        </span>
      </div>
    </div>

    <div class="focus-groups">
      <div class="focus-group glass" :class="{ 'group-danger': overdueTasks.length > 0 }">
        <div class="group-header">
          <div class="group-title">
            <span class="group-icon danger-icon">⏰</span>
            <h3>已逾期</h3>
            <span class="group-count danger-count">{{ overdueTasks.length }}</span>
          </div>
        </div>
        <div class="group-tasks">
          <transition-group name="fade">
            <div 
              v-for="task in overdueTasks" 
              :key="'overdue-' + task.id" 
              :class="['focus-task-item', { 'is-completed': task.completed }]"
            >
              <label class="mini-checkbox">
                <input type="checkbox" :checked="task.completed" @change="toggleTask(task)">
                <span class="mini-check-box"></span>
              </label>
              <div class="task-info">
                <span class="task-text">{{ task.text }}</span>
                <div class="task-mini-meta">
                  <span :class="['priority-mini-tag', `mini-${task.priority}`]">
                    {{ priorityLabel(task.priority) }}
                  </span>
                  <span class="due-text danger-text">{{ formatTime(task.dueDate) }}</span>
                </div>
              </div>
            </div>
          </transition-group>
          <div v-if="overdueTasks.length === 0" class="empty-group">
            <span class="empty-icon">✅</span>
            <span>暂无逾期任务</span>
          </div>
        </div>
      </div>

      <div class="focus-group glass" :class="{ 'group-urgent': urgentTasks.length > 0 }">
        <div class="group-header">
          <div class="group-title">
            <span class="group-icon urgent-icon">⚡</span>
            <h3>24小时内到期</h3>
            <span class="group-count urgent-count">{{ urgentTasks.length }}</span>
          </div>
        </div>
        <div class="group-tasks">
          <transition-group name="fade">
            <div 
              v-for="task in urgentTasks" 
              :key="'urgent-' + task.id" 
              :class="['focus-task-item', { 'is-completed': task.completed }]"
            >
              <label class="mini-checkbox">
                <input type="checkbox" :checked="task.completed" @change="toggleTask(task)">
                <span class="mini-check-box"></span>
              </label>
              <div class="task-info">
                <span class="task-text">{{ task.text }}</span>
                <div class="task-mini-meta">
                  <span :class="['priority-mini-tag', `mini-${task.priority}`]">
                    {{ priorityLabel(task.priority) }}
                  </span>
                  <span class="due-text urgent-text">{{ formatTime(task.dueDate) }}</span>
                </div>
              </div>
            </div>
          </transition-group>
          <div v-if="urgentTasks.length === 0" class="empty-group">
            <span class="empty-icon">🕐</span>
            <span>无即将到期任务</span>
          </div>
        </div>
      </div>

      <div class="focus-group glass" :class="{ 'group-soon': soonTasks.length > 0 }">
        <div class="group-header">
          <div class="group-title">
            <span class="group-icon soon-icon">�</span>
            <h3>3天内到期</h3>
            <span class="group-count soon-count">{{ soonTasks.length }}</span>
          </div>
        </div>
        <div class="group-tasks">
          <transition-group name="fade">
            <div 
              v-for="task in soonTasks" 
              :key="'soon-' + task.id" 
              :class="['focus-task-item', { 'is-completed': task.completed }]"
            >
              <label class="mini-checkbox">
                <input type="checkbox" :checked="task.completed" @change="toggleTask(task)">
                <span class="mini-check-box"></span>
              </label>
              <div class="task-info">
                <span class="task-text">{{ task.text }}</span>
                <div class="task-mini-meta">
                  <span :class="['priority-mini-tag', `mini-${task.priority}`]">
                    {{ priorityLabel(task.priority) }}
                  </span>
                  <span class="due-text soon-text">{{ formatTime(task.dueDate) }}</span>
                </div>
              </div>
            </div>
          </transition-group>
          <div v-if="soonTasks.length === 0" class="empty-group">
            <span class="empty-icon">📋</span>
            <span>暂无近期到期任务</span>
          </div>
        </div>
      </div>

      <div class="focus-group glass">
        <div class="group-header">
          <div class="group-title">
            <span class="group-icon primary-icon">📝</span>
            <h3>普通待办</h3>
            <span class="group-count">{{ normalTasks.length }}</span>
          </div>
        </div>
        <div class="group-tasks">
          <transition-group name="fade">
            <div 
              v-for="task in normalTasks" 
              :key="'normal-' + task.id" 
              :class="['focus-task-item', { 'is-completed': task.completed }]"
            >
              <label class="mini-checkbox">
                <input type="checkbox" :checked="task.completed" @change="toggleTask(task)">
                <span class="mini-check-box"></span>
              </label>
              <div class="task-info">
                <span class="task-text">{{ task.text }}</span>
                <div class="task-mini-meta">
                  <span :class="['priority-mini-tag', `mini-${task.priority}`]">
                    {{ priorityLabel(task.priority) }}
                  </span>
                  <span v-if="task.dueDate" class="due-text">{{ formatTime(task.dueDate) }}</span>
                </div>
              </div>
            </div>
          </transition-group>
          <div v-if="normalTasks.length === 0" class="empty-group">
            <span class="empty-icon">🎐</span>
            <span>暂无普通待办</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
const { computed, inject } = Vue;

export default {
  props: ['tasks', 'currentUser'],
  emits: ['update'],
  setup(props, { emit }) {
    const showToast = inject('showToast');

    const myRelatedTasks = computed(() => {
      if (!props.currentUser) return [];
      return props.tasks.filter(t => 
        t.userId === props.currentUser.id || t.assigneeId === props.currentUser.id
      );
    });

    const sortByPriority = (a, b) => {
      const priorityWeight = { high: 3, medium: 2, low: 1 };
      return priorityWeight[b.priority] - priorityWeight[a.priority];
    };

    const overdueTasks = computed(() => {
      return myRelatedTasks.value
        .filter(t => !t.completed && utils.isOverdue(t.dueDate))
        .sort(sortByPriority);
    });

    const urgentTasks = computed(() => {
      return myRelatedTasks.value
        .filter(t => !t.completed && utils.isDueWithin24h(t.dueDate))
        .sort(sortByPriority);
    });

    const soonTasks = computed(() => {
      return myRelatedTasks.value
        .filter(t => !t.completed && utils.isDueWithin3Days(t.dueDate) && !utils.isDueWithin24h(t.dueDate))
        .sort(sortByPriority);
    });

    const normalTasks = computed(() => {
      return myRelatedTasks.value
        .filter(t => !t.completed && !utils.isOverdue(t.dueDate) && !utils.isDueWithin24h(t.dueDate) && !utils.isDueWithin3Days(t.dueDate))
        .sort(sortByPriority);
    });

    const totalPending = computed(() => {
      return overdueTasks.value.length + urgentTasks.value.length
        + soonTasks.value.length + normalTasks.value.length;
    });

    const formatTime = (ts) => utils.formatDate(ts);
    
    const priorityLabel = (priority) => {
      const labels = { high: '紧急', medium: '普通', low: '次要' };
      return labels[priority] || priority;
    };

    const toggleTask = async (task) => {
      try {
        await axios.put(`/api/tasks/${task.id}`, {
          ...task,
          completed: !task.completed
        });
        emit('update');
        if (!task.completed) {
          showToast('任务已完成 🎉', 'success');
        }
      } catch (e) {
        showToast('状态更新失败', 'danger');
      }
    };

    return {
      overdueTasks,
      urgentTasks,
      soonTasks,
      normalTasks,
      totalPending,
      formatTime,
      priorityLabel,
      toggleTask
    };
  }
}
</script>
