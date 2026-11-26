import { useEffect, useId, useRef, useState } from 'react';
import { ChevronDown, Search, X } from 'lucide-react';
import { labels, matches } from './api';

export function Combo({ label, options, value, onChange, required, placeholder = 'Gõ để tìm hoặc chọn' }) {
  const id = useId();
  const input = useRef(null);
  const list = useRef(null);
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [active, setActive] = useState(-1);
  const selected = options.find(item => item.value === value);
  const filtered = options.filter(item => matches(item.label + ' ' + (item.detail || ''), query));
  const visibleValue = open ? query : selected?.label || '';

  useEffect(() => {
    if (open && active >= 0) list.current?.children[active]?.scrollIntoView({ block: 'nearest' });
  }, [active, open]);

  function pick(item) {
    onChange(item.value);
    setOpen(false);
    setActive(-1);
  }

  function onKeyDown(event) {
    if (event.key === 'Escape' && open) {
      event.preventDefault();
      event.stopPropagation();
      setOpen(false);
    } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      if (!open) {
        setQuery('');
        setOpen(true);
        setActive(event.key === 'ArrowDown' ? 0 : options.length - 1);
      } else {
        const direction = event.key === 'ArrowDown' ? 1 : -1;
        setActive(index => Math.max(0, Math.min(index + direction, filtered.length - 1)));
      }
    } else if (event.key === 'Enter' && open) {
      event.preventDefault();
      if (filtered[active]) pick(filtered[active]);
    }
  }

  return <div className="field">
    <label htmlFor={id}>{label}{required && <span className="required" aria-hidden="true"> *</span>}</label>
    <div className="combo" onBlur={event => {
      if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false);
    }}>
      <Search size={16} className="combo-search" aria-hidden="true" />
      <input id={id} ref={input} role="combobox" autoComplete="off" aria-expanded={open}
        aria-controls={open ? id + '-list' : undefined} aria-autocomplete="list" aria-required={required}
        aria-activedescendant={open && filtered[active] ? id + '-' + active : undefined}
        placeholder={open && selected ? selected.label : placeholder} value={visibleValue}
        onFocus={() => { setQuery(''); setActive(-1); setOpen(true); }}
        onClick={() => { if (!open) { setQuery(''); setOpen(true); setActive(-1); } }}
        onChange={event => { setQuery(event.target.value); setOpen(true); setActive(-1); }}
        onKeyDown={onKeyDown} />
      <button type="button" className="combo-toggle" aria-label={open ? 'Đóng danh sách' : 'Mở danh sách'}
        onMouseDown={event => event.preventDefault()}
        onClick={() => { if (open) setOpen(false); else { input.current.focus(); setQuery(''); setActive(-1); setOpen(true); } }}>
        <ChevronDown size={17} />
      </button>
      {open && <div className="combo-popup">
        <ul id={id + '-list'} role="listbox" aria-label={label} ref={list}>
          {filtered.map((item, index) => <li key={item.value} id={id + '-' + index} role="option"
            aria-selected={item.value === value} className={index === active ? 'highlight' : ''}
            onMouseDown={event => event.preventDefault()} onClick={() => pick(item)}>
            <span>{item.label}</span>{item.detail && <small>{item.detail}</small>}
          </li>)}
        </ul>
        {!filtered.length && <p className="combo-empty" role="status">Không có lựa chọn phù hợp</p>}
      </div>}
    </div>
  </div>;
}

export function Field({ label, ...props }) {
  const id = useId();
  return <div className="field">
    <label htmlFor={id}>{label}{props.required && <span className="required" aria-hidden="true"> *</span>}</label>
    <input id={id} {...props} />
  </div>;
}

export function SearchBox({ value, onChange, placeholder }) {
  return <label className="searchbox">
    <Search size={17} aria-hidden="true" />
    <input type="search" aria-label={placeholder} value={value} onChange={event => onChange(event.target.value)} placeholder={placeholder} />
  </label>;
}

export function Modal({ title, children, close }) {
  const dialog = useRef(null);
  const id = useId();
  useEffect(() => {
    const element = dialog.current;
    element.showModal();
    return () => element.close();
  }, []);
  return <dialog ref={dialog} aria-labelledby={id} onCancel={event => { event.preventDefault(); close(); }}>
    <div className="modal-header"><h2 id={id}>{title}</h2><button type="button" className="icon-button" onClick={close} aria-label="Đóng"><X size={21} /></button></div>
    <div className="modal-content">{children}</div>
  </dialog>;
}

export function ErrorMessage({ error }) {
  return error && <div className="notice error" role="alert">{error}</div>;
}

export function Badge({ status }) {
  const tone = ['ACTIVE', 'CONFIRMED', 'PAID', 'COMPLETED'].includes(status) ? 'green'
    : ['UNPAID', 'DELAYED', 'MAINTENANCE'].includes(status) ? 'amber'
    : ['CANCELLED', 'RETIRED', 'INACTIVE'].includes(status) ? 'gray' : 'blue';
  return <span className={'badge ' + tone}>{labels[status] || status}</span>;
}

export function Table({ columns, rows, empty = 'Không tìm thấy dữ liệu.' }) {
  if (!rows.length) return <div className="empty">{empty}</div>;
  return <div className="table-scroll"><table>
    <thead><tr>{columns.map(([label]) => <th scope="col" key={label}>{label}</th>)}</tr></thead>
    <tbody>{rows.map(row => <tr key={row.id}>{columns.map(([label, render]) => <td key={label}>{render(row)}</td>)}</tr>)}</tbody>
  </table></div>;
}
