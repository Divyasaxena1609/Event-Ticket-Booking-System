const GATEWAY_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

async function request(path, options = {}) {
  const token = localStorage.getItem('accessToken');
  const user = JSON.parse(localStorage.getItem('user') || 'null');
  const response = await fetch(`${GATEWAY_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(user?.userUuid ? { 'X-User-Id': user.userUuid } : {}),
      ...options.headers,
    },
    ...options,
  });
  const payload = response.status === 204 ? null : await response.json().catch(() => ({}));
  if (!response.ok) {
    const errorMsg =
      payload?.message ||
      (typeof payload === 'object' && Object.values(payload || {}).find(v => typeof v === 'string')) ||
      'Request failed';
    throw new Error(errorMsg);
  }
  return payload?.data ?? payload;
}

export const api = {
  login: (body) => request('/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  register: (body) => request('/auth/register', { method: 'POST', body: JSON.stringify(body) }),
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
