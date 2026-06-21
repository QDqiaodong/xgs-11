<template>
  <select :value="modelValue" @change="$emit('update:modelValue', $event.target.value || null)" class="input">
    <option :value="null">未指派</option>
    <option v-for="user in users" :key="user.id" :value="user.id">
      {{ user.username }}
    </option>
  </select>
</template>

<script>
const { ref, onMounted, inject } = Vue;

export default {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  setup() {
    const users = ref([]);
    const showToast = inject('showToast');

    onMounted(async () => {
      try {
        const res = await axios.get('/api/users');
        users.value = res.data;
      } catch (e) {
        if (e.response && e.response.status === 401) {
          showToast('登录已过期，请重新登录', 'danger');
        } else {
          showToast('获取用户列表失败', 'danger');
        }
      }
    });

    return { users };
  }
}
</script>
