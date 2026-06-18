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
        <h1 class="brand-title">加入社区<br/><span class="text-gradient">发现更多可能</span></h1>
        <p class="brand-description">简单、优雅、高效。加入数千名同样追求极致效率的用户，开启全新的任务管理体验。</p>
        
        <div class="brand-features">
          <div class="feature-tag">
            <span class="feat-icon">🚀</span> 快速上手
          </div>
          <div class="feature-tag">
            <span class="feat-icon">☁️</span> 多端同步
          </div>
          <div class="feature-tag">
            <span class="feat-icon">🛠️</span> 灵活定制
          </div>
        </div>
      </div>
    </div>

    <div class="auth-side-form">
      <div class="form-card glass animate-entrance" style="animation-delay: 0.2s;">
        <div class="form-header">
          <h2>创建账号</h2>
          <p>开启您的专业任务管理之旅</p>
        </div>
        
        <form @submit.prevent="handleRegister" class="premium-form">
          <div class="form-group floating-group">
            <div class="input-container">
              <span class="field-icon">👤</span>
              <input type="text" v-model="username" class="input" placeholder=" " required id="reg-user">
              <label for="reg-user">设置用户名</label>
            </div>
          </div>

          <div class="form-group floating-group">
            <div class="input-container">
              <span class="field-icon">🔒</span>
              <input type="password" v-model="password" class="input" placeholder=" " required id="reg-pass">
              <label for="reg-pass">设置登录密码</label>
            </div>
          </div>

          <div class="form-group floating-group">
            <div class="input-container">
              <span class="field-icon">🛡️</span>
              <input type="password" v-model="confirmPassword" class="input" placeholder=" " required id="reg-pass-confirm">
              <label for="reg-pass-confirm">确认登录密码</label>
            </div>
          </div>


          <button type="submit" class="btn btn-primary btn-wide" :disabled="loading">
            <span v-if="!loading">立即创建账号</span>
            <span v-else class="btn-loader"></span>
          </button>
        </form>
        
        <div class="form-footer">
          <p>已经有账号了? <a @click="$emit('goto-login')" class="text-primary-link">返回登录</a></p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
const { ref, inject } = Vue;

export default {
  emits: ['goto-login'],
  inject: ['showToast'],
  setup(props, { emit }) {
    const username = ref('');
    const password = ref('');
    const confirmPassword = ref('');
    const loading = ref(false);
    const showToast = inject('showToast');

    const handleRegister = async () => {
      if (password.value !== confirmPassword.value) {
        showToast('两次输入的密码不一致', 'danger');
        return;
      }
      loading.value = true;
      try {
        const res = await UserAuth.register(username.value, password.value);
        if (res.success) {
          showToast('账号创建成功，请开始登录', 'success');
          emit('goto-login');
        } else {
          showToast(res.message || '注册请求被拒绝', 'danger');
        }
      } catch (e) {
        showToast('无法连接到注册服务', 'danger');
      } finally {
        loading.value = false;
      }
    };

    return { username, password, confirmPassword, handleRegister, loading };
  }
}
</script>
