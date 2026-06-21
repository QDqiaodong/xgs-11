export function formatDate(timestamp) {
    if (!timestamp) return '无';
    const date = new Date(timestamp);
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    const h = String(date.getHours()).padStart(2, '0');
    const min = String(date.getMinutes()).padStart(2, '0');
    return `${y}-${m}-${d} ${h}:${min}`;
}

export function isExpired(timestamp) {
    if (!timestamp) return false;
    return new Date(timestamp) < new Date();
}

export function isNearExpiry(timestamp) {
    if (!timestamp) return false;
    const now = new Date();
    const expiry = new Date(timestamp);
    const diff = expiry - now;
    return diff > 0 && diff < 24 * 60 * 60 * 1000;
}

export function isOverdue(timestamp) {
    if (!timestamp) return false;
    return new Date(timestamp) < new Date();
}

export function isDueToday(timestamp) {
    if (!timestamp) return false;
    const now = new Date();
    const due = new Date(timestamp);
    return now.toDateString() === due.toDateString() && !isOverdue(timestamp);
}

export function isDueWithin24h(timestamp) {
    if (!timestamp) return false;
    const now = new Date();
    const due = new Date(timestamp);
    const diff = due - now;
    return diff > 0 && diff <= 24 * 60 * 60 * 1000;
}

export function isDueWithin24hButNotToday(timestamp) {
    if (!timestamp) return false;
    return isDueWithin24h(timestamp) && !isDueToday(timestamp);
}

export function isDueWithin3Days(timestamp) {
    if (!timestamp) return false;
    const now = new Date();
    const due = new Date(timestamp);
    const diff = due - now;
    return diff > 0 && diff <= 3 * 24 * 60 * 60 * 1000;
}

export function isDueWithin3DaysButNot24h(timestamp) {
    if (!timestamp) return false;
    return isDueWithin3Days(timestamp) && !isDueWithin24h(timestamp);
}

export function getTaskRiskLevel(task) {
    if (task.completed) return 'normal';
    if (isOverdue(task.dueDate)) return 'overdue';
    if (isDueWithin24h(task.dueDate)) return 'urgent';
    if (isDueWithin3Days(task.dueDate)) return 'soon';
    return 'normal';
}

export function getWeekDates(refDate = new Date()) {
    const d = new Date(refDate);
    d.setHours(0, 0, 0, 0);
    const day = d.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    const monday = new Date(d);
    monday.setDate(d.getDate() + diff);
    const dates = [];
    for (let i = 0; i < 7; i++) {
        const date = new Date(monday);
        date.setDate(monday.getDate() + i);
        dates.push(date);
    }
    return dates;
}

export function isSameDay(date1, date2) {
    if (!date1 || !date2) return false;
    const d1 = new Date(date1);
    const d2 = new Date(date2);
    d1.setHours(0, 0, 0, 0);
    d2.setHours(0, 0, 0, 0);
    return d1.getTime() === d2.getTime();
}

export function formatDateShort(timestamp) {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    const h = String(date.getHours()).padStart(2, '0');
    const min = String(date.getMinutes()).padStart(2, '0');
    return `${h}:${min}`;
}

export function formatWeekDay(date) {
    const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    return days[date.getDay()];
}

export function formatMonthDay(date) {
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${m}/${d}`;
}

export function isToday(date) {
    return isSameDay(date, new Date());
}

export function addWeeks(date, n) {
    const d = new Date(date);
    d.setDate(d.getDate() + n * 7);
    return d;
}

export function getWeekRangeText(dates) {
    if (!dates || dates.length === 0) return '';
    const first = dates[0];
    const last = dates[dates.length - 1];
    const m1 = first.getMonth() + 1;
    const d1 = first.getDate();
    const m2 = last.getMonth() + 1;
    const d2 = last.getDate();
    return `${m1}月${d1}日 - ${m2}月${d2}日`;
}

export function getTaskPersonInfo(task, currentUser) {
    if (!task || !currentUser) return null;
    const isCreator = task.userId === currentUser.id;
    const isAssignee = task.assigneeId === currentUser.id;
    if (isCreator) {
        if (task.assigneeId && !isAssignee) {
            return { role: '执行人', name: task.assigneeName || '未知' };
        }
        return { role: '', name: '我自己' };
    }
    return { role: '指派人', name: task.username || '未知' };
}

export function formatTaskPerson(task, currentUser) {
    const info = getTaskPersonInfo(task, currentUser);
    if (!info) return '';
    return info.role ? `${info.role} ${info.name}` : info.name;
}

export function formatDateTimeLocal(timestamp) {
    if (!timestamp) return '';
    const d = new Date(timestamp);
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const h = String(d.getHours()).padStart(2, '0');
    const min = String(d.getMinutes()).padStart(2, '0');
    return `${y}-${m}-${day}T${h}:${min}`;
}

export function getTaskResponsibility(task, currentUser) {
    if (!task || !currentUser) return 'none';
    const isCreator = task.userId === currentUser.id;
    const isAssignee = task.assigneeId === currentUser.id;
    if (isCreator && isAssignee) return 'self';
    if (isCreator) return 'creator';
    if (isAssignee) return 'assignee';
    return 'none';
}

export function needsOverdueReason(task) {
    if (!task || task.completed) return false;
    return isOverdue(task.dueDate);
}

export function getOverdueReasonSummary(reason, maxLen = 24) {
    if (!reason) return '';
    const trimmed = String(reason).trim();
    if (trimmed.length <= maxLen) return trimmed;
    return trimmed.substring(0, maxLen) + '...';
}

export function wasCompletedOverdue(task) {
    if (!task || !task.completed) return false;
    return !!task.overdueReason;
}
