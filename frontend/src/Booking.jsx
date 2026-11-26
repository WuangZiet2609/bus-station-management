import { useEffect, useState } from 'react';
import { Armchair, BusFront, CalendarDays, MapPin, Search, Ticket } from 'lucide-react';
import { api, dateTime, matches, money, normalize, option, tripOption } from './api';
import { Badge, Combo, ErrorMessage, Field, Modal, SearchBox, Table } from './ui';

export function TripSearch({ user, askLogin, resumeBooking, notify, openTickets }) {
  const [data, setData] = useState(null);
  const [filters, setFilters] = useState({ from: '', to: '', date: '' });
  const [applied, setApplied] = useState(filters);
  const [booking, setBooking] = useState(null);
  const [pendingTrip, setPendingTrip] = useState(null);
  const [error, setError] = useState('');
  const [version, setVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    Promise.all([api('/routes', { signal: controller.signal }), api('/trips/search', { signal: controller.signal })])
      .then(([routes, trips]) => { if (!controller.signal.aborted) { setData({ routes, trips }); setError(''); } })
      .catch(err => { if (!controller.signal.aborted) setError(err.message); });
    return () => controller.abort();
  }, [version]);

  useEffect(() => {
    if (user && pendingTrip && resumeBooking) {
      setBooking(pendingTrip);
      setPendingTrip(null);
    }
  }, [user, pendingTrip, resumeBooking]);

  const places = key => [...new Set((data?.routes || []).filter(route => route.isActive).map(route => route[key]).filter(Boolean))]
    .sort((a, b) => a.localeCompare(b, 'vi')).map(place => option(place, place));
  const routes = new Map(data?.routes.map(route => [route.id, route]));
  const trips = (data?.trips || []).filter(trip => {
    const route = routes.get(trip.routeId);
    return (!applied.from || normalize(route?.departureProvince) === normalize(applied.from))
       && (!applied.to || normalize(route?.arrivalProvince) === normalize(applied.to))
       && (!applied.date || trip.departureTime.startsWith(applied.date));
  });
  function choose(trip) {
    if (user) setBooking(trip);
    else { setPendingTrip(trip); askLogin(); }
  }

  return <>
    <section className="booking-hero">
      <div className="hero-content"><h1>Tìm chuyến xe</h1><p>Chọn điểm đi, điểm đến và ngày khởi hành để tìm vé.</p></div>
      <BusFront className="hero-icon" size={158} strokeWidth={0.85} aria-hidden="true" />
      <form className="trip-search" onSubmit={event => { event.preventDefault(); setApplied(filters); }}>
        <Combo label="Điểm đi" options={[option('', 'Tất cả điểm đi'), ...places('departureProvince')]} value={filters.from}
          onChange={from => setFilters(previous => ({ ...previous, from }))} />
        <Combo label="Điểm đến" options={[option('', 'Tất cả điểm đến'), ...places('arrivalProvince')]} value={filters.to}
          onChange={to => setFilters(previous => ({ ...previous, to }))} />
        <Field label="Ngày khởi hành" type="date" value={filters.date} onChange={event => setFilters(previous => ({ ...previous, date: event.target.value }))} />
        <button className="primary" type="submit"><Search size={18} /> Tìm chuyến</button>
      </form>
    </section>
    <div className="section-heading"><h2>Chuyến đang mở bán</h2>{data && <span>{trips.length} chuyến</span>}</div>
    <ErrorMessage error={error} />
    {error && <button className="secondary" onClick={() => setVersion(value => value + 1)}>Tải lại</button>}
    {!data && !error && <div className="panel empty" role="status">Đang tải chuyến xe…</div>}
    {data && !trips.length && <div className="panel empty"><CalendarDays size={29} /><p>Chưa có chuyến phù hợp.</p>
      {(applied.from || applied.to || applied.date) && <button className="secondary" onClick={() => {
        const cleared = { from: '', to: '', date: '' };
        setFilters(cleared);
        setApplied(cleared);
      }}>Xóa bộ lọc</button>}</div>}
    {trips.length > 0 && <div className="trip-list">{trips.map(trip => <article className="trip-card panel" key={trip.id}>
      <div className="trip-route"><div className="heading-icon"><MapPin size={20} /></div><div><h3>{trip.routeName}</h3><small>{trip.tripCode} · {trip.licensePlate}</small></div></div>
      <div><small>Khởi hành</small><strong>{dateTime(trip.departureTime)}</strong></div>
      <div><small>Đến dự kiến</small><strong>{dateTime(trip.arrivalTime)}</strong></div>
      <div><strong>{money(trip.basePrice)}</strong><small>{trip.availableSeats} ghế trống</small></div>
      <button className="primary" onClick={() => choose(trip)}>Chọn ghế</button>
    </article>)}</div>}
    {booking && <SeatPicker trip={booking} close={() => setBooking(null)}
      booked={() => { setBooking(null); setVersion(value => value + 1); notify('Đặt vé thành công.'); openTickets(); }} />}
  </>;
}

