<template>
  <select :value="modelValue" @change="$emit('update:modelValue', $event.target.value || null)" class="input">
    <option :value="null">未指派</option>
    <option v-for="user in users" :key="user.id" :value="user.id">
      {{ user.username }}
    </option>
  </select>
</template>

<script>
const { ref, onMounted } = Vue;

export default {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  setup() {
    const users = ref([]);

    onMounted(async () => {
      try {
        const res = await axios.get('/api/users');
        users.value = res.data;
      } catch (e) {
        console.error('Failed to fetch users');
      }
    });

    return { users };
  }
}
</script>
