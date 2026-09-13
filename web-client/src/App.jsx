import { useEffect, useMemo, useState } from 'react';
import { Link, Navigate, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom';
import { ArrowRight, CalendarDays, Check, ChevronLeft, CreditCard, Download, Heart, LayoutDashboard, LogOut, MapPin, Menu, Moon, Plus, Search, Settings, ShieldCheck, Sun, Ticket, UserRound, Users, X } from 'lucide-react';
import { bookingRows } from './data/events';
import { api } from './lib/api';
import { CategorySeatMap, detectLayoutType, getSeatPrice, getSeatCategoryLabel } from './components/CategorySeatMap';

const nav = [{ to: '/', label: 'Discover' }, { to: '/events', label: 'Events' }, { to: '/about', label: 'About' }];
const cx = (...v) => v.filter(Boolean).join(' ');
const toDisplayEvent = event => ({
  id: event.eventUuid,
  title: event.title,
  description: event.description,
  category: event.category || 'Event',
  venue: event.venueName || 'Venue to be announced',
  city: event.city || '',
  date: event.eventDate ? new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(`${event.eventDate}T00:00:00`)) : 'Date to be announced',
  time: event.startTime || 'Time to be announced',
  price: Number(event.ticketPrice || 0),
  seats: event.availableSeats ?? event.totalSeats ?? 0,
  image: 'https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=1200&q=80',
});
const toDisplayUser = profile => ({
  userUuid: profile.userUuid,
  name: `${profile.firstName || ''} ${profile.lastName || ''}`.trim() || profile.email,
  email: profile.email,
  phoneNumber: profile.phoneNumber || '',
  role: profile.role || 'USER',
});
const loadRazorpay = () => new Promise(resolve => { if (window.Razorpay) return resolve(true); const script = document.createElement('script'); script.src = 'https://checkout.razorpay.com/v1/checkout.js'; script.onload = () => resolve(true); script.onerror = () => resolve(false); document.body.appendChild(script); });
const ticketStorageKey = 'latestConfirmedTicket';
const formatMoney = amount => `Rs. ${Number(amount || 0).toLocaleString('en-IN')}`;
const downloadTicketPdf = ({ booking, event }) => {
  const escapePdf = value => String(value || '').replace(/\\/g, '\\\\').replace(/[()]/g, '\\$&').replace(/[^\x20-\x7E]/g, '');
  const text = (value, x, y, size, font = 'F1', colour = '0.10 0.12 0.20') => `BT\n/${font} ${size} Tf\n${colour} rg\n${x} ${y} Td\n(${escapePdf(value)}) Tj\nET`;
  const seats = (booking.seats || []).join(', ');
  const stream = [
    '0.06 0.18 0.42 rg\n0 700 595 142 re f',
    '0.08 0.35 0.70 rg\n0 684 595 16 re f',
    text('EVENTHORIZON', 48, 792, 13, 'F2', '1 1 1'), text('CONFIRMED ADMISSION TICKET', 48, 762, 23, 'F2', '1 1 1'),
    text(`Booking ID  ${booking.bookingUUID}`, 48, 730, 10, 'F1', '0.87 0.94 1'),
    '0.96 0.98 1 rg\n48 638 499 36 re f', text('PAYMENT CONFIRMED', 66, 651, 11, 'F2', '0.05 0.42 0.25'),
    text(event.title, 48, 596, 20, 'F2'), text(`${event.date}  |  ${event.time}`, 48, 566, 12), text(`${event.venue}, ${event.city}`, 48, 543, 12),
    '0.91 0.95 1 rg\n48 454 499 64 re f', text('YOUR SEATS', 66, 494, 10, 'F2', '0.10 0.28 0.55'), text(seats || 'Not available', 66, 470, 18, 'F2', '0.05 0.18 0.42'),
    text('AMOUNT PAID', 48, 412, 10, 'F2', '0.36 0.40 0.49'), text(formatMoney(booking.totalAmount), 48, 382, 20, 'F2'),
    '0.82 0.85 0.90 RG\n48 335 m\n547 335 l\nS', text('Ticket status', 48, 306, 10, 'F2', '0.36 0.40 0.49'), text(booking.status || 'CONFIRMED', 48, 284, 12, 'F2', '0.05 0.42 0.25'),
    text('Show this ticket at the venue entrance. Keep your booking ID handy for support.', 48, 105, 10, 'F1', '0.36 0.40 0.49'), text('EventHorizon - Enjoy the moment.', 48, 76, 10, 'F2', '0.10 0.18 0.42')
  ].join('\n');
  const objects = ['<< /Type /Catalog /Pages 2 0 R >>', '<< /Type /Pages /Kids [3 0 R] /Count 1 >>', '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> /Contents 6 0 R >>', '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>', '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>', `<< /Length ${stream.length} >>\nstream\n${stream}\nendstream`];
  let pdf = '%PDF-1.4\n'; const offsets = [0];
  objects.forEach((object, index) => { offsets.push(pdf.length); pdf += `${index + 1} 0 obj\n${object}\nendobj\n`; });
  const xref = pdf.length; pdf += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n${offsets.slice(1).map(offset => `${String(offset).padStart(10, '0')} 00000 n `).join('\n')}\ntrailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF`;
  const link = document.createElement('a'); link.href = URL.createObjectURL(new Blob([pdf], { type: 'application/pdf' })); link.download = `EventHorizon-${booking.bookingUUID}.pdf`; link.click(); URL.revokeObjectURL(link.href);
};

function App() {
  const navigate = useNavigate();
  const [theme, setTheme] = useState(localStorage.getItem('theme') || 'light');
  const [user, setUser] = useState(() => JSON.parse(localStorage.getItem('user') || 'null'));
  const [events, setEvents] = useState([]);
  const [eventsError, setEventsError] = useState('');
  useEffect(() => { document.documentElement.classList.toggle('dark', theme === 'dark'); localStorage.setItem('theme', theme); }, [theme]);
  useEffect(() => {
    api.events()
      .then(response => setEvents((response.data || response).map(toDisplayEvent)))
      .catch(error => setEventsError(error.message || 'Unable to load events.'));
  }, []);
  useEffect(() => {
    if (!user?.userUuid) return;
    api.user(user.userUuid)
      .then(profile => {
        const refreshedUser = toDisplayUser(profile.data || profile);
        localStorage.setItem('user', JSON.stringify(refreshedUser));
        setUser(refreshedUser);
      })
      .catch(() => {});
  }, [user?.userUuid]);
  const signOut = () => {
    localStorage.clear();
    sessionStorage.clear();
    setUser(null);
    navigate('/');
  };
  return <Routes>
    <Route path="/login" element={user ? <Navigate to="/" /> : <AuthPage onAuth={setUser} />} />
    <Route path="/register" element={user ? <Navigate to="/" /> : <AuthPage register onAuth={setUser} />} />
    <Route path="/*" element={<Shell user={user} signOut={signOut} theme={theme} setTheme={setTheme}><Routes>
      <Route path="/" element={<PublicHome events={events} />} /><Route path="/events" element={<Events events={events} error={eventsError} />} /><Route path="/events/:id" element={<EventDetailsPublic user={user} events={events} />} />
      <Route path="/book/:id" element={<LiveCheckoutFlow user={user} events={events} />} /><Route path="/confirmation/:id" element={<Confirmation events={events} />} />
      <Route path="/bookings" element={<BookingsConnected user={user} events={events} />} /><Route path="/account" element={<Account user={user} setUser={setUser} signOut={signOut} />} />
      <Route path="/organizer" element={user?.role === 'ORGANIZER' ? <OrganizerConsole user={user} /> : <Navigate to="/" replace />} /><Route path="/admin" element={user?.role === 'ADMIN' ? <Admin /> : <Navigate to="/" replace />} /><Route path="/about" element={<About />} />
    </Routes></Shell>} />
  </Routes>;
}

function Shell({ children, user, signOut, theme, setTheme }) {
  const [open, setOpen] = useState(false); const role = user?.role || 'USER';
  const navigation = role === 'ADMIN' ? [...nav, { to: '/admin', label: 'Admin' }] : role === 'ORGANIZER' ? [...nav, { to: '/organizer', label: 'Organizer' }] : nav;
  return <div className="min-h-screen"><header className="sticky top-0 z-40 border-b border-slate-200/80 bg-slate-50/85 backdrop-blur dark:border-white/10 dark:bg-[#101118]/85"><div className="mx-auto flex h-18 max-w-7xl items-center justify-between px-4 py-3 sm:px-6"><Link to="/" className="flex items-center gap-2 font-bold tracking-tight"><span className="grid h-9 w-9 place-items-center rounded-xl bg-brand-600 text-white"><Ticket size={19} /></span><span>Event<span className="text-brand-600">Horizon</span></span></Link><nav className="hidden items-center gap-6 md:flex">{navigation.map(i => <Link key={i.to} to={i.to} className="text-sm font-medium text-slate-600 hover:text-brand-600 dark:text-slate-300">{i.label}</Link>)}</nav><div className="flex items-center gap-2"><button onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')} className="btn-secondary !p-2.5" aria-label="Toggle theme">{theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}</button>{user ? <div className="relative group"><button className="flex items-center gap-2 rounded-xl p-1.5 pl-2 text-sm font-semibold hover:bg-slate-100 dark:hover:bg-white/10"><span className="hidden sm:inline">{user.name}</span><span className="grid h-8 w-8 place-items-center rounded-lg bg-brand-100 text-brand-700 dark:bg-brand-500/20 dark:text-brand-50">{user.name?.[0]}</span></button><div className="invisible absolute right-0 top-full mt-2 w-48 rounded-2xl border border-slate-100 bg-white p-2 opacity-0 shadow-xl transition group-hover:visible group-hover:opacity-100 dark:border-white/10 dark:bg-[#20222e]"><Link to="/account" className="menu"><UserRound size={16}/> My account</Link>{role === 'ORGANIZER' && <Link to="/organizer" className="menu"><LayoutDashboard size={16}/> Organizer hub</Link>}{role === 'ADMIN' && <Link to="/admin" className="menu"><ShieldCheck size={16}/> Admin console</Link>}<button onClick={signOut} className="menu w-full text-left text-rose-600"><LogOut size={16}/> Log out</button></div></div> : <Link to="/login" className="btn-primary">Sign in</Link>}<button className="p-2 md:hidden" onClick={() => setOpen(!open)}>{open ? <X/> : <Menu/>}</button></div></div>{open && <nav className="border-t px-6 py-4 md:hidden">{navigation.map(i => <Link onClick={() => setOpen(false)} key={i.to} to={i.to} className="block py-2 text-sm font-semibold">{i.label}</Link>)}</nav>}</header><main>{children}</main><footer className="mt-20 border-t border-slate-200 bg-white dark:border-white/10 dark:bg-[#151620]"><div className="mx-auto flex max-w-7xl flex-col gap-3 px-6 py-9 text-sm text-slate-500 sm:flex-row sm:justify-between"><span>© 2026 EventHorizon. Made for memorable moments.</span><div className="flex gap-5"><Link to="/about">About us</Link><Link to="/account">Account settings</Link></div></div></footer></div>;
}

function Home({ events }) { const featured = events[0]; return <><section className="overflow-hidden bg-[#151529] text-white"><div className="mx-auto grid max-w-7xl gap-10 px-6 py-16 lg:grid-cols-[1.1fr_.9fr] lg:py-24"><div><span className="tag !bg-white/10 !text-white">Your next great night is here</span><h1 className="mt-5 text-5xl font-bold leading-[1.05] tracking-tight sm:text-6xl">Find moments worth <span className="text-violet-300">showing up for.</span></h1><p className="mt-6 max-w-xl text-lg text-slate-300">Book concerts, conferences, sports and more—beautifully simple, securely yours.</p><div className="mt-8 flex flex-wrap gap-3"><Link to="/events" className="btn-primary !bg-white !text-brand-700">Explore events <ArrowRight size={17}/></Link><Link to="/about" className="btn-secondary !bg-white/10 !text-white">How it works</Link></div></div>{featured && <div className="relative min-h-64 overflow-hidden rounded-3xl bg-gradient-to-br from-brand-500 via-violet-600 to-fuchsia-700 p-7 shadow-glow"><div className="absolute -right-10 -top-16 h-56 w-56 rounded-full bg-white/15"/><p className="relative text-sm font-semibold text-white/70">FEATURED EVENT</p><h2 className="relative mt-8 text-3xl font-bold">{featured.title}</h2><p className="relative mt-3 flex items-center gap-2"><CalendarDays size={17}/> {featured.date} · {featured.venue}</p><Link to={`/events/${featured.id}`} className="absolute bottom-7 left-7 btn-secondary !bg-white !text-brand-700">View event</Link></div>}</div></section><section className="mx-auto max-w-7xl px-6 py-16"><SectionTitle eyebrow="HAPPENING NOW" title="Pick your next experience" action="See all events"/><EventGrid list={events.slice(0, 3)}/></section><section className="mx-auto grid max-w-7xl gap-4 px-6 md:grid-cols-3"><Stat value="20k+" label="tickets booked this month"/><Stat value="4.9/5" label="average fan rating"/><Stat value="100%" label="secure payments"/></section></> }
function PublicHome({ events }) {
  const featured = events[0];
  return <><section className="overflow-hidden bg-[#151529] text-white"><div className="mx-auto grid max-w-7xl gap-10 px-6 py-16 lg:grid-cols-[1.1fr_.9fr] lg:py-24"><div><span className="tag !bg-white/10 !text-white">YOUR NEXT GREAT NIGHT IS HERE</span><h1 className="mt-5 text-5xl font-bold leading-[1.05] sm:text-6xl">Find moments worth <span className="text-violet-300">showing up for.</span></h1><p className="mt-6 max-w-xl text-lg text-slate-300">Discover concerts, conferences, sport, and more—then reserve your place in seconds.</p><Link to="/events" className="btn-primary mt-8 !bg-white !text-brand-700">Explore events <ArrowRight size={17}/></Link></div>{featured && <div className="rounded-3xl bg-gradient-to-br from-brand-500 via-violet-600 to-fuchsia-700 p-8 shadow-glow"><p className="text-sm font-semibold text-white/70">FEATURED EVENT</p><h2 className="mt-8 text-3xl font-bold">{featured.title}</h2><p className="mt-3 flex items-center gap-2"><CalendarDays size={17}/>{featured.date} · {featured.venue}</p><Link to={`/events/${featured.id}`} className="btn-secondary mt-8 !bg-white !text-brand-700">View event</Link></div>}</div></section><section className="mx-auto max-w-7xl px-6 py-16"><SectionTitle eyebrow="HAPPENING NOW" title="Pick your next experience" action="See all events"/><EventGrid list={events.slice(0, 3)}/></section><CustomerReviews/></>;
}
function CustomerReviews() { const reviews = [['Aarav Mehta','The booking process was fast and clear. My tickets were in my inbox straight away.'],['Nisha Kapoor','I found a wonderful concert I would have otherwise missed.'],['Rahul Verma','A clean, reliable way to plan a great evening with friends.']]; return <section className="bg-slate-100 py-16 dark:bg-white/5"><div className="mx-auto max-w-7xl px-6"><SectionTitle eyebrow="CUSTOMER REVIEWS" title="Loved by event-goers"/><div className="grid gap-5 md:grid-cols-3">{reviews.map(([name, review]) => <blockquote key={name} className="panel p-6"><div className="text-brand-600">★★★★★</div><p className="mt-4 leading-7 text-slate-600 dark:text-slate-300">“{review}”</p><footer className="mt-5 font-bold">{name}</footer></blockquote>)}</div></div></section>; }
function SectionTitle({ eyebrow, title, action }) { return <div className="mb-7 flex items-end justify-between"><div><p className="text-xs font-bold tracking-[.18em] text-brand-600">{eyebrow}</p><h2 className="mt-2 text-3xl font-bold tracking-tight">{title}</h2></div>{action && <Link to="/events" className="hidden text-sm font-semibold text-brand-600 sm:block">{action} <ArrowRight className="inline" size={15}/></Link>}</div> }
function Stat({ value, label }) { return <div className="panel p-6"><p className="text-3xl font-bold text-brand-600">{value}</p><p className="mt-1 text-sm text-slate-500">{label}</p></div> }
function EventGrid({ list }) { return <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">{list.map(e => <EventCard key={e.id} event={e}/>)}</div> }
function EventCard({ event }) { return <Link to={`/events/${event.id}`} className="group panel overflow-hidden transition hover:-translate-y-1 hover:shadow-xl"><div className="relative h-48 overflow-hidden"><img src={event.image} alt="" className="h-full w-full object-cover transition duration-500 group-hover:scale-105"/><span className="absolute left-3 top-3 tag">{event.category}</span><button className="absolute right-3 top-3 grid h-9 w-9 place-items-center rounded-xl bg-white/90 text-slate-700"><Heart size={17}/></button></div><div className="p-5"><p className="text-xs font-semibold text-brand-600">{event.date} · {event.time}</p><h3 className="mt-2 text-xl font-bold">{event.title}</h3><p className="mt-2 flex items-center gap-1 text-sm text-slate-500"><MapPin size={15}/>{event.venue}, {event.city}</p><div className="mt-5 flex items-center justify-between"><span className="font-bold">₹{event.price.toLocaleString('en-IN')}</span><span className="text-sm font-semibold text-brand-600">Details →</span></div></div></Link> }
function Events({ events, error }) { const [query, setQuery] = useState(''); const filtered = useMemo(() => events.filter(e => `${e.title} ${e.category} ${e.city}`.toLowerCase().includes(query.toLowerCase())), [events, query]); return <div className="mx-auto max-w-7xl px-6 py-12"><p className="text-xs font-bold tracking-[.18em] text-brand-600">DISCOVER</p><h1 className="mt-2 text-4xl font-bold">Events made for you</h1><div className="panel mt-8 flex items-center gap-3 p-3"><Search className="ml-2 text-slate-400" size={19}/><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Search artists, events or cities" className="w-full bg-transparent py-2 text-sm"/><button className="btn-primary">Search</button></div><div className="mt-7 flex gap-2 overflow-auto">{['All', 'Music', 'Conference', 'Theatre', 'Sports'].map(x => <button onClick={() => setQuery(x === 'All' ? '' : x)} className="btn-secondary whitespace-nowrap" key={x}>{x}</button>)}</div><div className="mt-8">{error ? <p className="text-rose-600">{error}</p> : filtered.length ? <EventGrid list={filtered}/> : <p className="text-slate-500">No events found.</p>}</div></div> }
function EventDetails({ events }) { const { id } = useParams(); const event = events.find(x => x.id === id) || events[0]; if (!event) return <Navigate to="/events" replace />; return <div className="mx-auto max-w-6xl px-6 py-10"><Link to="/events" className="mb-5 inline-flex items-center gap-1 text-sm font-semibold text-slate-500"><ChevronLeft size={17}/> All events</Link><div className="overflow-hidden rounded-3xl bg-[#141526] text-white"><div className="grid lg:grid-cols-2"><img className="h-full min-h-72 w-full object-cover" src={event.image} alt=""/><div className="p-8 lg:p-12"><span className="tag">{event.category}</span><h1 className="mt-5 text-4xl font-bold">{event.title}</h1><div className="mt-7 space-y-4 text-slate-300"><p className="flex gap-3"><CalendarDays/> {event.date} · {event.time}</p><p className="flex gap-3"><MapPin/> {event.venue}, {event.city}</p></div><p className="mt-8 text-2xl font-bold">₹{event.price.toLocaleString('en-IN')}</p><Link to={`/book/${event.id}`} className="btn-primary mt-7 w-full">Choose seats <ArrowRight size={17}/></Link></div></div></div><section className="mt-12 grid gap-8 lg:grid-cols-[1.4fr_.6fr]"><div><h2 className="text-2xl font-bold">About this event</h2><p className="mt-4 leading-7 text-slate-600 dark:text-slate-300">{event.description || 'Event details will be shared by the organizer.'}</p></div><div className="panel p-6"><p className="font-bold">Good to know</p><ul className="mt-4 space-y-3 text-sm text-slate-500"><li>• Doors open 60 minutes early</li><li>• Digital tickets only</li><li>• {event.seats} seats remaining</li></ul></div></section></div> }

function EventDetailsPublic({ user, events }) {
  const { id } = useParams();
  const [event, setEvent] = useState(() => events.find((item) => item.id === id) || null);
  const [loading, setLoading] = useState(!event);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    const found = events.find((item) => item.id === id);
    if (found) {
      setEvent(found);
      setLoading(false);
      return;
    }
    if (id) {
      setLoading(true);
      api.event(id)
        .then((response) => {
          const ev = toDisplayEvent(response.data || response);
          setEvent(ev);
          setLoading(false);
        })
        .catch(() => {
          setLoading(false);
          setNotFound(true);
        });
    }
  }, [id, events]);

  if (loading) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-24 text-center">
        <div className="inline-block h-8 w-8 animate-spin rounded-full border-4 border-brand-600 border-r-transparent"></div>
        <p className="mt-4 text-sm font-bold text-slate-500">Loading event details…</p>
      </div>
    );
  }

  if (notFound && !event) return <Navigate to="/events" replace />;
  if (!event) return null;

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <Link to="/events" className="text-sm font-semibold text-brand-600">← All events</Link>
      <div className="mt-5 overflow-hidden rounded-3xl bg-[#141526] text-white">
        <div className="grid lg:grid-cols-2">
          <img className="min-h-72 h-full w-full object-cover" src={event.image} alt="" />
          <div className="p-8 lg:p-12">
            <span className="tag">{event.category}</span>
            <h1 className="mt-5 text-4xl font-bold">{event.title}</h1>
            <p className="mt-6 flex items-center gap-2 text-slate-300"><CalendarDays size={18} />{event.date} · {event.time}</p>
            <p className="mt-3 flex items-center gap-2 text-slate-300"><MapPin size={18} />{event.venue}, {event.city}</p>
            <p className="mt-7 text-2xl font-bold">₹{event.price.toLocaleString('en-IN')}</p>
            <Link to={user ? `/book/${event.id}` : '/register'} className="btn-primary mt-7 w-full">{user ? 'Book ticket' : 'Register or sign in to book'} <ArrowRight size={17} /></Link>
          </div>
        </div>
      </div>
      <section className="mt-10">
        <h2 className="text-2xl font-bold">About this event</h2>
        <p className="mt-3 max-w-3xl leading-7 text-slate-600 dark:text-slate-300">{event.description || 'Event details will be shared by the organizer.'}</p>
      </section>
    </div>
  );
}

function LiveCheckoutFlow({ user, events }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const [event, setEvent] = useState(() => events.find((item) => item.id === id) || null);
  const [loading, setLoading] = useState(!event);
  const [notFound, setNotFound] = useState(false);

  const [step, setStep] = useState(1);
  const [quantity, setQuantity] = useState(1);
  const [seats, setSeats] = useState([]);
  const [availability, setAvailability] = useState({ bookedSeats: [], blockedSeats: [] });
  const [error, setError] = useState('');
  const [paying, setPaying] = useState(false);
  const [activeBookingUuid, setActiveBookingUuid] = useState(null);

  useEffect(() => {
    const found = events.find((item) => item.id === id);
    if (found) {
      setEvent(found);
      setLoading(false);
      return;
    }
    if (id) {
      setLoading(true);
      api.event(id)
        .then((response) => {
          const ev = toDisplayEvent(response.data || response);
          setEvent(ev);
          setLoading(false);
        })
        .catch(() => {
          setLoading(false);
          setNotFound(true);
        });
    }
  }, [id, events]);

  const loadAvailability = () => {
    if (id) {
      api.bookedSeats(id)
        .then((response) => {
          const avail = Array.isArray(response) ? { bookedSeats: response, blockedSeats: [] } : response;
          setAvailability(avail);
        })
        .catch(() => setError('Unable to load current seat availability.'));
    }
  };

  useEffect(() => {
    loadAvailability();
  }, [id]);

  if (loading) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-24 text-center">
        <div className="inline-block h-8 w-8 animate-spin rounded-full border-4 border-brand-600 border-r-transparent"></div>
        <p className="mt-4 text-sm font-bold text-slate-500">Loading seat map and event details…</p>
      </div>
    );
  }

  if (notFound && !event) return <Navigate to="/events" replace />;
  if (!event) return null;

  const handleCancelOrBack = async (msg = '') => {
    if (activeBookingUuid) {
      try {
        await api.releaseBooking(activeBookingUuid);
      } catch (e) {}
      setActiveBookingUuid(null);
    } else if (seats.length > 0) {
      try {
        await api.releaseSeats(event.id, seats);
      } catch (e) {}
    }
    setSeats([]);
    setStep(1);
    if (msg) setError(msg);
    loadAvailability();
  };

  const layoutType = detectLayoutType(event.category, event.title);
  const unitPrice = Number(event.price || 0);
  const total = seats.length * unitPrice;

  const count = (value) => {
    const next = Math.max(1, Number(value) || 1);
    setQuantity(next);
    setSeats((current) => current.slice(0, next));
  };

  const toggleSeat = (seatId) => {
    if (availability.bookedSeats.includes(seatId) || availability.blockedSeats.includes(seatId)) return;
    setSeats((current) =>
      current.includes(seatId)
        ? current.filter((item) => item !== seatId)
        : current.length < quantity
        ? [...current, seatId]
        : current
    );
  };

  const pay = async () => {
    if (!user?.userUuid) return navigate('/register');
    if (seats.length !== quantity) return setError(`Select ${quantity} seat${quantity === 1 ? '' : 's'} before payment.`);
    setPaying(true);
    setError('');
    try {
      const booking = await api.createBooking({
        eventUuid: event.id,
        userId: user.userUuid,
        seats,
        totalAmount: total,
        ticketPrice: event.price,
      });
      setActiveBookingUuid(booking.bookingUUID);

      const order = await api.createPaymentOrder(booking.bookingUUID);
      if (!(await loadRazorpay())) throw new Error('Razorpay could not be loaded. Check your internet connection.');
      const rzp = new window.Razorpay({
        key: order.key,
        amount: order.amount,
        currency: order.currency,
        name: 'EventHorizon',
        description: event.title,
        order_id: order.orderId,
        // The hold is enforced by the server; this closes Checkout at the same boundary.
        timeout: 720,
        handler: async (result) => {
          try {
            await api.verifyPayment({
              razorpayOrderId: result.razorpay_order_id,
              razorpayPaymentId: result.razorpay_payment_id,
              razorpaySignature: result.razorpay_signature,
            });
            const confirmedTicket = {
              ...booking,
              eventUuid: event.id,
              seats: [...seats],
              totalAmount: total,
              status: 'CONFIRMED',
            };
            sessionStorage.setItem(ticketStorageKey, JSON.stringify(confirmedTicket));
            navigate(`/confirmation/${event.id}`, { state: { booking: confirmedTicket } });
          } catch (err) {
            setError(err.message || 'Payment verification failed.');
            handleCancelOrBack('Payment verification failed. Your seat reservation has been released.');
          }
        },
        modal: {
          ondismiss: async () => {
            try {
              await api.failPayment({
                bookingUUID: booking.bookingUUID,
                razorpayOrderId: order.orderId,
                reason: 'Payment modal closed by user',
              });
            } catch (e) {}
            handleCancelOrBack('Payment was cancelled. Your seat reservation has been released.');
          },
        },
        prefill: { name: user.name, email: user.email },
      });

      rzp.on('payment.failed', async function (response) {
        const errorReason = response?.error?.description || response?.error?.reason || 'Payment failed';
        try {
          await api.failPayment({
            bookingUUID: booking.bookingUUID,
            razorpayOrderId: order.orderId,
            reason: errorReason,
          });
        } catch (e) {}
        handleCancelOrBack(`Payment failed: ${errorReason}. Your seats have been released.`);
      });

      rzp.open();
    } catch (err) {
      setError(err.message || 'Payment could not be started.');
      if (activeBookingUuid) {
        try { await api.releaseBooking(activeBookingUuid); } catch (e) {}
      }
    } finally { setPaying(false); }
  };

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      {/* Checkout Step Indicator */}
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-2 text-xs font-bold sm:text-sm">
          {[
            { num: 1, title: '1. Ticket Quantity' },
            { num: 2, title: '2. Choose Seats' },
            { num: 3, title: '3. Payment' },
          ].map((s) => (
            <span
              key={s.num}
              className={`rounded-full px-3.5 py-1.5 transition-all ${
                step === s.num
                  ? 'bg-brand-600 text-white shadow-md'
                  : step > s.num
                  ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300'
                  : 'bg-slate-200 text-slate-600 dark:bg-white/10 dark:text-slate-400'
              }`}
            >
              {s.title}
            </span>
          ))}
        </div>

      </div>

      <div className="grid gap-7 lg:grid-cols-[1fr_340px]">
        <section className="space-y-6">
          {step === 1 && (
            <div className="panel p-6 sm:p-8">
              <span className="tag">{event.category}</span>
              <h1 className="mt-3 text-3xl font-black">{event.title}</h1>
              <p className="mt-2 text-slate-500">How many tickets would you like to reserve? (₹{unitPrice.toLocaleString('en-IN')} per seat)</p>
              <div className="mt-7 flex items-center gap-4">
                <button
                  type="button"
                  onClick={() => count(quantity - 1)}
                  disabled={quantity === 1}
                  className="quantity-control"
                >
                  −
                </button>
                <input
                  type="number"
                  min="1"
                  max="10"
                  value={quantity}
                  onChange={(e) => count(e.target.value)}
                  className="field !mt-0 w-24 text-center text-xl font-extrabold"
                />
                <button
                  type="button"
                  onClick={() => count(quantity + 1)}
                  className="grid h-10 w-10 place-items-center rounded-lg bg-brand-600 text-xl font-bold text-white hover:bg-brand-700"
                >
                  +
                </button>
              </div>
              <button
                type="button"
                onClick={() => setStep(2)}
                className="btn-primary mt-8 gap-2"
              >
                Choose seats <ArrowRight size={17} />
              </button>
            </div>
          )}

          {step === 2 && (
            <div>
              <div className="mb-5 flex items-center justify-between">
                <div>
                  <h1 className="text-2xl font-black sm:text-3xl">Select {quantity} Seat{quantity === 1 ? '' : 's'}</h1>
                  <p className="mt-1 text-sm text-slate-500">
                    {seats.length} of {quantity} seat{quantity === 1 ? '' : 's'} selected for {event.title}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => handleCancelOrBack()}
                  className="text-xs font-semibold text-brand-600 hover:underline"
                >
                  ← Change quantity ({quantity})
                </button>
              </div>

              <CategorySeatMap
                event={event}
                selectedSeats={seats}
                onToggleSeat={toggleSeat}
                bookedSeats={availability.bookedSeats}
                blockedSeats={availability.blockedSeats}
                maxSeats={quantity}
              />
            </div>
          )}

          {step === 3 && (
            <div className="panel p-6 sm:p-8">
              <div className="flex items-center justify-between">
                <h1 className="text-3xl font-black">Secure Checkout</h1>
                <button
                  type="button"
                  onClick={() => setStep(2)}
                  className="text-xs font-semibold text-brand-600 hover:underline"
                >
                  ← Change seats
                </button>
              </div>
              <p className="mt-2 text-slate-500">
                You will be redirected to Razorpay to complete payment securely within 12 minutes.
              </p>
              <div className="mt-6 rounded-2xl bg-slate-50 p-4 border border-slate-200 dark:border-white/10 dark:bg-white/5">
                <div className="flex items-center gap-3">
                  <ShieldCheck size={24} className="text-emerald-600" />
                  <div>
                    <p className="text-sm font-bold">256-Bit SSL Encrypted Payment</p>
                    <p className="text-xs text-slate-500">Instant digital ticket delivery with booking ID</p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {error && <p className="text-sm font-bold text-rose-600">{error}</p>}
        </section>

        {/* Sidebar Summary */}
        <aside className="panel h-fit p-6 space-y-5">
          <div>
            <p className="text-xs font-bold tracking-[0.16em] text-brand-600">BOOKING SUMMARY</p>
            <h2 className="mt-2 text-xl font-black">{event.title}</h2>
            <p className="mt-1 text-xs text-slate-500">{event.venue}, {event.city}</p>
          </div>

          <div className="border-t border-slate-100 pt-4 dark:border-white/10 space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-slate-500">Ticket Price:</span>
              <span className="font-bold">₹{unitPrice.toLocaleString('en-IN')} / seat</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Ticket Quantity:</span>
              <span className="font-bold">{quantity}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Category Layout:</span>
              <span className="font-bold text-brand-600">{layoutType}</span>
            </div>
          </div>

          {/* Selected Seat Details */}
          <div className="border-t border-slate-100 pt-4 dark:border-white/10">
            <p className="text-xs font-bold uppercase text-slate-400">Selected Seats ({seats.length}/{quantity}):</p>
            {seats.length ? (
              <div className="mt-2.5 space-y-2 max-h-48 overflow-y-auto pr-1">
                {seats.map((seatId) => {
                  const sLabel = getSeatCategoryLabel(seatId, layoutType);
                  return (
                    <div key={seatId} className="flex items-center justify-between text-xs rounded-lg bg-slate-50 p-2 dark:bg-white/5">
                      <div>
                        <span className="font-black text-slate-900 dark:text-white">{seatId}</span>
                        <span className="ml-2 text-[11px] text-slate-500">({sLabel})</span>
                      </div>
                      <span className="font-extrabold text-brand-600">₹{unitPrice.toLocaleString('en-IN')}</span>
                    </div>
                  );
                })}
              </div>
            ) : (
              <p className="mt-2 text-xs italic text-slate-400">No seats selected yet.</p>
            )}
          </div>

          {/* Total Breakdown */}
          <div className="border-t border-slate-200 pt-4 dark:border-white/10">
            <div className="flex items-center justify-between text-lg font-black">
              <span>Total</span>
              <span className="text-brand-600">₹{total.toLocaleString('en-IN')}</span>
            </div>
          </div>

          {step === 2 && (
            <button
              type="button"
              onClick={() => setStep(3)}
              disabled={seats.length !== quantity}
              className="btn-primary w-full gap-2 disabled:opacity-40"
            >
              Continue to Payment <ArrowRight size={17} />
            </button>
          )}

          {step === 3 && (
            <div className="space-y-3">
              <button
                type="button"
                onClick={pay}
                disabled={paying}
                className="btn-primary w-full gap-2"
              >
                {paying ? 'Opening Razorpay...' : `Pay ₹${total.toLocaleString('en-IN')}`} <CreditCard size={17} />
              </button>
              <button
                type="button"
                onClick={() => handleCancelOrBack()}
                className="btn-secondary w-full text-xs"
              >
                Cancel & Release Seats
              </button>
            </div>
          )}
        </aside>
      </div>
    </div>
  );
}

function CheckoutBookingFlowFinal({ user, events }) {
  const { id } = useParams(); const navigate = useNavigate(); const event = events.find(item => item.id === id); const [step, setStep] = useState(1); const [quantity, setQuantity] = useState(1); const [seats, setSeats] = useState([]); const [error, setError] = useState(''); const [paying, setPaying] = useState(false);
  if (!event) return <Navigate to="/events" replace />;
  const hall = /comedy|movie|film|cinema|theatre/i.test(`${event.category} ${event.title}`); const rows = hall ? ['A','B','C','D','E','F','G','H'] : ['A','B','C','D','E','F']; const perRow = hall ? 10 : 12; const unavailable = ['A3','B7','C2','D9','E5','F10']; const all = rows.flatMap(row => Array.from({ length: perRow }, (_, i) => `${row}${i + 1}`)); const total = seats.length * event.price;
  const changeQuantity = value => { const next = Math.max(1, Number(value) || 1); setQuantity(next); setSeats(current => current.slice(0, next)); }; const choose = seat => !unavailable.includes(seat) && setSeats(current => current.includes(seat) ? current.filter(item => item !== seat) : current.length < quantity ? [...current, seat] : current);
  const pay = async () => { if (!user?.userUuid) return navigate('/register'); if (seats.length !== quantity) return setError(`Please select ${quantity} seat${quantity === 1 ? '' : 's'}.`); setPaying(true); setError(''); try { const booking = await api.createBooking({ eventUuid: event.id, userId: user.userUuid, seats }); const order = await api.createPaymentOrder(booking.bookingUUID); if (!await loadRazorpay()) throw new Error('Razorpay checkout could not be loaded.'); new window.Razorpay({ key: order.key, amount: order.amount, currency: order.currency, name: 'EventHorizon', description: event.title, order_id: order.orderId, handler: async payment => { try { await api.verifyPayment({ razorpayOrderId: payment.razorpay_order_id, razorpayPaymentId: payment.razorpay_payment_id, razorpaySignature: payment.razorpay_signature }); navigate(`/confirmation/${event.id}`); } catch (err) { setError(err.message || 'Payment verification failed.'); } }, prefill: { name: user.name, email: user.email } }).open(); } catch (err) { setError(err.message || 'Unable to start payment.'); } finally { setPaying(false); } };
  return <div className="mx-auto max-w-6xl px-6 py-10"><div className="flex gap-2 text-sm font-semibold">{['Tickets','Seats','Payment'].map((item, index) => <span key={item} className={cx('rounded-full px-3 py-1', step === index + 1 ? 'bg-brand-600 text-white' : 'bg-slate-200 text-slate-600')}>{index + 1}. {item}</span>)}</div><div className="mt-8 grid gap-7 lg:grid-cols-[1fr_320px]"><section className="panel p-6">{step === 1 && <><h1 className="text-3xl font-bold">How many tickets?</h1><p className="mt-2 text-slate-500">Choose a quantity, or type a custom number.</p><div className="mt-7 flex items-center gap-3"><button onClick={() => changeQuantity(quantity - 1)} disabled={quantity === 1} className="quantity-control">−</button><input min="1" type="number" value={quantity} onChange={e => changeQuantity(e.target.value)} className="field !mt-0 w-24 text-center text-lg font-bold"/><button onClick={() => changeQuantity(quantity + 1)} className="grid h-10 w-10 place-items-center rounded-lg bg-brand-600 text-xl font-bold text-white">+</button></div><button onClick={() => setStep(2)} className="btn-primary mt-7">Continue <ArrowRight size={17}/></button></>}{step === 2 && <><h1 className="text-3xl font-bold">Select {quantity} seat{quantity === 1 ? '' : 's'}</h1><p className="mt-2 text-slate-500">{seats.length} selected.</p><div className="mt-6">{hall ? <div className="rounded-lg bg-slate-200 py-3 text-center text-xs font-bold tracking-[.25em] text-slate-700">SCREEN / PERFORMANCE AREA</div> : <div className="rounded-t-[100%] bg-slate-800 py-3 text-center text-xs font-bold tracking-[.25em] text-white">STAGE</div>}{rows.map(row => <div className="mt-4" key={row}><p className="mb-1 text-xs font-bold text-slate-500">ROW {row} — ₹{event.price.toLocaleString('en-IN')}</p><div className={hall ? 'grid grid-cols-[repeat(5,1fr)_18px_repeat(5,1fr)] gap-2' : 'grid grid-cols-6 gap-2'}>{all.filter(seat => seat[0] === row).map((seat, index) => <><button type="button" key={seat} disabled={unavailable.includes(seat)} onClick={() => choose(seat)} className={cx('h-9 rounded-md text-xs font-bold', unavailable.includes(seat) ? 'bg-slate-300 text-slate-500' : seats.includes(seat) ? 'bg-brand-600 text-white' : 'bg-slate-200 text-slate-700')}>{seat.slice(1)}</button>{hall && index === 4 && <span key={`${seat}gap`}/>}</>)}</div></div>)}</div><button onClick={() => setStep(3)} disabled={seats.length !== quantity} className="btn-primary mt-7 disabled:opacity-40">Continue to payment <ArrowRight size={17}/></button></>}{step === 3 && <><h1 className="text-3xl font-bold">Payment</h1><p className="mt-2 text-slate-500">Review your booking summary and pay securely with Razorpay.</p></>}{error && <p className="mt-5 text-sm text-rose-600">{error}</p>}</section><aside className="panel h-fit p-6"><p className="text-xs font-bold tracking-[.16em] text-brand-600">BOOKING SUMMARY</p><h2 className="mt-4 font-bold">{event.title}</h2><p className="mt-2 text-sm text-slate-500">{quantity} ticket{quantity === 1 ? '' : 's'}</p><p className="mt-3 text-sm text-slate-500">{seats.join(', ') || 'Seats not selected yet'}</p><div className="mt-5 flex justify-between border-t pt-4 text-lg font-bold"><span>Total</span><span>Rs. {total.toLocaleString()}</span></div>{step === 3 && <button onClick={pay} disabled={paying} className="btn-primary mt-5 w-full">{paying ? 'Opening Razorpay...' : `Pay Rs. ${total.toLocaleString()}`} <CreditCard size={17}/></button>}</aside></div></div>;
}

function CheckoutBookingFlow({ user, events }) {
  const { id } = useParams(); const navigate = useNavigate(); const event = events.find(item => item.id === id); const [step, setStep] = useState(1); const [quantity, setQuantity] = useState(1); const [selected, setSelected] = useState([]); const [error, setError] = useState(''); const [paying, setPaying] = useState(false);
  if (!event) return <Navigate to="/events" replace />;
  const auditorium = /comedy|movie|film|cinema|theatre/i.test(`${event.category} ${event.title}`); const rows = auditorium ? ['A','B','C','D','E','F','G','H'] : ['A','B','C','D','E','F']; const perRow = auditorium ? 10 : 12; const taken = ['A3','B7','C2','D9','E5','F10']; const allSeats = rows.flatMap(row => Array.from({ length: perRow }, (_, i) => `${row}${i + 1}`)); const total = selected.length * event.price;
  const setCount = value => { const next = Math.max(1, Number(value) || 1); setQuantity(next); setSelected(current => current.slice(0, next)); }; const toggle = seat => { if (taken.includes(seat)) return; setSelected(current => current.includes(seat) ? current.filter(item => item !== seat) : current.length < quantity ? [...current, seat] : current); };
  const pay = async () => { if (!user?.userUuid) return navigate('/register'); if (selected.length !== quantity) return setError(`Select ${quantity} seat${quantity === 1 ? '' : 's'} to continue.`); setPaying(true); setError(''); try { const booking = await api.createBooking({ eventUuid: event.id, userId: user.userUuid, seats: selected }); const order = await api.createPaymentOrder(booking.bookingUUID); if (!await loadRazorpay()) throw new Error('Unable to load Razorpay checkout.'); new window.Razorpay({ key: order.key, amount: order.amount, currency: order.currency, name: 'EventHorizon', description: event.title, order_id: order.orderId, handler: async response => { try { await api.verifyPayment({ razorpayOrderId: response.razorpay_order_id, razorpayPaymentId: response.razorpay_payment_id, razorpaySignature: response.razorpay_signature }); navigate(`/confirmation/${event.id}`); } catch (err) { setError(err.message || 'Payment verification failed.'); } }, prefill: { name: user.name, email: user.email }, theme: { color: '#7c3aed' } }).open(); } catch (err) { setError(err.message || 'Unable to start payment.'); } finally { setPaying(false); } };
  return <div className="mx-auto max-w-6xl px-6 py-10"><div className="flex gap-3 text-sm font-semibold">{['Tickets','Seats','Payment'].map((label, index) => <span key={label} className={cx('rounded-full px-3 py-1', step === index + 1 ? 'bg-brand-600 text-white' : 'bg-slate-200 text-slate-500')}>{index + 1}. {label}</span>)}</div><div className="mt-8 grid gap-7 lg:grid-cols-[1fr_320px]"><section className="panel p-6 sm:p-8">{step === 1 && <><h1 className="text-3xl font-bold">How many tickets?</h1><p className="mt-2 text-slate-500">Set your ticket quantity before choosing specific seats.</p><div className="mt-8 flex items-center gap-4"><button onClick={() => setCount(quantity - 1)} disabled={quantity <= 1} className="grid h-11 w-11 place-items-center rounded-xl bg-slate-100 text-xl font-bold disabled:opacity-40">−</button><input type="number" min="1" value={quantity} onChange={e => setCount(e.target.value)} className="field !mt-0 w-28 text-center text-lg font-bold"/><button onClick={() => setCount(quantity + 1)} className="grid h-11 w-11 place-items-center rounded-xl bg-brand-600 text-xl font-bold text-white">+</button></div><button onClick={() => setStep(2)} className="btn-primary mt-8">Choose seats <ArrowRight size={17}/></button></>}{step === 2 && <><h1 className="text-3xl font-bold">Choose {quantity} seat{quantity === 1 ? '' : 's'}</h1><p className="mt-2 text-slate-500">{selected.length} of {quantity} selected.</p><div className="mt-7">{auditorium ? <div className="rounded-lg bg-slate-200 py-3 text-center text-xs font-bold tracking-[.3em]">SCREEN / PERFORMANCE AREA</div> : <div className="rounded-t-[100%] bg-slate-800 py-3 text-center text-xs font-bold tracking-[.3em] text-white">STAGE</div>}<div className="mt-6 space-y-3">{rows.map(row => <div key={row}><div className="mb-1 text-xs font-bold text-slate-500">ROW {row} — Rs. {event.price.toLocaleString('en-IN')}</div><div className={auditorium ? 'grid grid-cols-[repeat(5,1fr)_18px_repeat(5,1fr)] gap-2' : 'grid grid-cols-6 gap-2'}>{allSeats.filter(seat => seat[0] === row).map((seat, index) => <><button type="button" key={seat} disabled={taken.includes(seat)} onClick={() => toggle(seat)} className={cx('h-9 rounded-md text-xs font-bold', taken.includes(seat) ? 'bg-slate-200 text-slate-400' : selected.includes(seat) ? 'bg-brand-600 text-white' : 'bg-slate-200 text-slate-700')}>{seat.slice(1)}</button>{auditorium && index === 4 && <span key={`${seat}-aisle`}/>}</>)}</div></div>)}</div></div><button onClick={() => setStep(3)} disabled={selected.length !== quantity} className="btn-primary mt-8 disabled:opacity-40">Continue to payment <ArrowRight size={17}/></button></>}{step === 3 && <><h1 className="text-3xl font-bold">Pay securely with Razorpay</h1><p className="mt-3 text-slate-500">You will be taken to Razorpay to complete your payment securely.</p><button onClick={pay} disabled={paying} className="btn-primary mt-8">{paying ? 'Opening Razorpay...' : `Pay Rs. ${total.toLocaleString()}`} <CreditCard size={17}/></button></>}{error && <p className="mt-5 text-sm text-rose-600">{error}</p>}</section><aside className="panel h-fit p-6"><p className="text-xs font-bold tracking-[.16em] text-brand-600">ORDER SUMMARY</p><h2 className="mt-4 font-bold">{event.title}</h2><p className="mt-2 text-sm text-slate-500">{quantity} ticket{quantity === 1 ? '' : 's'}</p><p className="mt-3 text-sm text-slate-500">{selected.join(', ') || 'Seats not selected yet'}</p><div className="mt-5 flex justify-between border-t pt-4 text-lg font-bold"><span>Total</span><span>Rs. {total.toLocaleString()}</span></div></aside></div></div>;
}

function UnlimitedBookingFlow({ user, events }) {
  const { id } = useParams(); const navigate = useNavigate(); const event = events.find(item => item.id === id); const [selected, setSelected] = useState([]); const [error, setError] = useState(''); const [saving, setSaving] = useState(false);
  if (!event) return <Navigate to="/events" replace />;
  const auditorium = /comedy|movie|film|cinema|theatre/i.test(`${event.category} ${event.title}`); const rows = auditorium ? ['A','B','C','D','E','F','G','H'] : ['A','B','C','D','E','F']; const perRow = auditorium ? 10 : 12; const taken = ['A3','B7','C2','D9','E5','F10']; const allSeats = rows.flatMap(row => Array.from({ length: perRow }, (_, index) => `${row}${index + 1}`)); const total = selected.length * event.price;
  const toggle = seat => !taken.includes(seat) && setSelected(current => current.includes(seat) ? current.filter(item => item !== seat) : [...current, seat]); const add = () => { const seat = allSeats.find(item => !taken.includes(item) && !selected.includes(item)); if (seat) toggle(seat); }; const remove = () => setSelected(current => current.slice(0, -1));
  const submit = async e => { e.preventDefault(); if (!user?.userUuid) return navigate('/register'); if (!selected.length) return setError('Select at least one seat.'); setSaving(true); try { const booking = await api.createBooking({ eventUuid: event.id, userId: user.userUuid, seats: selected }); navigate(`/confirmation/${event.id}`, { state: { booking, seats: selected } }); } catch (err) { setError(err.message || 'Unable to create booking.'); } finally { setSaving(false); } };
  return <div className="mx-auto max-w-6xl px-6 py-10"><Link to={`/events/${event.id}`} className="text-sm font-semibold text-brand-600">Back to event</Link><div className="mt-5 grid gap-7 lg:grid-cols-[1fr_320px]"><form onSubmit={submit} className="panel p-6 sm:p-8"><p className="text-xs font-bold tracking-[.18em] text-brand-600">{auditorium ? 'AUDITORIUM SEATING' : 'CONCERT SEATING'}</p><h1 className="mt-2 text-3xl font-bold">Choose your seats</h1><div className="mx-auto mt-7 max-w-xl">{auditorium ? <div className="rounded-lg border-2 border-slate-300 bg-slate-100 py-3 text-center text-xs font-bold tracking-[.3em] text-slate-600">SCREEN / PERFORMANCE AREA</div> : <div className="rounded-t-[100%] bg-slate-800 py-3 text-center text-xs font-bold tracking-[.3em] text-white">STAGE</div>}<div className="mt-7 space-y-4">{rows.map(row => <div key={row}><div className="mb-2 flex justify-between text-xs font-bold text-slate-500"><span>ROW {row}</span><span>Rs. {event.price.toLocaleString('en-IN')}</span></div><div className={auditorium ? 'grid grid-cols-[repeat(5,1fr)_18px_repeat(5,1fr)] gap-2' : 'grid grid-cols-6 gap-2'}>{allSeats.filter(seat => seat[0] === row).map((seat, index) => <><button type="button" key={seat} disabled={taken.includes(seat)} onClick={() => toggle(seat)} className={cx('h-9 rounded-md text-xs font-bold', taken.includes(seat) ? 'bg-slate-200 text-slate-400' : selected.includes(seat) ? 'bg-brand-600 text-white' : 'bg-slate-200 text-slate-700')}>{seat.slice(1)}</button>{auditorium && index === 4 && <span key={`${seat}-aisle`}/>}</>)}</div></div>)}</div></div></form><aside className="panel h-fit p-6"><p className="text-xs font-bold tracking-[.16em] text-brand-600">BOOKING SUMMARY</p><h2 className="mt-4 text-xl font-bold">{event.title}</h2><div className="mt-5 flex items-center justify-between rounded-xl bg-slate-100 p-3"><span className="text-sm font-semibold">Selected seats</span><div className="flex items-center gap-3"><button type="button" onClick={remove} disabled={!selected.length} className="grid h-8 w-8 place-items-center rounded-lg bg-white text-lg font-bold disabled:opacity-40">−</button><b className="min-w-5 text-center">{selected.length}</b><button type="button" onClick={add} className="grid h-8 w-8 place-items-center rounded-lg bg-brand-600 text-lg font-bold text-white">+</button></div></div><p className="mt-4 text-sm text-slate-500">{selected.join(', ') || 'Choose seats from the map.'}</p><div className="mt-5 flex justify-between border-t pt-4 text-lg font-bold"><span>Total</span><span>Rs. {total.toLocaleString()}</span></div>{error && <p className="mt-4 text-sm text-rose-600">{error}</p>}<button disabled={!selected.length || saving} className="btn-primary mt-6 w-full disabled:opacity-50">{saving ? 'Creating booking...' : 'Book tickets'} <ArrowRight size={17}/></button></aside></div></div>;
}

function DynamicSeatBookingFlow({ user, events }) {
  const { id } = useParams(); const navigate = useNavigate(); const event = events.find(item => item.id === id); const [selected, setSelected] = useState([]); const [error, setError] = useState(''); const [saving, setSaving] = useState(false);
  if (!event) return <Navigate to="/events" replace />;
  const auditorium = /comedy|movie|film|cinema|theatre/i.test(`${event.category} ${event.title}`); const rows = auditorium ? ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'] : ['A', 'B', 'C', 'D', 'E', 'F']; const taken = auditorium ? ['A2', 'B8', 'C5', 'D6', 'F3', 'G9'] : ['A3', 'A4', 'B7', 'C2', 'D9', 'E5', 'F10']; const total = selected.length * event.price;
  const toggle = seat => { if (!taken.includes(seat)) setSelected(current => current.includes(seat) ? current.filter(item => item !== seat) : current.length < 6 ? [...current, seat] : current); };
  const button = seat => <button type="button" key={seat} disabled={taken.includes(seat)} onClick={() => toggle(seat)} className={cx('h-9 rounded-md text-xs font-bold transition', taken.includes(seat) ? 'cursor-not-allowed bg-slate-200 text-slate-400 dark:bg-white/10' : selected.includes(seat) ? 'bg-brand-600 text-white' : 'bg-slate-200 text-slate-700 hover:bg-slate-300 dark:bg-white/10 dark:text-white')}>{seat.slice(1)}</button>;
  const submit = async e => { e.preventDefault(); if (!user?.userUuid) return navigate('/register'); if (!selected.length) return setError('Select at least one seat.'); setSaving(true); setError(''); try { const booking = await api.createBooking({ eventUuid: event.id, userId: user.userUuid, seats: selected }); navigate(`/confirmation/${event.id}`, { state: { booking, seats: selected } }); } catch (err) { setError(err.message || 'Unable to create booking.'); } finally { setSaving(false); } };
  return <div className="mx-auto max-w-6xl px-6 py-10"><Link to={`/events/${event.id}`} className="text-sm font-semibold text-brand-600">Back to event</Link><div className="mt-5 grid gap-7 lg:grid-cols-[1fr_320px]"><form onSubmit={submit} className="panel p-6 sm:p-8"><p className="text-xs font-bold tracking-[.18em] text-brand-600">{auditorium ? 'AUDITORIUM SEATING' : 'CONCERT SEATING'}</p><h1 className="mt-2 text-3xl font-bold">Choose your seats</h1><p className="mt-2 text-sm text-slate-500">Select up to 6 seats for {event.title}.</p><div className="mx-auto mt-8 max-w-xl">{auditorium ? <div className="rounded-lg border-2 border-slate-300 bg-slate-100 py-3 text-center text-xs font-bold tracking-[.3em] text-slate-600 dark:border-white/20 dark:bg-white/10 dark:text-white">SCREEN / PERFORMANCE AREA</div> : <div className="rounded-t-[100%] bg-slate-800 py-3 text-center text-xs font-bold tracking-[.3em] text-white">STAGE</div>}<div className="mt-8 space-y-4">{rows.map(row => <div key={row}><div className="mb-2 flex items-center justify-between text-xs font-bold text-slate-500"><span>ROW {row}</span><span>Rs. {event.price.toLocaleString('en-IN')}</span></div>{auditorium ? <div className="grid grid-cols-[repeat(5,minmax(0,1fr))_20px_repeat(5,minmax(0,1fr))] gap-2">{Array.from({ length: 10 }, (_, index) => `${row}${index + 1}`).slice(0, 5).map(button)}<span/>{Array.from({ length: 10 }, (_, index) => `${row}${index + 1}`).slice(5).map(button)}</div> : <div className="grid grid-cols-6 gap-2">{Array.from({ length: 12 }, (_, index) => `${row}${index + 1}`).map(button)}</div>}</div>)}</div><div className="mt-6 flex flex-wrap gap-4 text-xs text-slate-500"><span><i className="mr-1 inline-block h-3 w-3 rounded bg-brand-600"/> Selected</span><span><i className="mr-1 inline-block h-3 w-3 rounded bg-slate-300"/> Taken</span></div></div></form><aside className="panel h-fit p-6"><p className="text-xs font-bold tracking-[.16em] text-brand-600">BOOKING SUMMARY</p><h2 className="mt-4 text-xl font-bold">{event.title}</h2><p className="mt-2 text-sm text-slate-500">{selected.length} of 6 seats selected</p><div className="my-5 border-t border-dashed border-slate-200"/>{selected.length ? <div className="space-y-2 text-sm">{selected.map(seat => <p key={seat} className="flex justify-between"><span>{seat}</span><b>Rs. {event.price.toLocaleString()}</b></p>)}</div> : <p className="text-sm text-slate-500">Choose seats from the map.</p>}<div className="mt-5 flex justify-between border-t pt-4 text-lg font-bold"><span>Total</span><span>Rs. {total.toLocaleString()}</span></div>{error && <p className="mt-4 text-sm text-rose-600">{error}</p>}<button disabled={!selected.length || saving} className="btn-primary mt-6 w-full disabled:opacity-50">{saving ? 'Creating booking...' : `Book ${selected.length || ''} ticket${selected.length === 1 ? '' : 's'}`} <ArrowRight size={17}/></button></aside></div></div>;
}

function SeatBookingFlow({ user, events }) {
  const { id } = useParams(); const navigate = useNavigate(); const event = events.find(item => item.id === id); const [selected, setSelected] = useState([]); const [error, setError] = useState(''); const [saving, setSaving] = useState(false);
  if (!event) return <Navigate to="/events" replace />;
  const rows = ['A', 'B', 'C', 'D', 'E', 'F']; const taken = ['A3', 'A4', 'B7', 'C2', 'D9', 'E5', 'F10'];
  const total = selected.length * event.price;
  const toggle = seat => { if (taken.includes(seat)) return; setSelected(current => current.includes(seat) ? current.filter(item => item !== seat) : current.length < 6 ? [...current, seat] : current); };
  const submit = async e => { e.preventDefault(); if (!user?.userUuid) return navigate('/register'); if (!selected.length) return setError('Select at least one seat.'); setSaving(true); setError(''); try { const booking = await api.createBooking({ eventUuid: event.id, userId: user.userUuid, seats: selected }); navigate(`/confirmation/${event.id}`, { state: { booking, seats: selected } }); } catch (err) { setError(err.message || 'Unable to create booking.'); } finally { setSaving(false); } };
  return <div className="mx-auto max-w-6xl px-6 py-10"><Link to={`/events/${event.id}`} className="text-sm font-semibold text-brand-600">Back to event</Link><div className="mt-5 grid gap-7 lg:grid-cols-[1fr_320px]"><form onSubmit={submit} className="panel p-6 sm:p-8"><h1 className="text-3xl font-bold">Choose your seats</h1><p className="mt-2 text-sm text-slate-500">Select up to 6 seats for {event.title}.</p><div className="mx-auto mt-8 max-w-xl"><div className="rounded-t-[100%] bg-slate-800 py-3 text-center text-xs font-bold tracking-[.3em] text-white">STAGE</div><div className="mt-8 space-y-4">{rows.map(row => <div key={row}><div className="mb-2 flex items-center justify-between text-xs font-bold text-slate-500"><span>ROW {row}</span><span>Rs. {event.price.toLocaleString('en-IN')}</span></div><div className="grid grid-cols-6 gap-2">{Array.from({ length: 12 }, (_, index) => `${row}${index + 1}`).map(seat => <button type="button" key={seat} disabled={taken.includes(seat)} onClick={() => toggle(seat)} className={cx('aspect-square rounded-lg text-xs font-bold transition', taken.includes(seat) ? 'cursor-not-allowed bg-slate-200 text-slate-400 dark:bg-white/10' : selected.includes(seat) ? 'bg-brand-600 text-white' : 'bg-slate-200 text-slate-700 hover:bg-slate-300 dark:bg-white/10 dark:text-white')}>{seat.slice(1)}</button>)}</div></div>)}</div><div className="mt-6 flex flex-wrap gap-4 text-xs text-slate-500"><span><i className="mr-1 inline-block h-3 w-3 rounded bg-brand-600"/> Selected</span><span><i className="mr-1 inline-block h-3 w-3 rounded bg-slate-300"/> Taken</span></div></div></form><aside className="panel h-fit p-6"><p className="text-xs font-bold tracking-[.16em] text-brand-600">BOOKING SUMMARY</p><h2 className="mt-4 text-xl font-bold">{event.title}</h2><p className="mt-2 text-sm text-slate-500">{selected.length} of 6 seats selected</p><div className="my-5 border-t border-dashed border-slate-200"/>{selected.length ? <div className="space-y-2 text-sm">{selected.map(seat => <p key={seat} className="flex justify-between"><span>{seat}</span><b>Rs. {event.price.toLocaleString()}</b></p>)}</div> : <p className="text-sm text-slate-500">Choose seats from the map.</p>}<div className="mt-5 flex justify-between border-t pt-4 text-lg font-bold"><span>Total</span><span>Rs. {total.toLocaleString()}</span></div>{error && <p className="mt-4 text-sm text-rose-600">{error}</p>}<button disabled={!selected.length || saving} className="btn-primary mt-6 w-full disabled:opacity-50">{saving ? 'Creating booking...' : `Book ${selected.length || ''} ticket${selected.length === 1 ? '' : 's'}`} <ArrowRight size={17}/></button></aside></div></div>;
}

function BookingFlowConnected({ user, events }) {
  const { id } = useParams(); const navigate = useNavigate(); const event = events.find(item => item.id === id); const [seats, setSeats] = useState(''); const [error, setError] = useState(''); const [saving, setSaving] = useState(false);
  if (!event) return <Navigate to="/events" replace />;
  const submit = async e => { e.preventDefault(); if (!user?.userUuid) return navigate('/login'); const selected = seats.split(',').map(value => value.trim().toUpperCase()).filter(Boolean); if (!selected.length) return setError('Enter at least one seat number.'); setSaving(true); setError(''); try { const booking = await api.createBooking({ eventUuid: event.id, userId: user.userUuid, seats: selected }); navigate(`/confirmation/${event.id}`, { state: { booking, seats: selected } }); } catch (err) { setError(err.message || 'Unable to create booking.'); } finally { setSaving(false); } };
  return <div className="mx-auto max-w-xl px-6 py-12"><Link to={`/events/${event.id}`} className="text-sm font-semibold text-brand-600">← Back to event</Link><div className="panel mt-5 p-7"><h1 className="text-3xl font-bold">Book {event.title}</h1><p className="mt-2 text-slate-500">₹{event.price.toLocaleString()} per ticket. Enter the seats you want to reserve.</p><form onSubmit={submit} className="mt-7"><label className="text-sm font-semibold">Seat numbers<input className="field" placeholder="A1, A4" value={seats} onChange={e => setSeats(e.target.value)}/></label>{error && <p className="mt-4 text-sm text-rose-600">{error}</p>}<button disabled={saving} className="btn-primary mt-6 w-full disabled:opacity-50">{saving ? 'Creating booking…' : 'Confirm booking'} <ArrowRight size={17}/></button></form></div></div>;
}

function BookingsConnected({ user, events }) {
  const [bookings, setBookings] = useState([]);
  const [error, setError] = useState('');
  useEffect(() => {
    if (!user?.userUuid) return;
    api.bookings(user.userUuid).then(response => {
      const latestTicket = JSON.parse(sessionStorage.getItem(ticketStorageKey) || 'null');
      setBookings(response.map(booking => latestTicket?.bookingUUID === booking.bookingUUID && (!booking.seats || !booking.seats.length) ? { ...booking, seats: latestTicket.seats, totalAmount: booking.totalAmount ?? latestTicket.totalAmount } : booking));
    }).catch(err => setError(err.message || 'Unable to load bookings.'));
  }, [user?.userUuid]);

  if (!user) return <Navigate to="/login" replace />;

  const getStatusBadge = (status) => {
    switch (status) {
      case 'CONFIRMED':
        return 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300 border border-emerald-300';
      case 'FAILED':
        return 'bg-rose-100 text-rose-800 dark:bg-rose-950/60 dark:text-rose-300 border border-rose-300';
      case 'REFUNDED':
        return 'bg-purple-100 text-purple-800 dark:bg-purple-950/60 dark:text-purple-300 border border-purple-300';
      case 'CANCELLED':
      case 'EXPIRED':
        return 'bg-slate-100 text-slate-600 dark:bg-white/10 dark:text-slate-400 border border-slate-300';
      default:
        return 'bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300 border border-amber-300';
    }
  };

  return (
    <div className="mx-auto max-w-6xl px-6 py-12">
      <SectionTitle eyebrow="MY TICKETS" title="Your bookings"/>
      {error && <p className="text-rose-600">{error}</p>}
      {!error && !bookings.length && <p className="text-slate-500">You have no bookings yet.</p>}
      <div className="grid gap-4">
        {bookings.map(booking => {
          const event = events.find(item => item.id === booking.eventUuid);
          const seats = booking.seats || booking.seatNumbers || booking.bookingSeats?.map(seat => seat.seatNumber) || [];
          return (
            <div className="panel p-5" key={booking.bookingUUID}>
              <span className={`inline-block rounded-full px-3 py-1 text-xs font-bold ${getStatusBadge(booking.status)}`}>
                {booking.status}
              </span>
              <h3 className="mt-3 text-xl font-bold">{event?.title || booking.eventUuid}</h3>
              <p className="mt-2 text-sm text-slate-500">Booking ID: {booking.bookingUUID}</p>
              <p className="mt-1 text-sm text-slate-500">Seats: {seats.length ? seats.join(', ') : 'Seats released'}</p>
              <p className="mt-1 text-sm text-slate-500">Amount: {formatMoney(booking.totalAmount)}</p>
              {booking.status === 'CONFIRMED' && event && (
                <button onClick={() => downloadTicketPdf({ booking: { ...booking, seats }, event })} className="btn-secondary mt-4">
                  <Download size={16}/> Download PDF
                </button>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

function BookingFlow({ user, events }) { const { id } = useParams(); const navigate = useNavigate(); const event = events.find(x => x.id === id) || events[0]; const [step, setStep] = useState(1); const [selected, setSelected] = useState([]); const [method, setMethod] = useState('razorpay'); if (!event) return <Navigate to="/events" replace />; const occupied = ['A2','A3','B8','B9','C5','C6','C7','D13']; const seats = ['A','B','C','D','E'].flatMap(r => Array.from({ length: 14 }, (_, i) => `${r}${i + 1}`)); const total = selected.length * event.price;
 const next = () => step < 3 ? setStep(step + 1) : navigate(`/confirmation/${event.id}`); return <div className="mx-auto max-w-6xl px-6 py-10"><div className="flex items-center gap-3 text-sm font-semibold">{['Seats','Payment','Review'].map((x,i) => <div key={x} className="flex items-center gap-3"><span className={cx('grid h-7 w-7 place-items-center rounded-full', step >= i+1 ? 'bg-brand-600 text-white' : 'bg-slate-200 text-slate-500')}>{i+1}</span>{x}{i < 2 && <span className="hidden h-px w-12 bg-slate-200 sm:block"/>}</div>)}</div><div className="mt-9 grid gap-8 lg:grid-cols-[1fr_330px]"><div className="panel p-6 sm:p-8">{step === 1 && <><h1 className="text-2xl font-bold">Choose your seats</h1><p className="mt-2 text-sm text-slate-500">Select up to 6 seats. Gold tickets are ₹{event.price.toLocaleString()} each.</p><div className="mx-auto mt-8 max-w-md"><div className="rounded-t-[100%] bg-slate-800 py-2 text-center text-xs font-bold tracking-[.3em] text-white">STAGE</div><div className="mt-8 space-y-3">{['A','B','C','D','E'].map(row => <div key={row} className="flex items-center gap-2"><span className="w-4 text-xs font-bold text-slate-400">{row}</span><div className="grid flex-1 grid-cols-7 gap-2">{seats.filter(s => s[0] === row).map(s => <button disabled={occupied.includes(s)} onClick={() => setSelected(old => old.includes(s) ? old.filter(x => x !== s) : old.length < 6 ? [...old, s] : old)} className={cx('aspect-square rounded-md text-[10px] font-bold transition', occupied.includes(s) ? 'cursor-not-allowed bg-slate-200 text-slate-400 dark:bg-white/10' : selected.includes(s) ? 'bg-brand-600 text-white' : 'bg-brand-100 text-brand-700 hover:bg-brand-200 dark:bg-brand-500/20 dark:text-brand-50')} key={s}>{s.slice(1)}</button>)}</div></div>)}</div><div className="mt-6 flex gap-4 text-xs text-slate-500"><span className="flex items-center gap-1"><i className="h-3 w-3 rounded bg-brand-100"/> Available</span><span className="flex items-center gap-1"><i className="h-3 w-3 rounded bg-brand-600"/> Selected</span><span className="flex items-center gap-1"><i className="h-3 w-3 rounded bg-slate-200"/> Taken</span></div></div></>}{step === 2 && <><h1 className="text-2xl font-bold">How would you like to pay?</h1><p className="mt-2 text-sm text-slate-500">All transactions are secure and encrypted.</p><div className="mt-7 space-y-3">{[['razorpay','Razorpay','UPI, cards, net banking & wallets'],['card','Credit or debit card','Visa, Mastercard, RuPay'],['upi','UPI','Pay with any UPI app']].map(([id,title,text]) => <button key={id} onClick={() => setMethod(id)} className={cx('flex w-full items-center gap-4 rounded-2xl border p-5 text-left', method === id ? 'border-brand-500 bg-brand-50 dark:bg-brand-500/10' : 'border-slate-200 dark:border-white/10')}><span className="grid h-10 w-10 place-items-center rounded-xl bg-white text-brand-600 shadow-sm dark:bg-white/10"><CreditCard/></span><span><b>{title}</b><small className="mt-1 block text-slate-500">{text}</small></span><span className={cx('ml-auto h-5 w-5 rounded-full border-4', method === id ? 'border-brand-600' : 'border-slate-200')}/></button>)}</div>{method === 'razorpay' && <div className="mt-6 rounded-xl bg-blue-50 p-4 text-sm text-blue-800 dark:bg-blue-500/10 dark:text-blue-100"><b>Razorpay checkout</b> will open after you confirm your order.</div>}</>}{step === 3 && <><h1 className="text-2xl font-bold">Review your booking</h1><div className="mt-7 rounded-2xl bg-slate-50 p-5 dark:bg-white/5"><b>{event.title}</b><p className="mt-2 text-sm text-slate-500">{event.date} · {event.time}</p><p className="mt-1 text-sm text-slate-500">Seats: {selected.join(', ')}</p></div><div className="mt-6 flex items-center gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800 dark:bg-emerald-500/10 dark:text-emerald-100"><Check/> Tickets will be sent to {user?.email || 'your email'}.</div></>}</div><aside className="panel h-fit p-6"><p className="text-xs font-bold tracking-[.16em] text-brand-600">ORDER SUMMARY</p><h3 className="mt-4 font-bold">{event.title}</h3><p className="mt-1 text-sm text-slate-500">{selected.length || 0} ticket{selected.length !== 1 && 's'} · Gold</p><div className="my-5 border-t border-dashed border-slate-200"/><div className="space-y-2 text-sm text-slate-500"><p className="flex justify-between"><span>Tickets</span><span>₹{total.toLocaleString()}</span></p><p className="flex justify-between"><span>Convenience fee</span><span>₹{Math.round(total*.04).toLocaleString()}</span></p></div><div className="mt-5 flex justify-between border-t pt-4 text-lg font-bold"><span>Total</span><span>₹{Math.round(total*1.04).toLocaleString()}</span></div><button disabled={step === 1 && !selected.length} onClick={next} className="btn-primary mt-6 w-full disabled:cursor-not-allowed disabled:opacity-40">{step === 3 ? 'Pay securely' : 'Continue'} <ArrowRight size={17}/></button></aside></div></div> }
function Confirmation({ events }) { const { id } = useParams(); const location = useLocation(); const event = events.find(x => x.id === id); const booking = location.state?.booking || JSON.parse(sessionStorage.getItem(ticketStorageKey) || 'null'); if (!event || !booking || booking.eventUuid !== event.id) return <Navigate to="/bookings" replace />; return <div className="mx-auto max-w-xl px-6 py-20 text-center"><div className="mx-auto grid h-20 w-20 place-items-center rounded-full bg-emerald-100 text-emerald-600"><Check size={40}/></div><p className="mt-7 text-xs font-bold tracking-[.2em] text-emerald-600">BOOKING CONFIRMED</p><h1 className="mt-3 text-4xl font-bold">You’re going!</h1><p className="mt-4 text-slate-500">Your payment was confirmed and this ticket is safely in your account.</p><div className="panel mt-9 text-left"><img src={event.image} className="h-36 w-full object-cover" alt=""/><div className="space-y-3 p-6"><p className="text-xl font-bold">{event.title}</p><p className="flex items-center gap-2 text-sm text-slate-500"><CalendarDays size={16}/>{event.date} · {event.time}</p><p className="flex items-center gap-2 text-sm text-slate-500"><MapPin size={16}/>{event.venue}, {event.city}</p><p className="text-sm text-slate-600 dark:text-slate-300"><b>Seats:</b> {booking.seats.join(', ')}</p><p className="text-sm text-slate-600 dark:text-slate-300"><b>Amount paid:</b> {formatMoney(booking.totalAmount)}</p><div className="flex justify-between border-t pt-4"><span className="text-sm text-slate-500">Booking ID</span><b>{booking.bookingUUID}</b></div></div></div><div className="mt-7 flex flex-wrap justify-center gap-3"><button onClick={() => downloadTicketPdf({ booking, event })} className="btn-secondary"><Download size={17}/> Download PDF</button><Link className="btn-primary" to="/bookings">View my tickets</Link></div></div> }

function AuthPage({ register, onAuth }) {
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', phoneNumber: '', email: '', password: '', role: 'USER' });
  const [error, setError] = useState(''); const [submitting, setSubmitting] = useState(false);
  const change = key => e => setForm(current => ({ ...current, [key]: e.target.value }));
  const submit = async e => {
    e.preventDefault(); setError(''); setSubmitting(true);
    try {
      let token;
      if (register) {
        const [firstName, ...lastName] = form.name.trim().split(/\s+/);
        const profile = await api.createUser({
          firstName,
          lastName: lastName.join(' ') || firstName,
          email: form.email,
          phoneNumber: form.phoneNumber,
          role: form.role || 'USER',
        });
        token = await api.register({ email: form.email, password: form.password, userUuid: profile.userUuid });
        localStorage.setItem('user', JSON.stringify(toDisplayUser(profile)));
        onAuth(toDisplayUser(profile));
      } else {
        token = await api.login({ email: form.email, password: form.password });
        localStorage.setItem('accessToken', token.accessToken);
        const profile = await api.user(token.userUuid);
        const user = toDisplayUser(profile); localStorage.setItem('user', JSON.stringify(user)); onAuth(user);
      }
      localStorage.setItem('accessToken', token.accessToken); localStorage.setItem('refreshToken', token.refreshToken);
      navigate('/');
    } catch (err) { setError(err.message || 'Authentication failed.'); } finally { setSubmitting(false); }
  };
  return <div className="grid min-h-screen lg:grid-cols-2"><div className="hidden bg-[#151529] p-12 text-white lg:flex lg:flex-col"><Link to="/" className="flex items-center gap-2 font-bold"><span className="grid h-9 w-9 place-items-center rounded-xl bg-brand-600"><Ticket size={19}/></span>EventHorizon</Link><div className="my-auto max-w-md"><p className="text-sm font-bold tracking-[.2em] text-violet-300">MAKE IT MEMORABLE</p><h1 className="mt-5 text-5xl font-bold leading-tight">Everything exciting, in one place.</h1><p className="mt-5 text-slate-300">Discover the events you love. Manage every ticket with confidence.</p></div></div><div className="flex items-center justify-center p-6"><form onSubmit={submit} className="w-full max-w-md"><p className="text-xs font-bold tracking-[.18em] text-brand-600">{register ? 'JOIN EVENTHORIZON' : 'WELCOME BACK'}</p><h1 className="mt-2 text-4xl font-bold">{register ? 'Create your account' : 'Sign in to your account'}</h1>{register && <><label className="mt-7 block text-sm font-semibold">Full name<input required className="field" value={form.name} onChange={change('name')}/></label><label className="mt-5 block text-sm font-semibold">Phone number<input required className="field" placeholder="+919876543210" value={form.phoneNumber} onChange={change('phoneNumber')}/></label><div className="mt-5"><span className="block text-sm font-semibold mb-2">I want to register as</span><div className="grid grid-cols-2 gap-3"><label className={cx('flex cursor-pointer items-center justify-center gap-2 rounded-xl border p-3 text-sm font-semibold transition', form.role === 'USER' ? 'border-brand-600 bg-brand-50 text-brand-700 dark:border-brand-500 dark:bg-brand-500/10 dark:text-brand-300 ring-2 ring-brand-600/20' : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50 dark:border-white/10 dark:bg-[#1f202b] dark:text-slate-200')}><input type="radio" name="role" value="USER" checked={form.role === 'USER'} onChange={change('role')} className="sr-only"/><UserRound size={17}/> Attendee / User</label><label className={cx('flex cursor-pointer items-center justify-center gap-2 rounded-xl border p-3 text-sm font-semibold transition', form.role === 'ORGANIZER' ? 'border-brand-600 bg-brand-50 text-brand-700 dark:border-brand-500 dark:bg-brand-500/10 dark:text-brand-300 ring-2 ring-brand-600/20' : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50 dark:border-white/10 dark:bg-[#1f202b] dark:text-slate-200')}><input type="radio" name="role" value="ORGANIZER" checked={form.role === 'ORGANIZER'} onChange={change('role')} className="sr-only"/><LayoutDashboard size={17}/> Organizer</label></div></div></>}<label className="mt-5 block text-sm font-semibold">Email address<input required type="email" className="field" value={form.email} onChange={change('email')}/></label><label className="mt-5 block text-sm font-semibold">Password<input required minLength="8" type="password" className="field" value={form.password} onChange={change('password')}/></label>{error && <p className="mt-4 text-sm text-rose-600">{error}</p>}<button disabled={submitting} className="btn-primary mt-7 w-full disabled:opacity-50">{submitting ? 'Please wait…' : register ? 'Create account' : 'Sign in'} <ArrowRight size={17}/></button><p className="mt-7 text-center text-sm text-slate-500">{register ? 'Already have an account?' : 'New to EventHorizon?'} <Link className="font-bold text-brand-600" to={register ? '/login' : '/register'}>{register ? 'Sign in' : 'Create account'}</Link></p></form></div></div>;
}
function Bookings() { return <div className="mx-auto max-w-6xl px-6 py-12"><SectionTitle eyebrow="MY TICKETS" title="Your bookings"/><div className="grid gap-5">{bookingRows.map(b => <div key={b.id} className="panel grid overflow-hidden sm:grid-cols-[180px_1fr_auto]"><img src={b.event.image} className="h-40 w-full object-cover sm:h-full" alt=""/><div className="p-5"><span className="tag">{b.status}</span><h3 className="mt-3 text-xl font-bold">{b.event.title}</h3><p className="mt-2 text-sm text-slate-500">{b.event.date} · {b.event.time}</p><p className="mt-1 text-sm text-slate-500">{b.seats}</p></div><div className="flex items-center p-5 sm:justify-end"><button className="btn-secondary">View ticket</button></div></div>)}</div></div> }
function Account({ user, setUser, signOut }) {
  if (!user) return <Navigate to="/" replace />;
  const [saved, setSaved] = useState(false);
  const [name, setName] = useState(user?.name || 'Alex Morgan');
  const save = e => { e.preventDefault(); const updated = {...user, name}; setUser(updated); localStorage.setItem('user', JSON.stringify(updated)); setSaved(true); };
  return <div className="mx-auto max-w-5xl px-6 py-12"><SectionTitle eyebrow="ACCOUNT" title="Profile & settings"/><div className="grid gap-7 md:grid-cols-[200px_1fr]"><aside className="panel h-fit p-3"><Link className="menu !bg-brand-50 !text-brand-700 dark:!bg-brand-500/15 dark:!text-white" to="/account"><UserRound size={17}/> Personal details</Link><Link className="menu" to="/bookings"><Ticket size={17}/> My bookings</Link><button className="menu w-full text-left"><Settings size={17}/> Preferences</button><button onClick={signOut} className="menu w-full text-left text-rose-600"><LogOut size={17}/> Log out</button></aside><div className="panel p-6 sm:p-8"><h2 className="text-xl font-bold">Personal details</h2><p className="mt-1 text-sm text-slate-500">Keep your information up to date.</p><form onSubmit={save} className="mt-7 grid gap-5 sm:grid-cols-2"><label className="text-sm font-semibold">Full name<input className="field" value={name} onChange={e => setName(e.target.value)}/></label><label className="text-sm font-semibold">Email<input className="field" value={user?.email || ''} disabled/></label><label className="text-sm font-semibold">Phone number<input className="field" placeholder="+91 98765 43210"/></label><label className="text-sm font-semibold">City<input className="field" placeholder="New Delhi"/></label><div className="sm:col-span-2"><button className="btn-primary">Save changes</button>{saved && <span className="ml-3 text-sm font-semibold text-emerald-600">Saved!</span>}</div></form><div className="mt-10 border-t pt-7 dark:border-white/10"><h3 className="font-bold">Appearance</h3><p className="mt-1 text-sm text-slate-500">Use the sun/moon control in the header to switch light and dark mode.</p></div></div></div></div>;
}
function Organizer({ events }) { return <Dashboard role="Organizer" cards={[['₹1.28L','Gross sales'],['842','Tickets sold'],['3','Live events']]} title="Good morning, organizer"><div className="panel p-6"><div className="flex items-center justify-between"><h2 className="text-xl font-bold">Your events</h2><button className="btn-primary"><Plus size={17}/> Create event</button></div><div className="mt-5 divide-y dark:divide-white/10">{events.slice(0,3).map(e => <div className="flex items-center gap-4 py-4" key={e.id}><img className="h-12 w-12 rounded-xl object-cover" src={e.image}/><div className="flex-1"><b>{e.title}</b><p className="text-xs text-slate-500">{e.date} · {e.seats} seats left</p></div><button className="btn-secondary">Manage</button></div>)}</div></div></Dashboard> }
function OrganizerLive() {
  const [data, setData] = useState(null);
  useEffect(() => { api.organizerEvents().then(response => {
    const events = response.data || response;
    return Promise.allSettled(events.map(event => api.eventBookings(event.eventUuid))).then(bookings => setData({ events, bookings }));
  }).catch(() => setData({ events: [], bookings: [] })); }, []);
  if (!data) return <div className="mx-auto max-w-7xl px-6 py-12 text-slate-500">Loading your live event data…</div>;
  const bookings = data.bookings.flatMap(result => result.status === 'fulfilled' ? (result.value.data || result.value) : []);
  const confirmed = bookings.filter(booking => booking.status === 'CONFIRMED');
  const revenue = confirmed.reduce((total, booking) => total + Number(booking.totalAmount || 0), 0);
  return <Dashboard role="Organizer" title="Your event overview" cards={[[data.events.length.toLocaleString(), 'Your events'], [confirmed.length.toLocaleString(), 'Confirmed bookings'], [formatMoney(revenue), 'Confirmed sales'], [bookings.length.toLocaleString(), 'Booking requests']]}><div className="panel p-6"><h2 className="text-xl font-bold">Your events</h2>{data.events.length ? <div className="mt-5 divide-y dark:divide-white/10">{data.events.map(event => <div className="flex items-center gap-4 py-4" key={event.eventUuid}><div className="grid h-12 w-12 place-items-center rounded-xl bg-brand-50 text-brand-600"><CalendarDays size={20}/></div><div className="flex-1"><b>{event.title}</b><p className="mt-1 text-xs text-slate-500">{event.eventDate} · {event.venueName}, {event.city} · {event.availableSeats} seats available</p></div><span className="tag">{event.status || 'UPCOMING'}</span></div>)}</div> : <p className="mt-5 text-sm text-slate-500">You have not created any events yet.</p>}</div></Dashboard>;
}

function OrganizerConsole({ user }) {
  const blank = { title: '', description: '', category: 'Music', organizerName: user.name, eventDate: '', startTime: '', endTime: '', venueName: '', city: '', address: '', totalSeats: 100, ticketPrice: 0 };
  const [events, setEvents] = useState([]); const [form, setForm] = useState(blank); const [editing, setEditing] = useState(null); const [open, setOpen] = useState(false); const [error, setError] = useState(''); const [saving, setSaving] = useState(false);
  const load = () => api.organizerEvents().then(response => setEvents(response.data || response)).catch(err => setError(err.message || 'Unable to load your events.'));
  useEffect(() => { load(); }, []);
  const change = key => event => setForm(current => ({ ...current, [key]: event.target.value }));
  const manage = event => { setEditing(event); setForm({ title: event.title || '', description: event.description || '', category: event.category || 'Music', organizerName: event.organizerName || user.name, eventDate: event.eventDate || '', startTime: event.startTime || '', endTime: event.endTime || '', venueName: event.venueName || '', city: event.city || '', address: event.address || '', totalSeats: event.totalSeats || 0, ticketPrice: event.ticketPrice || 0 }); setError(''); setOpen(true); };
  const submit = async event => { event.preventDefault(); if (form.eventDate < new Date().toISOString().slice(0, 10)) return setError('Event date cannot be in the past.'); setSaving(true); setError(''); const payload = { ...form, totalSeats: Number(form.totalSeats), ticketPrice: Number(form.ticketPrice) }; try { if (editing) await api.updateEvent(editing.eventUuid, payload); else await api.createEvent(payload); setOpen(false); setEditing(null); setForm(blank); load(); } catch (err) { setError(err.message || 'Unable to save the event.'); } finally { setSaving(false); } };
  const fields = [['title', 'Event title', 'text'], ['category', 'Category', 'text'], ['eventDate', 'Event date', 'date'], ['startTime', 'Start time', 'time'], ['endTime', 'End time', 'time'], ['venueName', 'Venue', 'text'], ['city', 'City', 'text'], ['address', 'Address', 'text'], ['totalSeats', 'Total seats', 'number'], ['ticketPrice', 'Ticket price (Rs.)', 'number']];
  return <div className="mx-auto max-w-6xl px-6 py-12"><p className="text-xs font-bold tracking-[.18em] text-brand-600">ORGANIZER HUB</p><div className="mt-2 flex flex-wrap items-center justify-between gap-4"><h1 className="text-4xl font-bold">Manage your events</h1><button onClick={() => { setEditing(null); setForm(blank); setError(''); setOpen(true); }} className="btn-primary"><Plus size={17}/> Create event</button></div>{error && <p className="mt-5 text-sm text-rose-600">{error}</p>}{open && <form onSubmit={submit} className="panel mt-8 grid gap-4 p-6 sm:grid-cols-2"><h2 className="sm:col-span-2 text-xl font-bold">{editing ? 'Edit event' : 'Create event'}</h2>{fields.map(([key, label, type]) => <label key={key} className="text-sm font-semibold">{label}<input required={key !== 'address'} type={type} min={type === 'date' ? new Date().toISOString().slice(0, 10) : type === 'number' ? '0' : undefined} className="field" value={form[key]} onChange={change(key)}/></label>)}<label className="sm:col-span-2 text-sm font-semibold">Description<textarea className="field min-h-24" value={form.description} onChange={change('description')}/></label><div className="sm:col-span-2 flex gap-3"><button disabled={saving} className="btn-primary">{saving ? 'Saving…' : editing ? 'Save changes' : 'Create event'}</button><button type="button" onClick={() => setOpen(false)} className="btn-secondary">Cancel</button></div></form>}<div className="panel mt-8 p-6"><h2 className="text-xl font-bold">Your events</h2>{events.length ? <div className="mt-5 divide-y dark:divide-white/10">{events.map(event => <div className="flex flex-wrap items-center gap-4 py-4" key={event.eventUuid}><div className="flex-1"><b>{event.title}</b><p className="mt-1 text-sm text-slate-500">{event.eventDate} · {event.venueName}, {event.city} · {event.availableSeats}/{event.totalSeats} seats available</p></div><span className="tag">{event.status || 'UPCOMING'}</span><button onClick={() => manage(event)} className="btn-secondary">Manage</button></div>)}</div> : <p className="mt-5 text-sm text-slate-500">Create your first event to see it here.</p>}</div></div>;
}

function Admin() {
  const [data, setData] = useState(null);
  useEffect(() => { Promise.allSettled([api.adminAnalytics(), api.users(), api.events()]).then(([analytics, users, events]) => setData({ analytics: analytics.status === 'fulfilled' ? (analytics.value.data || analytics.value) : null, users: users.status === 'fulfilled' ? (users.value.data || users.value) : [], events: events.status === 'fulfilled' ? (events.value.data || events.value) : [] })); }, []);
  if (!data) return <div className="mx-auto max-w-7xl px-6 py-12 text-slate-500">Loading live analytics…</div>;
  const activeUsers = data.users.filter(user => user.active).length;
  const liveEvents = data.events.length;
  const analytics = data.analytics;
  return <Dashboard role="Admin" title="Platform overview" cards={[[activeUsers.toLocaleString(), 'Active users'], [liveEvents.toLocaleString(), 'Published events'], [analytics ? formatMoney(analytics.revenue) : 'Unavailable', 'Confirmed sales'], [analytics ? analytics.ticketsSold.toLocaleString() : 'Unavailable', 'Tickets sold']]}><div className="panel p-6"><h2 className="text-xl font-bold">Live platform activity</h2>{analytics ? <div className="mt-5 grid gap-4 sm:grid-cols-2"><p className="rounded-xl bg-slate-50 p-4 text-sm dark:bg-white/5"><b>{analytics.totalBookings}</b><span className="block mt-1 text-slate-500">Bookings created</span></p><p className="rounded-xl bg-slate-50 p-4 text-sm dark:bg-white/5"><b>{analytics.confirmedBookings}</b><span className="block mt-1 text-slate-500">Payments confirmed</span></p></div> : <p className="mt-4 text-sm text-rose-600">Booking analytics are temporarily unavailable. Restart booking-service to load sales and ticket data.</p>}</div></Dashboard>;
}
function Dashboard({ role, cards, title, children }) { return <div className="mx-auto max-w-6xl px-6 py-12"><p className="text-xs font-bold tracking-[.18em] text-brand-600">{role.toUpperCase()} HUB</p><h1 className="mt-2 text-4xl font-bold">{title}</h1><div className="mt-8 grid gap-4 sm:grid-cols-3">{cards.map(([a,b]) => <Stat key={b} value={a} label={b}/>)}</div><div className="mt-8">{children}</div></div> }
function About() { return <div className="mx-auto max-w-4xl px-6 py-16"><p className="text-xs font-bold tracking-[.18em] text-brand-600">OUR STORY</p><h1 className="mt-3 text-5xl font-bold leading-tight">More life, less logistics.</h1><p className="mt-7 max-w-2xl text-lg leading-8 text-slate-600 dark:text-slate-300">EventHorizon makes it effortless to find the experiences that move you—and to get in the room. We partner with organizers to make every booking feel clear, secure and exciting.</p><div className="mt-14 grid gap-5 sm:grid-cols-3"><Stat value="2026" label="built for today’s audiences"/><Stat value="24/7" label="ticketing support"/><Stat value="India" label="our home and stage"/></div></div> }

export default App;