function SeatPicker({ trip, close, booked }) {
  const [occupied, setOccupied] = useState(null);
  const [seat, setSeat] = useState(null);
  const [error, setError] = useState('');
  const [version, setVersion] = useState(0);
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    const controller = new AbortController();
    api('/trips/' + trip.id + '/occupied-seats', { signal: controller.signal })
      .then(seats => { if (!controller.signal.aborted) setOccupied(new Set(seats)); })
      .catch(err => { if (!controller.signal.aborted) setError(err.message); });
    return () => controller.abort();
  }, [trip.id, version]);

  async function book() {
    setBusy(true);
    setError('');
    try {
      await api('/tickets', { method: 'POST', body: { tripId: trip.id, seatNumber: seat } });
      booked();
    } catch (err) {
      setError(err.message);
      if (err.status === 409) {
        setSeat(null);
        setOccupied(null);
        setVersion(value => value + 1);
      }
    } finally {
      setBusy(false);
    }
  }

  return <Modal title="Chọn ghế" close={() => { if (!busy) close(); }}>
    <div className="seat-summary"><strong>{trip.routeName}</strong><span>{dateTime(trip.departureTime)} · {money(trip.basePrice)} / ghế</span></div>
    <div className="seat-legend"><span>Ghế trống</span><span>Đang chọn</span><span>Đã đặt</span></div>
    {occupied ? <div className="seat-grid" role="group" aria-label="Danh sách ghế">
      {Array.from({ length: trip.totalSeats }, (_, index) => index + 1).map(number => <button type="button" key={number}
        disabled={occupied.has(number) || busy} aria-label={'Ghế ' + number + (occupied.has(number) ? ', đã đặt' : '')}
        aria-pressed={number === seat} onClick={() => setSeat(number)}><Armchair size={17} /> {number}</button>)}
    </div> : !error && <p role="status">Đang tải sơ đồ ghế…</p>}
    <ErrorMessage error={error} />
    {error && !occupied && <button className="secondary" onClick={() => { setError(''); setVersion(value => value + 1); }}>Tải lại ghế</button>}
    <div className="modal-actions"><button className="secondary" disabled={busy} onClick={close}>Hủy</button>
      <button className="primary" onClick={book} disabled={busy || seat === null || !occupied}>{busy ? 'Đang đặt…' : seat ? 'Đặt ghế ' + seat : 'Đặt vé'}</button></div>
  </Modal>;
}

