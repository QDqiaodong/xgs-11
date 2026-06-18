<template>
  <div class="auth-screen animate-entrance">
    <div class="auth-side-brand">
      <div class="brand-visual-layer">
        <div class="floating-shape shape-1"></div>
        <div class="floating-shape shape-2"></div>
      </div>
      <div class="brand-content">
        <div class="logo-badge">
          <span class="logo-icon">✨</span>
        </div>
        <h1 class="brand-title">高效协作<br/><span class="text-gradient">开启专业之旅</span></h1>
        <p class="brand-description">不仅仅是待办清单，更是您的私人效率中心。集成化的任务管理，助您掌控每一刻。</p>
        
        <div class="brand-features">
          <div class="feature-tag">
            <span class="feat-icon">⚡</span> 极致响应
          </div>
          <div class="feature-tag">
            <span class="feat-icon">🔒</span> 安全加密
          </div>
          <div class="feature-tag">
            <span class="feat-icon">🌐</span> 全端同步
          </div>
        </div>
      </div>
    </div>

    <div class="auth-side-form">
      <div class="form-card glass animate-entrance" style="animation-delay: 0.2s;">
        <div class="form-header">
          <h2>欢迎登录</h2>
          <p>请填账户信息，开始高效的一天</p>
        </div>
        
        <form @submit.prevent="handleLogin" class="premium-form">
          <div class="form-group floating-group">
            <div class="input-container">
              <span class="field-icon">👤</span>
              <input type="text" v-model="username" class="input" placeholder=" " required id="login-user">
              <label for="login-user">用户名 / 账号</label>
            </div>
          </div>

          <div class="form-group floating-group">
            <div class="input-container">
              <span class="field-icon">🔒</span>
              <input type="password" v-model="password" class="input" placeholder=" " required id="login-pass">
              <label for="login-pass">登录密码</label>
            </div>
          </div>


          <button type="submit" class="btn btn-primary btn-wide" :disabled="loading">
            <span v-if="!loading">立即进入</span>
            <span v-else class="btn-loader"></span>
          </button>
        </form>
        
        <div class="form-footer">
          <p>还没有账号? <a @click="$emit('goto-register')" class="text-primary-link">申请加入</a></p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
const { ref, inject } = Vue;

export default {
  emits: ['login-success', 'goto-register'],
  inject: ['showToast'],
  setup(props, { emit }) {
    const username = ref('');
    const password = ref('');
    const loading = ref(false);
    const showToast = inject('showToast');

    const handleLogin = async () => {
      loading.value = true;
      try {
        const res = await UserAuth.login(username.value, password.value);
        if (res.success) {
          showToast('登录成功，欢迎回来', 'success');
          emit('login-success');
        } else {
          showToast(res.message || '凭据错误，请重试', 'danger');
        }
      } catch (e) {
        showToast('无法连接到服务中心', 'danger');
      } finally {
        loading.value = false;
      }
    };

    return { username, password, handleLogin, loading };
  }
}
</script>
