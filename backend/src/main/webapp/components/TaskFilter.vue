<template>
  <div class="filter-controls">
    <div class="search-box">
      <span class="search-icon">🔍</span>
      <input 
        type="text" 
        :value="modelValue.keyword" 
        @input="updateFilter('keyword', $event.target.value)"
        class="input search-input" 
        placeholder="搜索任务描述..."
      >
    </div>
    
    <div class="select-group">
      <div class="filter-item">
        <label>状态</label>
        <select 
          :value="modelValue.status" 
          @change="updateFilter('status', $event.target.value)"
          class="input select-input"
        >
          <option value="all">全部显示</option>
          <option value="pending">待处理</option>
          <option value="completed">已完成</option>
        </select>
      </div>

      <div class="filter-item">
        <label>优先级</label>
        <select 
          :value="modelValue.priority" 
          @change="updateFilter('priority', $event.target.value)"
          class="input select-input"
        >
          <option value="all">不限</option>
          <option value="high">高 (紧急)</option>
          <option value="medium">中 (普通)</option>
          <option value="low">低 (次要)</option>
        </select>
      </div>

      <div class="filter-item">
        <label>到期时间</label>
        <select 
          :value="modelValue.dueDate" 
          @change="updateFilter('dueDate', $event.target.value)"
          class="input select-input"
        >
          <option value="all">全部</option>
          <option value="overdue">已逾期</option>
          <option value="dueToday">今日到期</option>
          <option value="within24h">24小时内</option>
          <option value="within3d">3天内</option>
          <option value="normal">普通待办</option>
        </select>
      </div>

      <div class="filter-item">
        <label>责任</label>
        <select 
          :value="modelValue.responsibility" 
          @change="updateFilter('responsibility', $event.target.value)"
          class="input select-input"
        >
          <option value="all">全部责任</option>
          <option value="creator">我创建</option>
          <option value="assignee">指派给我</option>
          <option value="self">我创建且自办</option>
        </select>
      </div>

      <div class="filter-item">
        <label>归属</label>
        <select 
          :value="modelValue.role" 
          @change="updateFilter('role', $event.target.value)"
          class="input select-input"
        >
          <option value="all">全部项目</option>
          <option value="mine">由我创建</option>
          <option value="assigned">指派给我</option>
        </select>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: ['modelValue'],
  emits: ['update:modelValue', 'add'],
  setup(props, { emit }) {
    const updateFilter = (key, value) => {
      emit('update:modelValue', {
        ...props.modelValue,
        [key]: value
      });
    };
    return { updateFilter };
  }
}
</script>
