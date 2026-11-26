import { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { Bus, BusFront, CalendarDays, CheckCircle2, LayoutDashboard, LogIn, LogOut, Map, Search, Ticket, UserCircle2, Users, X } from 'lucide-react';
import { api, dateTime, labels } from './api';
import { Badge, ErrorMessage, Field, Modal, Table } from './ui';
import { Tickets, TripSearch } from './Booking';
import Operations from './Operations';
import './styles.css';

const publicPages = [{ id: 'search', name: 'Tìm chuyến', icon: Search }];
const operations = [
  { id: 'dashboard', name: 'Tổng quan', icon: LayoutDashboard },
  { id: 'trips', name: 'Chuyến xe', icon: CalendarDays },
  { id: 'routes', name: 'Tuyến đường', icon: Map },
  { id: 'vehicles', name: 'Đội xe', icon: Bus },
  { id: 'drivers', name: 'Tài xế', icon: Users },
];

function Dashboard({ navigate }) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [version, setVersion] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    Promise.all(['trips', 'routes', 'vehicles', 'tickets/search'].map(path => api('/' + path, { signal: controller.signal })))
      .then(([trips, routes, vehicles, tickets]) => {
        if (!controller.signal.aborted) { setData({ trips, routes, vehicles, tickets }); setError(''); }
      })
      .catch(err => { if (!controller.signal.aborted) setError(err.message); });
    return () => controller.abort();
  }, [version]);

  const upcoming = (data?.trips || []).filter(trip => new Date(trip.departureTime).getTime() > Date.now()
    && !['COMPLETED', 'CANCELLED'].includes(trip.status));
  const metrics = [
    { id: 'trips', title: 'Chuyến sắp khởi hành', count: upcoming.length, icon: CalendarDays, theme: 'blue' },
    { id: 'routes', title: 'Tuyến đang hoạt động', count: data?.routes.filter(route => route.isActive).length, icon: Map, theme: 'green' },
    { id: 'vehicles', title: 'Xe đang hoạt động', count: data?.vehicles.filter(vehicle => vehicle.status === 'ACTIVE').length, icon: Bus, theme: 'amber' },
    { id: 'all-tickets', title: 'Vé chưa thanh toán', count: data?.tickets.filter(ticket => ticket.paymentStatus === 'UNPAID').length, icon: Ticket, theme: 'purple' },
  ];
  const columns = [['Chuyến', trip => <><strong>{trip.routeName}</strong><small>{trip.tripCode}</small></>],
    ['Khởi hành', trip => dateTime(trip.departureTime)], ['Biển số', trip => trip.licensePlate],
    ['Tài xế chính', trip => trip.driverName || 'Chưa phân công'], ['Ghế còn lại', trip => trip.availableSeats ?? '—'],
    ['Trạng thái', trip => <Badge status={trip.status} />]];

  return <>
    <div className="page-heading"><div className="heading-icon"><LayoutDashboard size={23} /></div><h1>Tổng quan vận hành</h1></div>
    <ErrorMessage error={error} />
    {error && <button className="secondary" onClick={() => setVersion(value => value + 1)}>Tải lại</button>}
    {!data && !error && <p role="status">Đang tải tổng quan…</p>}
    {data && <>
      <div className="metrics">{metrics.map(({ id, title, count, icon: Icon, theme }) =>
        <button className="metric panel" key={id} onClick={() => navigate(id)}><span className={'metric-icon ' + theme}><Icon size={21} /></span>
          <span>{title}</span><strong>{count}</strong></button>)}</div>
      <section className="panel"><div className="panel-toolbar"><h2>Lịch khởi hành sắp tới</h2>
        <button className="secondary" onClick={() => navigate('trips')}>Xem lịch chuyến</button></div>
        <Table columns={columns} rows={upcoming.slice(0, 8)} empty="Chưa có chuyến sắp khởi hành." /></section>
    </>}
  </>;
}

function Auth({ close, signedIn }) {
  const [register, setRegister] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const body = Object.fromEntries(form);
    body.email = body.email.trim();
    setBusy(true);
    setError('');
    try {
      if (register) {
        await api('/auth/register', { method: 'POST', body: { ...body, fullName: body.fullName.trim(), phone: body.phone.trim() } });
        setRegister(false);
      }
      const result = await api('/auth/login', { method: 'POST', body: { email: body.email, password: body.password } });
      signedIn(result.user);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }
  return <Modal title={register ? 'Đăng ký tài khoản' : 'Đăng nhập'} close={() => { if (!busy) close(); }}>
    <form onSubmit={submit} className="auth-form">
      {register && <><Field label="Họ tên" name="fullName" autoComplete="name" required />
        <Field label="Số điện thoại" name="phone" type="tel" autoComplete="tel" required /></>}
      <Field label="Email" name="email" type="email" autoComplete="username" required />
      <Field label="Mật khẩu" name="password" type="password" required minLength={register ? 8 : undefined}
        autoComplete={register ? 'new-password' : 'current-password'} />
      <ErrorMessage error={error} />
      <button className="primary" disabled={busy}>{busy ? 'Đang xử lý…' : register ? 'Tạo tài khoản' : 'Đăng nhập'}</button>
      <button type="button" className="text-button" disabled={busy} onClick={() => { setRegister(value => !value); setError(''); }}>
        {register ? 'Đã có tài khoản? Đăng nhập' : 'Chưa có tài khoản? Đăng ký'}
      </button>
    </form>
  </Modal>;
}

