// Keep local development same-origin; Vite proxies /api to the gateway.
// Deployments can still provide VITE_API_URL with the public gateway URL.
const GATEWAY_URL = import.meta.env.VITE_API_URL || (import.meta.env.DEV ? '/api' : 'http://localhost:8080');

async function request(path, options = {}, retriedAfterRefresh = false) {
  const token = localStorage.getItem('accessToken');
  const user = JSON.parse(localStorage.getItem('user') || 'null');
  const headers = {
    ...(options.body || (options.method && options.method !== 'GET') ? { 'Content-Type': 'application/json' } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(user?.userUuid ? { 'X-User-Id': user.userUuid } : {}),
    ...options.headers,
  };

  let response;
  try {
    response = await fetch(`${GATEWAY_URL}${path}`, {
      ...options,
      headers,
    });
  } catch (error) {
    const endpoint = `${GATEWAY_URL}${path}`;
    throw new Error(
      `Unable to reach the API at ${endpoint}. Start the API gateway and required services, then try again.`,
      { cause: error }
    );
  }
  const payload = response.status === 204 ? null : await response.json().catch(() => ({}));
  if (!response.ok) {
    if (response.status === 401 && !path.startsWith('/auth/') && !retriedAfterRefresh) {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        try {
          const refreshed = await request('/auth/refresh', { method: 'POST', body: JSON.stringify({ refreshToken }) });
          localStorage.setItem('accessToken', refreshed.accessToken);
          localStorage.setItem('refreshToken', refreshed.refreshToken);
          return request(path, options, true);
        } catch (_) {
          // The normal 401 handling below clears the stale local session.
        }
      }
    }
    if (response.status === 401 && !path.startsWith('/auth/')) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    }
    const errorMsg =
      response.status === 401 && !path.startsWith('/auth/')
        ? 'Your session has expired. Please sign in again before making a booking.'
        :
      payload?.message ||
      (Array.isArray(payload?.errors) && payload.errors.map(e => e.defaultMessage || e).join(', ')) ||
      payload?.error ||
      (typeof payload === 'object' && Object.values(payload || {}).find(v => typeof v === 'string')) ||
      'Request failed';
    throw new Error(errorMsg);
  }
  return payload?.data ?? payload;
}

export const api = {
  login: (body) => request('/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  register: (body) => request('/auth/register', { method: 'POST', body: JSON.stringify(body) }),
  forgotPassword: (email) => request('/auth/forgot-password', { method: 'POST', body: JSON.stringify({ email }) }),
  resetPassword: (token, password) => request('/auth/reset-password', { method: 'POST', body: JSON.stringify({ token, password }) }),
  events: () => request('/event'),
  event: (eventUuid) => request(`/event/${eventUuid}`),
  organizerEvents: () => request('/event/organizer/me'),
  createEvent: (body) => request('/event', { method: 'POST', body: JSON.stringify(body) }),
  updateEvent: (eventUuid, body) => request(`/event/${eventUuid}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteEvent: (eventUuid) => request(`/event/${eventUuid}`, { method: 'DELETE' }),
  user: (userUuid) => request(`/user/${userUuid}`),
  createUser: (body) => request('/user', { method: 'POST', body: JSON.stringify(body) }),
  updateUser: (userUuid, body) => request(`/user/${userUuid}`, { method: 'PUT', body: JSON.stringify(body) }),
  bookings: (userUuid) => request(`/booking/user/${userUuid}`),
  users: () => request('/user'),
  adminAnalytics: () => request('/booking/admin/analytics'),
  bookedSeats: (eventUuid) => request(`/booking/event/${eventUuid}/seats`),
  calculateSeatPrices: (eventUuid, seats) => request(`/booking/event/${eventUuid}/seat-prices`, { method: 'POST', body: JSON.stringify(seats) }),
  eventBookings: (eventUuid) => request(`/booking/event/${eventUuid}`),
  createBooking: (body) => request('/booking', { method: 'POST', body: JSON.stringify(body) }),
  releaseBooking: (bookingUUID) => request(`/booking/${bookingUUID}/release`, { method: 'POST' }),
  releaseSeats: (eventUuid, seats) => request(`/booking/event/${eventUuid}/release-seats`, { method: 'POST', body: JSON.stringify(seats) }),
  createPaymentOrder: (bookingUUID) => request('/payment/create-order', { method: 'POST', body: JSON.stringify({ bookingUUID }) }),
  verifyPayment: (body) => request('/payment/verify', { method: 'POST', body: JSON.stringify(body) }),
  failPayment: (body) => request('/payment/fail', { method: 'POST', body: JSON.stringify(body) }),
};
