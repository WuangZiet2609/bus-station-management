export async function api(path, { body, ...options } = {}) {
  let response;
  try {
    response = await fetch('/api' + path, {
      credentials: 'same-origin',
      ...options,
      headers: { Accept: 'application/json', ...(body ? { 'Content-Type': 'application/json' } : {}) },
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new Error('Không kết nối được máy chủ. Hãy kiểm tra Spring Boot và thử lại.');
  }
  const data = response.headers.get('content-type')?.includes('application/json')
    ? await response.json() : null;
  if (!response.ok) {
    const details = Object.values(data?.errors || {}).filter(Boolean).join('. ');
    const fallback = { 401: 'Vui lòng đăng nhập.', 403: 'Bạn chưa đăng nhập hoặc không có quyền thực hiện.', 404: 'Không tìm thấy dữ liệu.', 502: 'Máy chủ Spring Boot chưa sẵn sàng.' };
    throw Object.assign(new Error(details || data?.message || fallback[response.status] || 'Yêu cầu thất bại. Vui lòng thử lại.'), { status: response.status });
  }
  return data;
}

export const normalize = (value = '') => String(value ?? '').toLocaleLowerCase('vi')
  .normalize('NFD').replace(/\p{Diacritic}/gu, '').replace(/đ/g, 'd').trim();

export function matches(value, query) {
  const text = normalize(value);
  return normalize(query).split(/\s+/).every(word => text.includes(word));
}

export const money = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
export const dateTime = value => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
  : '—';

export const labels = {
  SCHEDULED: 'Đã lên lịch', DELAYED: 'Hoãn giờ', BOARDING: 'Đang đón khách',
  IN_PROGRESS: 'Đang chạy', COMPLETED: 'Hoàn thành', CANCELLED: 'Đã hủy',
  ACTIVE: 'Hoạt động', MAINTENANCE: 'Bảo dưỡng', INACTIVE: 'Ngừng hoạt động', RETIRED: 'Ngừng sử dụng',
  BOOKED: 'Đã đặt', CONFIRMED: 'Đã xác nhận', PAID: 'Đã thanh toán', UNPAID: 'Chưa thanh toán',
  ADMIN: 'Quản trị viên', EMPLOYEE: 'Nhân viên', CUSTOMER: 'Khách hàng', DRIVER: 'Tài xế',
};

export const option = (value, label, detail = '') => ({ value, label, detail });
export const tripOption = trip => option(trip.id, trip.routeName, [trip.tripCode, dateTime(trip.departureTime), trip.licensePlate].filter(Boolean).join(' · '));
