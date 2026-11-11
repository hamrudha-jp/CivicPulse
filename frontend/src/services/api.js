// CivicPulse API Client

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export function getAuthToken() {
  return localStorage.getItem('civicpulse_token');
}

export function setAuthToken(token) {
  if (token) {
    localStorage.setItem('civicpulse_token', token);
  } else {
    localStorage.removeItem('civicpulse_token');
  }
}

export function getStoredUser() {
  const user = localStorage.getItem('civicpulse_user');
  if (!user) return null;
  const parsed = JSON.parse(user);
  if (parsed && parsed.userId && !parsed.id) {
    parsed.id = parsed.userId;
  }
  return parsed;
}

export function setStoredUser(user) {
  if (user) {
    if (user.userId && !user.id) {
      user.id = user.userId;
    }
    localStorage.setItem('civicpulse_user', JSON.stringify(user));
  } else {
    localStorage.removeItem('civicpulse_user');
  }
}

async function request(endpoint, options = {}) {
  const url = `${BASE_URL}${endpoint}`;
  const token = getAuthToken();

  const headers = { ...options.headers };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  // If not FormData, default to application/json
  if (!(options.body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(url, { ...options, headers });
  const data = await response.json();

  if (!response.ok) {
    throw new Error(data.message || `Request failed with status ${response.status}`);
  }

  return data;
}

export const api = {
  auth: {
    login: async (email, password) => {
      const res = await request('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password })
      });
      if (res.data?.token) {
        setAuthToken(res.data.token);
        setStoredUser(res.data);
      }
      return res.data;
    },
    register: async (registerData) => {
      const res = await request('/auth/register', {
        method: 'POST',
        body: JSON.stringify(registerData)
      });
      if (res.data?.token) {
        setAuthToken(res.data.token);
        setStoredUser(res.data);
      }
      return res.data;
    },
    me: async () => {
      return (await request('/auth/me')).data;
    },
    logout: () => {
      setAuthToken(null);
      setStoredUser(null);
    }
  },

  departments: {
    getAll: async () => (await request('/departments')).data,
    getCategories: async (deptId) => (await request(`/departments/${deptId}/categories`)).data
  },

  complaints: {
    submitJson: async (payload) => {
      return (await request('/complaints/json', {
        method: 'POST',
        body: JSON.stringify(payload)
      })).data;
    },
    submitWithPhoto: async (payload, file) => {
      const formData = new FormData();
      formData.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
      if (file) {
        formData.append('photo', file);
      }
      return (await request('/complaints', {
        method: 'POST',
        body: formData
      })).data;
    },
    track: async (trackingNumber) => (await request(`/complaints/track/${trackingNumber}`)).data,
    getById: async (id) => (await request(`/complaints/${id}`)).data,
    getMy: async (page = 0, size = 10) => (await request(`/complaints/my?page=${page}&size=${size}`)).data,
    getPublicFeed: async (params = {}) => {
      const query = new URLSearchParams(params).toString();
      return (await request(`/complaints/public-feed${query ? '?' + query : ''}`)).data;
    },
    upvote: async (id) => (await request(`/complaints/${id}/upvote`, { method: 'POST' })).data,
    cancel: async (id) => (await request(`/complaints/${id}/cancel`, { method: 'PATCH' })).data
  },

  official: {
    getQueue: async (params = {}) => {
      const query = new URLSearchParams(params).toString();
      return (await request(`/official/complaints${query ? '?' + query : ''}`)).data;
    },
    getAssignees: async (deptId) => {
      const endpoint = deptId ? `/official/assignees?departmentId=${deptId}` : '/official/assignees';
      return (await request(endpoint)).data;
    },
    assign: async (complaintId, officialId) => {
      return (await request(`/official/complaints/${complaintId}/assign`, {
        method: 'POST',
        body: JSON.stringify({ officialId })
      })).data;
    },
    updateStatus: async (complaintId, status, remarks) => {
      return (await request(`/official/complaints/${complaintId}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status, remarks })
      })).data;
    },
    resolveJson: async (complaintId, remarks) => {
      return (await request(`/official/complaints/${complaintId}/resolve/json`, {
        method: 'POST',
        body: JSON.stringify({ remarks })
      })).data;
    },
    resolveWithPhoto: async (complaintId, remarks, proofPhoto) => {
      const formData = new FormData();
      formData.append('data', new Blob([JSON.stringify({ remarks })], { type: 'application/json' }));
      if (proofPhoto) {
        formData.append('proofPhoto', proofPhoto);
      }
      return (await request(`/official/complaints/${complaintId}/resolve`, {
        method: 'POST',
        body: formData
      })).data;
    }
  },

  duplicates: {
    getPending: async (deptId) => {
      const endpoint = deptId ? `/duplicates/pending?departmentId=${deptId}` : '/duplicates/pending';
      return (await request(endpoint)).data;
    },
    review: async (matchId, action, remarks) => {
      return (await request(`/duplicates/${matchId}/review`, {
        method: 'POST',
        body: JSON.stringify({ action, remarks })
      }));
    }
  },

  analytics: {
    getDashboard: async (deptId) => {
      const endpoint = deptId ? `/analytics/dashboard?departmentId=${deptId}` : '/analytics/dashboard';
      return (await request(endpoint)).data;
    }
  },

  admin: {
    triggerEscalation: async () => (await request('/admin/escalation/trigger', { method: 'POST' })).data,
    getOfficials: async (deptId) => {
      const endpoint = deptId ? `/admin/officials?departmentId=${deptId}` : '/admin/officials';
      return (await request(endpoint)).data;
    }
  }
};
