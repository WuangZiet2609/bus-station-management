import { useEffect, useState } from 'react';
import { Bus, CalendarDays, Map, Pencil, Plus, Trash2, Users } from 'lucide-react';
import { api, dateTime, labels, matches, money, option } from './api';
import { Badge, Combo, ErrorMessage, Field, Modal, SearchBox, Table } from './ui';

const catalogs = {
  trips: {
    title: 'Chuyến xe', singular: 'chuyến xe', icon: CalendarDays,
    fields: [
      ['tripCode', 'Mã chuyến', 'text'],
      ['routeId', 'Tuyến đường', 'combo', true],
      ['vehicleId', 'Xe', 'combo', true],
      ['driverId', 'Tài xế chính', 'combo'],
      ['coDriverId', 'Tài xế phụ', 'combo'],
      ['departureTime', 'Giờ khởi hành', 'datetime-local', true],
      ['arrivalTime', 'Giờ đến dự kiến', 'datetime-local', true],
      ['basePrice', 'Giá vé (₫)', 'number', true, 1],
      ['notes', 'Ghi chú', 'text'],
    ],
    columns: [['Chuyến', row => <><strong>{row.routeName}</strong><small>{row.tripCode}</small></>],
      ['Khởi hành', row => dateTime(row.departureTime)], ['Xe / Tài xế', row => <>{row.licensePlate}<small>{row.driverName || 'Chưa phân công'}</small></>],
      ['Ghế trống', row => (row.availableSeats ?? '—') + ' / ' + row.totalSeats],
      ['Giá vé', row => money(row.basePrice)], ['Trạng thái', row => <Badge status={row.status} />]],
  },
  routes: {
    title: 'Tuyến đường', singular: 'tuyến', icon: Map,
    fields: [
      ['name', 'Tên tuyến', 'text', true],
      ['code', 'Mã tuyến', 'text'],
      ['departureProvince', 'Điểm đi', 'text', true],
      ['arrivalProvince', 'Điểm đến', 'text', true],
      ['distanceKm', 'Khoảng cách (km)', 'number', false, 0.01, 0.01],
      ['estimatedDurationMinutes', 'Thời gian dự kiến (phút)', 'number', false, 1],
      ['isActive', 'Trạng thái', 'combo', true, undefined, undefined, true],
    ],
    columns: [['Tên tuyến', row => <><strong>{row.name}</strong><small>{row.code}</small></>],
      ['Điểm đi', row => row.departureProvince], ['Điểm đến', row => row.arrivalProvince],
      ['Khoảng cách', row => row.distanceKm == null ? '—' : row.distanceKm + ' km'],
      ['Trạng thái', row => <Badge status={row.isActive ? 'ACTIVE' : 'INACTIVE'} />]],
  },
  vehicles: {
    title: 'Đội xe', singular: 'xe', icon: Bus,
    fields: [
      ['licensePlate', 'Biển số', 'text', true], ['name', 'Tên xe', 'text'],
      ['brand', 'Hãng xe', 'text'], ['model', 'Dòng xe', 'text'],
      ['seatCount', 'Số ghế', 'number', true, 1],
      ['status', 'Trạng thái', 'combo', true, undefined, undefined, true],
    ],
    columns: [['Biển số', row => <><strong>{row.licensePlate}</strong><small>{row.vehicleCode}</small></>],
      ['Xe', row => row.name || '—'], ['Hãng / Dòng xe', row => [row.brand, row.model].filter(Boolean).join(' / ') || '—'],
      ['Số ghế', row => row.seatCount], ['Trạng thái', row => <Badge status={row.status} />]],
  },
  drivers: {
    title: 'Tài xế', singular: 'tài xế', icon: Users,
    fields: [['fullName', 'Họ tên', 'text', true], ['email', 'Email', 'email', true], ['phone', 'Điện thoại', 'tel'],
      ['password', 'Mật khẩu', 'password']],
    columns: [['Họ tên', row => <><strong>{row.fullName}</strong><small>{row.customerCode}</small></>],
      ['Email', row => row.email], ['Điện thoại', row => row.phone || '—']],
  },
};

