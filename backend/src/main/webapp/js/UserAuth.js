export const UserAuth = {
    user: null,
    async checkAuth() {
        try {
            const resp = await axios.get('/api/me');
            this.user = resp.data.id ? resp.data : null;
            return this.user;
        } catch (e) {
            this.user = null;
            return null;
        }
    },
    async login(username, password) {
        try {
            const resp = await axios.post('/api/login', { username, password });
            if (resp.data.success) {
                this.user = resp.data.user;
                return { success: true };
            }
            return { success: false, message: resp.data.message };
        } catch (e) {
            if (e.response && e.response.data) {
                return e.response.data;
            }
            return { success: false, message: '网络错误' };
        }
    },
    async register(username, password) {
        try {
            const resp = await axios.post('/api/register', { username, password });
            return resp.data;
        } catch (e) {
            if (e.response && e.response.data) {
                return e.response.data;
            }
            return { success: false, message: '网络错误' };
        }
    },
    async logout() {
        await axios.post('/api/logout');
        this.user = null;
    }
};