export function Tickets({ user, manage, notify }) {
  const [data, setData] = useState(null);
  const [query, setQuery] = useState('');
  const [tripId, setTripId] = useState('');
  const [payment, setPayment] = useState('');
  const [error, setError] = useState('');
  const [version, setVersion] = useState(0);
  const [confirm, setConfirm] = useState(null);
  const [paymentError, setPaymentError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    Promise.all([api(manage ? '/tickets/search' : '/tickets/me', { signal: controller.signal }),
      api('/trips', { signal: controller.signal })])
      .then(([tickets, trips]) => { if (!controller.signal.aborted) { setData({ tickets, trips }); setError(''); } })
      .catch(err => { if (!controller.signal.aborted) setError(err.message); });
    return () => controller.abort();
  }, [manage, version]);

  const byId = new Map(data?.trips.map(trip => [trip.id, trip]));
  const rows = (data?.tickets || []).filter(ticket => {
    const trip = byId.get(ticket.tripId);
    return (!payment || ticket.paymentStatus === payment) && (!tripId || ticket.tripId === tripId)
      && matches([ticket.ticketCode, trip?.routeName, trip?.tripCode].join(' '), query);
  });
  const columns = [['Mã vé', ticket => <strong>{ticket.ticketCode}</strong>],
    ['Chuyến', ticket => { const trip = byId.get(ticket.tripId); return <>{trip?.routeName || '—'}<small>{dateTime(trip?.departureTime)}</small></>; }],
    ['Ghế', ticket => ticket.seatNumber], ['Giá vé', ticket => money(ticket.price)],
    ['Tình trạng vé', ticket => <Badge status={ticket.status} />], ['Thanh toán', ticket => <Badge status={ticket.paymentStatus} />]];
  if (!manage) columns.push(['Thao tác', ticket => ticket.userId === user.id && ticket.paymentStatus === 'UNPAID' && ticket.status === 'BOOKED'
    && new Date(byId.get(ticket.tripId)?.departureTime).getTime() > Date.now()
    ? <button className="secondary" onClick={() => { setConfirm(ticket); setPaymentError(''); }}>Thanh toán</button> : '—']);

  async function pay() {
    setBusy(true);
    setPaymentError('');
    try {
      const paid = await api('/tickets/' + confirm.id + '/pay', { method: 'POST' });
      setData(previous => ({ ...previous, tickets: previous.tickets.map(ticket => ticket.id === paid.id ? paid : ticket) }));
      setConfirm(null);
      notify('Đã xác nhận thanh toán.');
    } catch (err) {
      setPaymentError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return <>
    <div className="page-heading"><div className="heading-icon"><Ticket size={23} /></div><h1>{manage ? 'Quản lý vé' : 'Vé của tôi'}</h1></div>
    <section className="panel">
      <div className="panel-toolbar ticket-filters">
        <SearchBox placeholder="Tìm mã vé hoặc tuyến" value={query} onChange={setQuery} />
        <Combo label="Chuyến xe" options={[option('', 'Tất cả chuyến'), ...(data?.trips || []).map(tripOption)]} value={tripId} onChange={setTripId} />
        <Combo label="Thanh toán" options={[option('', 'Tất cả trạng thái'), option('PAID', 'Đã thanh toán'), option('UNPAID', 'Chưa thanh toán')]} value={payment} onChange={setPayment} />
      </div>
      <ErrorMessage error={error} />
      {error && <button className="secondary retry" onClick={() => setVersion(value => value + 1)}>Tải lại</button>}
      {data ? <Table columns={columns} rows={rows} empty="Không có vé phù hợp." /> : !error && <p className="empty" role="status">Đang tải vé…</p>}
    </section>
    {confirm && <Modal title="Xác nhận thanh toán" close={() => { if (!busy) setConfirm(null); }}>
      <p>Vé <strong>{confirm.ticketCode}</strong>, ghế {confirm.seatNumber} · <strong>{money(confirm.price)}</strong></p>
      <p className="muted">Thao tác này ghi nhận thanh toán trong hệ thống.</p>
      <ErrorMessage error={paymentError} />
      <div className="modal-actions"><button className="secondary" disabled={busy} onClick={() => setConfirm(null)}>Hủy</button>
        <button className="primary" disabled={busy} onClick={pay}>{busy ? 'Đang xử lý…' : 'Xác nhận'}</button></div>
    </Modal>}
  </>;
}