function Editor({ resource, row, references, close, saved }) {
  const config = catalogs[resource];
  const edit = Boolean(row);
  const fields = config.fields.filter(field => !field[6] || edit);
  const [values, setValues] = useState(() => Object.fromEntries(fields.map(([key, , type]) =>
    [key, type === 'datetime-local' ? row?.[key]?.slice(0, 16) || '' : row?.[key] ?? ''])));
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const drivers = (references.drivers || []).map(driver => option(driver.id, driver.fullName, [driver.customerCode, driver.phone, driver.email].filter(Boolean).join(' · ')));
  const options = {
    routeId: (references.routes || []).filter(route => route.isActive || route.id === row?.routeId)
      .map(route => option(route.id, route.name, [route.code, route.departureProvince, route.arrivalProvince].join(' · '))),
    vehicleId: (references.vehicles || []).filter(vehicle => vehicle.status === 'ACTIVE' || vehicle.id === row?.vehicleId)
      .map(vehicle => option(vehicle.id, vehicle.licensePlate, [vehicle.name, vehicle.vehicleCode, vehicle.seatCount + ' ghế'].filter(Boolean).join(' · '))),
    driverId: [...(row?.driverId ? [] : [option('', 'Chưa phân công')]), ...drivers.filter(driver => driver.value !== values.coDriverId)],
    coDriverId: [...(row?.coDriverId ? [] : [option('', 'Chưa phân công')]), ...drivers.filter(driver => driver.value !== values.driverId)],
    isActive: [option(true, 'Hoạt động'), option(false, 'Ngừng hoạt động')],
    status: ['ACTIVE', 'MAINTENANCE', 'INACTIVE', 'RETIRED'].map(status => option(status, labels[status])),
  };

  async function submit(event) {
    event.preventDefault();
    setError('');
    const body = {};
    for (const [key, label, type, required] of fields) {
      const value = typeof values[key] === 'string' ? values[key].trim() : values[key];
      if (value === '') {
        if (required) {
          setError('Vui lòng nhập hoặc chọn ' + label.toLocaleLowerCase('vi') + '.');
          return;
        }
        if (resource === 'drivers' && key === 'phone') body[key] = null;
        else if (edit && ['notes', 'name', 'brand', 'model'].includes(key)) body[key] = '';
      } else if (type === 'datetime-local' && edit && row[key]?.slice(0, 16) === value) {
        body[key] = row[key];
      } else {
        body[key] = type === 'number' ? Number(value) : value;
      }
    }
    setBusy(true);
    try {
      await api('/' + resource + (edit ? '/' + row.id : ''), { method: edit ? 'PUT' : 'POST', body });
      saved(edit ? 'Đã lưu thay đổi.' : 'Đã thêm ' + config.singular + '.');
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return <Modal title={(edit ? 'Sửa ' : 'Thêm ') + config.singular} close={() => { if (!busy) close(); }}>
    <form onSubmit={submit}>
      <div className="form-grid">
        {fields.map(([key, label, type, required, min, step]) => {
          const change = value => setValues(previous => ({ ...previous, [key]: value }));
          if (type === 'combo') {
            return <Combo key={key} label={label} required={required} value={values[key]} options={options[key]} onChange={change} />;
          }
          return <Field key={key} label={label} type={type} required={required || (key === 'password' && !edit)}
            min={min} step={step} minLength={key === 'password' ? 8 : undefined}
            autoComplete={key === 'password' ? 'new-password' : undefined}
            placeholder={key === 'password' && edit ? 'Để trống để giữ mật khẩu hiện tại' : undefined}
            value={values[key]} onChange={event => change(event.target.value)} />;
        })}
      </div>
      <ErrorMessage error={error} />
      <div className="modal-actions"><button type="button" className="secondary" disabled={busy} onClick={close}>Hủy</button>
        <button className="primary" disabled={busy}>{busy ? 'Đang lưu…' : 'Lưu'}</button></div>
    </form>
  </Modal>;
}

export default function Operations({ resource, notify }) {
  const [data, setData] = useState(null);
  const [query, setQuery] = useState('');
  const [version, setVersion] = useState(0);
  const [error, setError] = useState('');
  const [editor, setEditor] = useState(null);
  const [remove, setRemove] = useState(null);
  const [removeError, setRemoveError] = useState('');
  const [deleting, setDeleting] = useState(false);
  const config = catalogs[resource];

  useEffect(() => {
    const controller = new AbortController();
    const names = resource === 'trips' ? ['trips', 'routes', 'vehicles', 'drivers'] : [resource];
    Promise.all(names.map(async name => [name, await api('/' + name, { signal: controller.signal })]))
      .then(entries => { if (!controller.signal.aborted) { setData(Object.fromEntries(entries)); setError(''); } })
      .catch(err => { if (!controller.signal.aborted) setError(err.message); });
    return () => controller.abort();
  }, [resource, version]);

  async function confirmDelete() {
    setDeleting(true);
    setRemoveError('');
    try {
      await api('/' + resource + '/' + remove.id, { method: 'DELETE' });
      setRemove(null);
      setVersion(value => value + 1);
      notify('Đã xóa ' + config.singular + '.');
    } catch (err) {
      setRemoveError(err.message);
    } finally {
      setDeleting(false);
    }
  }

  const rows = (data?.[resource] || []).filter(row =>
    matches(Object.values(row).filter(value => typeof value === 'string').map(value => labels[value] || value).join(' '), query));
  const columns = [...config.columns, ['Thao tác', row => <div className="row-actions">
    <button type="button" className="icon-button" title="Sửa" aria-label={'Sửa ' + (row.tripCode || row.licensePlate || row.name || row.fullName)} onClick={() => setEditor(row)}><Pencil size={17} /></button>
    <button type="button" className="icon-button danger-text" title="Xóa" aria-label={'Xóa ' + (row.tripCode || row.licensePlate || row.name || row.fullName)}
      onClick={() => { setRemoveError(''); setRemove(row); }}><Trash2 size={17} /></button>
  </div>]];
  const Icon = config.icon;

  return <>
    <div className="page-heading"><div className="heading-icon"><Icon size={23} /></div><h1>{config.title}</h1></div>
    <section className="panel">
      <div className="panel-toolbar">
        <SearchBox value={query} onChange={setQuery} placeholder={'Tìm ' + config.singular} />
        <button className="primary" disabled={!data} onClick={() => setEditor({})}><Plus size={18} /> Thêm {config.singular}</button>
      </div>
      <ErrorMessage error={error} />
      {error && <button className="secondary retry" onClick={() => setVersion(value => value + 1)}>Tải lại</button>}
      {data ? <Table columns={columns} rows={rows} /> : !error && <p className="empty" role="status">Đang tải dữ liệu…</p>}
    </section>
    {editor && <Editor resource={resource} row={editor.id ? editor : null} references={data} close={() => setEditor(null)}
      saved={message => { setEditor(null); setVersion(value => value + 1); notify(message); }} />}
    {remove && <Modal title={'Xóa ' + config.singular} close={() => { if (!deleting) setRemove(null); }}>
      <p>Bạn có chắc muốn xóa <strong>{remove.tripCode || remove.licensePlate || remove.name || remove.fullName}</strong>?</p>
      <ErrorMessage error={removeError} />
      <div className="modal-actions"><button className="secondary" disabled={deleting} onClick={() => setRemove(null)}>Hủy</button>
        <button className="danger" disabled={deleting} onClick={confirmDelete}>{deleting ? 'Đang xóa…' : 'Xóa'}</button></div>
    </Modal>}
  </>;
}
