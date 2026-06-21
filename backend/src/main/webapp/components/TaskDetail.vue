<template>
  <div class="modal-overlay animate-entrance" @click.self="$emit('close')">
    <div class="modal glass task-detail-modal">
      <div class="modal-header-lite">
        <h3>任务详情</h3>
        <button @click="$emit('close')" class="btn-x">×</button>
      </div>

      <div class="task-detail-body">
        <div class="detail-section">
          <span class="detail-label">任务内容</span>
          <p class="detail-text">{{ task.text }}</p>
        </div>

        <div class="detail-grid">
          <div class="detail-cell">
            <span class="detail-label">优先级</span>
            <span :class="['detail-value', 'priority-dot-badge', `dot-${task.priority}`]">
              <span class="dot"></span>{{ priorityLabel }}
            </span>
          </div>
          <div class="detail-cell">
            <span class="detail-label">完成状态</span>
            <span class="detail-value">
              <span :class="['status-pill', task.completed ? 'status-done' : 'status-pending']">
                {{ task.completed ? '已完成' : '进行中' }}
              </span>
            </span>
          </div>
          <div class="detail-cell">
            <span class="detail-label">截止日期</span>
            <span class="detail-value" :class="{ 'danger-text': utils.isOverdue(task.dueDate) && !task.completed }">
              {{ formatTime(task.dueDate) }}
            </span>
          </div>
          <div class="detail-cell">
            <span class="detail-label">相关人员</span>
            <span class="detail-value">{{ personLabel || '—' }}</span>
          </div>
          <div class="detail-cell">
            <span class="detail-label">创建时间</span>
            <span class="detail-value">{{ formatTime(task.createdAt) }}</span>
          </div>
          <div class="detail-cell" v-if="task.completedAt">
            <span class="detail-label">完成时间</span>
            <span class="detail-value">{{ formatTime(task.completedAt) }}</span>
          </div>
        </div>

        <div class="detail-section overdue-detail-section" v-if="task.overdueReason">
          <span class="detail-label overdue-detail-label">📝 逾期原因（完整说明）</span>
          <p class="detail-overdue-text">{{ task.overdueReason }}</p>
        </div>
      </div>

      <div class="modal-btns">
        <button type="button" @click="$emit('close')" class="btn-cancel">关闭</button>
      </div>
    </div>
  </div>
</template>

<script>
const { computed } = Vue;

export default {
  props: ['task', 'currentUser'],
  emits: ['close'],
  setup(props) {
    const formatTime = (ts) => utils.formatDate(ts);
    const priorityLabel = computed(() => {
      const labels = { high: '紧急', medium: '普通', low: '次要' };
      return labels[props.task.priority] || props.task.priority;
    });
    const personLabel = computed(() => utils.formatTaskPerson(props.task, props.currentUser));
    return { formatTime, priorityLabel, personLabel, utils };
  }
};
</script>
