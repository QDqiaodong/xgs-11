<template>
  <div :class="['card', 'premium-task-card', { 'is-completed': task.completed, [`is-${riskLevel}`]: !task.completed && riskLevel !== 'normal' }]">
    <div class="card-accent-strip"></div>
    
    <div class="task-control">
      <label class="premium-checkbox">
        <input type="checkbox" :checked="task.completed" @change="toggleStatus">
        <span class="checkbox-box">
          <span class="checkbox-tick"></span>
        </span>
      </label>
    </div>
    
    <div class="task-content">
      <div v-if="!isEditing" class="view-layout">
        <div class="task-header-row">
          <h4 class="task-main-text">{{ task.text }}</h4>
          <div class="task-badges">
            <span v-if="responsibility !== 'none'" :class="['resp-badge', `resp-${responsibility}`]">
              {{ responsibilityLabel }}
            </span>
            <div :class="['priority-dot-badge', `dot-${task.priority}`]">
              <span class="dot"></span> {{ priorityLabel }}
            </div>
          </div>
        </div>
        
        <div class="task-meta-footer">
          <div class="meta-item">
            <span class="meta-icon">👤</span>
            <span class="meta-text">{{ personLabel }}</span>
          </div>
          <div class="meta-dot"></div>
          <div :class="['meta-item', `status-${riskLevel}`]">
            <span class="meta-icon">📅</span>
            <span class="meta-text">{{ formatTime(task.dueDate) }}</span>
            <span v-if="!task.completed && riskLevel !== 'normal'" :class="['risk-tag', `risk-${riskLevel}`]">{{ riskLabel }}</span>
          </div>
        </div>
      </div>
      
      <div v-else class="edit-layout">
        <textarea v-model="editData.text" class="input edit-area" placeholder="更新任务描述..."></textarea>
        <div class="edit-grid">
          <select v-model="editData.priority" class="input input-sm">
            <option value="high">紧急优先级</option>
            <option value="medium">普通优先级</option>
            <option value="low">低优先级</option>
          </select>
          <input type="datetime-local" v-model="editData.due_date" class="input input-sm">
        </div>
        <div class="edit-actions-row">
          <UserSelect v-model="editData.assigneeId" class="flex-1" />
        </div>
      </div>
    </div>

    <div class="task-operations">
      <template v-if="!isEditing">
        <button @click="startEdit" class="btn-op" title="修改">
          <span>✏️</span>
        </button>
        <button @click="$emit('delete', task.id)" class="btn-op op-danger" title="删除">
          <span>🗑️</span>
        </button>
      </template>
      <template v-else>
        <button @click="saveEdit" class="btn-op op-success" title="保存">
          <span>💾</span>
        </button>
        <button @click="cancelEdit" class="btn-op" title="取消">
          <span>↩️</span>
        </button>
      </template>
    </div>
  </div>
</template>

<script>
const { ref, reactive, computed, inject } = Vue;

export default {
  props: ['task', 'currentUser'],
  emits: ['update', 'delete'],
  inject: ['showToast'],
  setup(props, { emit }) {
    const isEditing = ref(false);
    const showToast = inject('showToast');
    
    const editData = reactive({
      text: props.task.text,
      priority: props.task.priority,
      assigneeId: props.task.assigneeId,
      due_date: utils.formatDateTimeLocal(props.task.dueDate)
    });

    const riskLevel = computed(() => utils.getTaskRiskLevel(props.task));
    const riskLabel = computed(() => {
      const labels = { overdue: '已逾期', urgent: '即将到期', soon: '3天内到期' };
      return labels[riskLevel.value] || '';
    });
    const formatTime = (ts) => utils.formatDate(ts);
    const personLabel = computed(() => utils.formatTaskPerson(props.task, props.currentUser));
    const priorityLabel = computed(() => {
      const labels = { high: '紧急', medium: '普通', low: '次要' };
      return labels[props.task.priority] || props.task.priority;
    });
    const responsibility = computed(() => utils.getTaskResponsibility(props.task, props.currentUser));
    const responsibilityLabel = computed(() => {
      const labels = { creator: '我创建', assignee: '指派给我', self: '自办', none: '' };
      return labels[responsibility.value] || '';
    });

    const toggleStatus = async () => {
      try {
        await axios.put(`/api/tasks/${props.task.id}`, {
          ...props.task,
          completed: !props.task.completed
        });
        emit('update');
      } catch (e) {
        let msg = '无法同步状态';
        if (e.response && e.response.status === 401) {
          msg = '登录已过期，请重新登录';
        }
        showToast(msg, 'danger');
      }
    };

    const startEdit = () => { isEditing.value = true; };
    const cancelEdit = () => {
      isEditing.value = false;
      Object.assign(editData, {
          text: props.task.text,
          priority: props.task.priority,
          assigneeId: props.task.assigneeId,
          due_date: utils.formatDateTimeLocal(props.task.dueDate)
      });
    };

    const saveEdit = async () => {
      const text = editData.text.trim();
      if (!text) {
        showToast('任务内容不能为空', 'danger');
        return;
      }
      if (text.length > 500) {
        showToast('任务内容不能超过 500 个字符', 'danger');
        return;
      }
      const validPriorities = ['high', 'medium', 'low'];
      if (!validPriorities.includes(editData.priority)) {
        showToast('优先级不合法', 'danger');
        return;
      }
      if (editData.due_date && isNaN(new Date(editData.due_date).getTime())) {
        showToast('截止时间不合法', 'danger');
        return;
      }
      try {
        const payload = { 
            ...editData,
            text: text,
            completed: props.task.completed,
            dueDate: editData.due_date ? new Date(editData.due_date).getTime() : null
        };
        await axios.put(`/api/tasks/${props.task.id}`, payload);
        showToast('任务更新成功', 'success');
        isEditing.value = false;
        emit('update');
      } catch (e) {
        let msg = '数据保存失败';
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

    return { 
        isEditing, editData, riskLevel, riskLabel, formatTime, personLabel,
        toggleStatus, startEdit, cancelEdit, saveEdit, priorityLabel,
        responsibility, responsibilityLabel
    };
  }
}
</script>