function App() {
  const [user, setUser] = useState(null);
  const [location, setLocation] = useState(() => window.location.hash.slice(1));
  const [auth, setAuth] = useState(false);
  const [loginForBooking, setLoginForBooking] = useState(false);
  const [sessionReady, setSessionReady] = useState(false);
  const [banner, setBanner] = useState(null);
  const [signingOut, setSigningOut] = useState(false);
  const admin = user?.role === 'ADMIN';
  const staff = admin || user?.role === 'EMPLOYEE';
  const customerPages = [...publicPages, ...(user ? [{ id: 'tickets', name: 'Vé của tôi', icon: Ticket }] : [])];
  const staffPages = [...(admin ? operations : []), ...(staff ? [{ id: 'all-tickets', name: 'Quản lý vé', icon: Ticket }] : [])];
  const page = [...customerPages, ...staffPages].some(item => item.id === location) ? location : admin ? 'dashboard' : 'search';

  useEffect(() => {
    const controller = new AbortController();
    api('/auth/me', { signal: controller.signal })
      .then(setUser)
      .catch(err => {
        if (!controller.signal.aborted && ![401, 403].includes(err.status)) setBanner({ error: true, message: err.message });
      }).finally(() => { if (!controller.signal.aborted) setSessionReady(true); });
    const update = () => setLocation(window.location.hash.slice(1));
    window.addEventListener('hashchange', update);
    return () => { controller.abort(); window.removeEventListener('hashchange', update); };
  }, []);

  useEffect(() => {
    if (!banner || banner.error) return;
    const timer = setTimeout(() => setBanner(null), 4500);
    return () => clearTimeout(timer);
  }, [banner]);

  function navigate(id) {
    window.location.hash = id;
    setLocation(id);
  }

  const notify = message => setBanner({ message });
  async function logout() {
    setSigningOut(true);
    try {
      await api('/auth/logout', { method: 'POST' });
      setUser(null);
      navigate('search');
      notify('Đã đăng xuất.');
    } catch (err) {
      setBanner({ error: true, message: err.message });
    } finally {
      setSigningOut(false);
    }
  }
  const links = items => items.map(({ id, name, icon: Icon }) => <a key={id} href={'#' + id}
    aria-current={page === id ? 'page' : undefined} onClick={() => setLocation(id)}><Icon size={19} />{name}</a>);

  return <div className="shell">
    <header className="topbar">
      <a className="brand" href={admin ? '#dashboard' : '#search'}><span><BusFront size={23} /></span>Bến Xe</a>
      <div className="account">
        {user ? <><UserCircle2 size={23} className="account-icon" /><div><strong>{user.fullName}</strong><small>{labels[user.role] || user.role}</small></div>
          <button className="top-button" aria-label="Đăng xuất" title="Đăng xuất" onClick={logout} disabled={signingOut}><LogOut size={18} /></button></>
          : <button className="top-button" disabled={!sessionReady} onClick={() => { setLoginForBooking(false); setAuth(true); }}><LogIn size={18} /> Đăng nhập</button>}
      </div>
    </header>
    <div className="layout">
      <aside className="sidebar"><nav aria-label="Điều hướng chính">
        {staffPages.length > 0 && <div className="nav-group"><h2>Vận hành</h2>{links(staffPages)}</div>}
        <div className="nav-group"><h2>Đặt vé</h2>{links(customerPages)}</div>
      </nav></aside>
      <main>
        {page === 'search' && <TripSearch user={user} resumeBooking={loginForBooking} notify={notify} openTickets={() => navigate('tickets')}
          askLogin={() => { navigate('search'); setLoginForBooking(true); setAuth(true); }} />}
        {page === 'dashboard' && <Dashboard navigate={navigate} />}
        {['trips', 'routes', 'vehicles', 'drivers'].includes(page) && <Operations key={page} resource={page} notify={notify} />}
        {['tickets', 'all-tickets'].includes(page) && <Tickets key={page} user={user} manage={page === 'all-tickets'} notify={notify} />}
      </main>
    </div>
    {auth && <Auth close={() => { setAuth(false); setLoginForBooking(false); }} signedIn={account => {
      setUser(account);
      setAuth(false);
      if (account.role === 'ADMIN' && !loginForBooking) navigate('dashboard');
      notify('Đăng nhập thành công.');
    }} />}
    {banner && <div className={'toast ' + (banner.error ? 'toast-error' : '')} role={banner.error ? 'alert' : 'status'}>
      {!banner.error && <CheckCircle2 size={19} />} {banner.message}
      <button type="button" className="icon-button" aria-label="Đóng thông báo" onClick={() => setBanner(null)}><X size={18} /></button>
    </div>}
  </div>;
}

createRoot(document.getElementById('root')).render(<App />);
