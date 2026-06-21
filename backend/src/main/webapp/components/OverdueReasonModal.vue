<template>
  <div class="modal-overlay animate-entrance" @click.self="$emit('cancel')">
    <div class="modal glass overdue-reason-modal">
      <div class="modal-header-lite">
        <h3>补录逾期原因</h3>
        <button @click="$emit('cancel')" class="btn-x">×</button>
      </div>

      <div class="overdue-task-preview">
        <span class="otp-icon">⏰</span>
        <div class="otp-info">
          <p class="otp-label">该任务已超过截止日期，完成前请补录原因</p>
          <p class="otp-text">{{ task.text }}</p>
          <p class="otp-due danger-text">截止于 {{ formatTime(task.dueDate) }}</p>
        </div>
      </div>

      <div class="premium-form-lite">
        <div class="form-group-lite">
          <label>逾期原因说明 <span class="req">*</span></label>
          <textarea
            v-model="reason"
            class="input input-text-area overdue-reason-area"
            placeholder="请简要说明任务逾期的原因，便于后续复盘..."
            maxlength="500"
          ></textarea>
          <p class="form-hint">{{ reason.length }} / 500 字</p>
        </div>
      </div>

      <div class="modal-btns">
        <button type="button" @click="$emit('cancel')" class="btn-cancel">取消</button>
        <button @click="handleSubmit" class="btn btn-primary">确认完成</button>
      </div>
    </div>
  </div>
</template>

<script>
const { ref, watch, inject } = Vue;

export default {
  props: ['task'],
  emits: ['submit', 'cancel'],
  setup(props, { emit }) {
    const showToast = inject('showToast');
    const reason = ref('');

    watch(
      () => props.task,
      (t) => {
        reason.value = t && t.overdueReason ? t.overdueReason : '';
      },
      { immediate: true }
    );

    const formatTime = (ts) => utils.formatDate(ts);

    const handleSubmit = () => {
      const text = reason.value.trim();
      if (!text) {
        showToast('请填写逾期原因', 'danger');
        return;
      }
      if (text.length > 500) {
        showToast('逾期原因不能超过 500 个字符', 'danger');
        return;
      }
      emit('submit', text);
    };

    return { reason, formatTime, handleSubmit };
  }
};
</script>
