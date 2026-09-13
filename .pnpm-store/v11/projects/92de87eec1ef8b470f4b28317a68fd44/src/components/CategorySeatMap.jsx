import React, { useMemo } from 'react';
import { Film, Music, Mic, Theater, Trophy, Presentation } from 'lucide-react';

export function detectLayoutType(category = '', title = '') {
  const text = `${category} ${title}`.toLowerCase();
  if (/movie|film|cinema|imax|multiplex/i.test(text)) return 'MOVIE';
  if (/comedy|standup|laughter|cabaret|club/i.test(text)) return 'COMEDY';
  if (/music|concert|festival|gig|dj|band/i.test(text)) return 'CONCERT';
  if (/theatre|theater|drama|play|musical|opera/i.test(text)) return 'THEATRE';
  if (/sports|stadium|cricket|football|match|arena/i.test(text)) return 'SPORTS';
  if (/conference|seminar|summit|keynote|expo/i.test(text)) return 'CONFERENCE';
  return 'MOVIE'; // Default fallback
}

export function getSeatPrice(seatId, basePrice) {
  return Number(basePrice) || 500;
}

export function getSeatCategoryLabel(seatId, layoutType) {
  if (layoutType === 'MOVIE') {
    if (seatId.startsWith('R')) return 'Recliner VIP';
    if (['A', 'B', 'C', 'D'].includes(seatId[0])) return 'Prime';
    return 'Classic';
  }
  if (layoutType === 'CONCERT') {
    if (seatId.startsWith('PIT')) return 'VIP Standing Pit';
    if (['A', 'B', 'C'].includes(seatId[0])) return 'Platinum Arena';
    return 'Grandstand';
  }
  if (layoutType === 'COMEDY') {
    if (seatId.startsWith('T1') || seatId.startsWith('T2') || seatId.startsWith('T3') || seatId.startsWith('T4')) return 'Front Table VIP';
    if (seatId.startsWith('T')) return 'Cabaret Table';
    return 'Bar Stool';
  }
  if (layoutType === 'THEATRE') {
    if (seatId.startsWith('Box')) return 'Royal Box';
    if (['A', 'B', 'C'].includes(seatId[0])) return 'Orchestra Stalls';
    return 'Balcony';
  }
  if (layoutType === 'SPORTS') {
    if (seatId.startsWith('VIP')) return 'Pavilion VIP';
    if (seatId.startsWith('E') || seatId.startsWith('W')) return 'Gold Stand';
    return 'Silver Stand';
  }
  return 'Standard Seat';
}

export function CategorySeatMap({
  event,
  selectedSeats = [],
  onToggleSeat,
  bookedSeats = [],
  blockedSeats = [],
  maxSeats = 6,
}) {
  const activeLayout = useMemo(() => detectLayoutType(event?.category, event?.title), [event?.category, event?.title]);
  const basePrice = Number(event?.price || 500);

  const isSeatDisabled = (seatId) => bookedSeats.includes(seatId) || blockedSeats.includes(seatId);

  const getSeatClass = (seatId) => {
    const booked = bookedSeats.includes(seatId);
    const blocked = blockedSeats.includes(seatId);
    const selected = selectedSeats.includes(seatId);

    if (booked) return 'bg-rose-500 text-white cursor-not-allowed opacity-90 shadow-inner';
    if (blocked) return 'bg-slate-400 text-slate-100 cursor-not-allowed opacity-80';
    if (selected) return 'bg-brand-600 text-white ring-2 ring-brand-400 shadow-md scale-105 font-black';

    // Section specific styling
    if (activeLayout === 'MOVIE') {
      if (seatId.startsWith('R')) return 'bg-amber-100 text-amber-900 border border-amber-300 hover:bg-amber-200 dark:bg-amber-900/40 dark:text-amber-200 dark:border-amber-700/50';
      if (['A', 'B', 'C', 'D'].includes(seatId[0])) return 'bg-sky-100 text-sky-900 hover:bg-sky-200 dark:bg-sky-950/60 dark:text-sky-200';
      return 'bg-slate-200 text-slate-800 hover:bg-slate-300 dark:bg-slate-800 dark:text-slate-200';
    }

    if (activeLayout === 'CONCERT') {
      if (seatId.startsWith('PIT')) return 'bg-purple-600 text-white font-extrabold hover:bg-purple-500 shadow-sm';
      if (['A', 'B'].includes(seatId[0])) return 'bg-indigo-100 text-indigo-900 border border-indigo-200 hover:bg-indigo-200 dark:bg-indigo-950/60 dark:text-indigo-200';
      return 'bg-slate-200 text-slate-800 hover:bg-slate-300 dark:bg-slate-800 dark:text-slate-200';
    }

    if (activeLayout === 'COMEDY') {
      if (seatId.startsWith('T1') || seatId.startsWith('T2') || seatId.startsWith('T3') || seatId.startsWith('T4')) {
        return 'bg-amber-50 text-amber-900 border border-amber-300 hover:bg-amber-100 dark:bg-amber-950/50 dark:text-amber-200';
      }
      if (seatId.startsWith('T')) return 'bg-violet-100 text-violet-900 border border-violet-200 hover:bg-violet-200 dark:bg-violet-950/50 dark:text-violet-200';
      return 'bg-emerald-100 text-emerald-900 hover:bg-emerald-200 dark:bg-emerald-950/50 dark:text-emerald-200';
    }

    return 'bg-slate-200 text-slate-800 hover:bg-slate-300 dark:bg-slate-800 dark:text-slate-200';
  };

  const getLayoutMeta = () => {
    switch (activeLayout) {
      case 'MOVIE':
        return { title: 'Cinema Hall Layout', icon: Film, desc: 'Curved Dolby Atmos screen with recliner balcony & auditorium rows' };
      case 'CONCERT':
        return { title: 'Concert Live Arena', icon: Music, desc: 'Illuminated main stage, standing VIP fan pit & tiered balcony' };
      case 'COMEDY':
        return { title: 'Comedy Club Layout', icon: Mic, desc: 'Intimate spotlight brick stage with front round tables & cabaret seating' };
      case 'THEATRE':
        return { title: 'Theatre Proscenium', icon: Theater, desc: 'Velvet curtain stage, orchestra pit, royal boxes & balcony circle' };
      case 'SPORTS':
        return { title: 'Sports Stadium Arena', icon: Trophy, desc: 'Main ground pitch surrounded by pavilion VIP & stadium stands' };
      case 'CONFERENCE':
        return { title: 'Conference Hall', icon: Presentation, desc: 'Keynote presentation stage with executive & delegate classroom rows' };
      default:
        return { title: 'Auditorium Seating', icon: Film, desc: 'Standard auditorium seating configuration' };
    }
  };

  const layoutMeta = getLayoutMeta();
  const IconComponent = layoutMeta.icon;

  return (
    <div className="w-full space-y-6">
      {/* Category Layout Indicator Header */}
      <div className="flex items-center justify-between rounded-2xl border border-brand-200/60 bg-brand-50/50 px-4 py-3 dark:border-brand-900/40 dark:bg-brand-950/20">
        <div className="flex items-center gap-3">
          <span className="grid h-10 w-10 place-items-center rounded-xl bg-brand-600 text-white shadow-sm">
            <IconComponent size={20} />
          </span>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-black uppercase tracking-wider text-brand-700 dark:text-brand-300">
                {event?.category || activeLayout} Layout
              </span>
              <span className="rounded-full bg-brand-200/70 px-2 py-0.5 text-[10px] font-extrabold text-brand-900 dark:bg-brand-900/80 dark:text-brand-100">
                AUTO-SELECTED
              </span>
            </div>
            <p className="text-xs text-slate-600 dark:text-slate-300 font-medium">{layoutMeta.desc}</p>
          </div>
        </div>
        <div className="text-right">
          <p className="text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">Fixed Ticket Price</p>
          <p className="text-sm font-black text-brand-600 dark:text-brand-400">₹{basePrice.toLocaleString('en-IN')} / seat</p>
        </div>
      </div>

      {/* Main Seat Map Area */}
      <div className="rounded-3xl border border-slate-200 bg-white p-5 sm:p-7 shadow-sm dark:border-white/10 dark:bg-[#181924]">
        {/* RENDER CINEMA MOVIE HALL LAYOUT */}
        {activeLayout === 'MOVIE' && (
          <div className="space-y-7">
            {/* Cinema Screen Visual */}
            <div className="relative mx-auto max-w-xl text-center">
              <div className="cinema-screen-curve h-10 w-full bg-gradient-to-b from-sky-300 via-sky-100 to-transparent dark:from-sky-500/40 dark:via-sky-900/20" />
              <div className="mt-1 flex items-center justify-center gap-2 text-[11px] font-extrabold uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">
                <Film size={13} className="text-sky-500" /> CURVED CINEMA SCREEN (DOLBY ATMOS)
              </div>
              <p className="mt-0.5 text-[10px] text-slate-400">All eyes forward · Projection beam from rear booth</p>
            </div>

            {/* Recliner VIP Section */}
            <div className="rounded-2xl border border-amber-200/70 bg-amber-50/50 p-4 dark:border-amber-900/40 dark:bg-amber-950/20">
              <div className="mb-3 flex items-center justify-between">
                <span className="flex items-center gap-1.5 text-xs font-extrabold text-amber-900 dark:text-amber-200">
                  👑 RECLINER VIP BALCONY
                </span>
                <span className="text-xs font-semibold text-amber-700 dark:text-amber-400">
                  ₹{basePrice.toLocaleString('en-IN')} (Plush Leather Recliners)
                </span>
              </div>
              {['R1', 'R2'].map((row) => (
                <div key={row} className="mt-2.5 flex items-center gap-3">
                  <span className="w-8 text-center text-xs font-bold text-slate-500">{row}</span>
                  <div className="grid flex-1 grid-cols-10 gap-2">
                    {Array.from({ length: 10 }, (_, i) => `${row}-${i + 1}`).map((seatId) => (
                      <button
                        key={seatId}
                        type="button"
                        disabled={isSeatDisabled(seatId)}
                        onClick={() => onToggleSeat(seatId)}
                        className={`seat-btn h-10 ${getSeatClass(seatId)}`}
                        title={`${seatId} - ₹${basePrice.toLocaleString('en-IN')}`}
                      >
                        {seatId.split('-')[1]}
                      </button>
                    ))}
                  </div>
                </div>
              ))}
            </div>

            {/* Prime & Classic Hall Rows with Dual Aisles */}
            <div className="space-y-4">
              <div className="flex items-center justify-between border-b border-slate-100 pb-2 dark:border-white/5">
                <span className="text-xs font-bold text-slate-500">AUDITORIUM PRIME & CLASSIC SEATING</span>
                <span className="text-xs font-semibold text-slate-500 dark:text-slate-400">₹{basePrice.toLocaleString('en-IN')} per seat</span>
              </div>

              {['A', 'B', 'C', 'D', 'E', 'F'].map((row) => {
                const rowSeats = Array.from({ length: 12 }, (_, i) => `${row}${i + 1}`);
                const leftBlock = rowSeats.slice(0, 3);
                const centerBlock = rowSeats.slice(3, 9);
                const rightBlock = rowSeats.slice(9, 12);

                return (
                  <div key={row} className="flex items-center gap-2 sm:gap-3">
                    <span className="w-8 text-center text-xs font-bold text-slate-500">
                      ROW {row}
                    </span>
                    <div className="flex flex-1 items-center gap-2 sm:gap-3">
                      {/* Left Block */}
                      <div className="grid grid-cols-3 gap-1.5">
                        {leftBlock.map((seatId) => (
                          <button
                            key={seatId}
                            type="button"
                            disabled={isSeatDisabled(seatId)}
                            onClick={() => onToggleSeat(seatId)}
                            className={`seat-btn h-9 w-7 sm:w-8 ${getSeatClass(seatId)}`}
                            title={`${seatId} - ₹${basePrice.toLocaleString('en-IN')}`}
                          >
                            {seatId.slice(1)}
                          </button>
                        ))}
                      </div>

                      {/* Aisle 1 */}
                      <div className="flex h-9 w-3 sm:w-4 items-center justify-center text-[9px] font-bold text-slate-300 dark:text-slate-600">
                        ║
                      </div>

                      {/* Center Block */}
                      <div className="grid flex-1 grid-cols-6 gap-1.5">
                        {centerBlock.map((seatId) => (
                          <button
                            key={seatId}
                            type="button"
                            disabled={isSeatDisabled(seatId)}
                            onClick={() => onToggleSeat(seatId)}
                            className={`seat-btn h-9 ${getSeatClass(seatId)}`}
                            title={`${seatId} - ₹${basePrice.toLocaleString('en-IN')}`}
                          >
                            {seatId.slice(1)}
                          </button>
                        ))}
                      </div>

                      {/* Aisle 2 */}
                      <div className="flex h-9 w-3 sm:w-4 items-center justify-center text-[9px] font-bold text-slate-300 dark:text-slate-600">
                        ║
                      </div>

                      {/* Right Block */}
                      <div className="grid grid-cols-3 gap-1.5">
                        {rightBlock.map((seatId) => (
                          <button
                            key={seatId}
                            type="button"
                            disabled={isSeatDisabled(seatId)}
                            onClick={() => onToggleSeat(seatId)}
                            className={`seat-btn h-9 w-7 sm:w-8 ${getSeatClass(seatId)}`}
                            title={`${seatId} - ₹${basePrice.toLocaleString('en-IN')}`}
                          >
                            {seatId.slice(1)}
                          </button>
                        ))}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* RENDER CONCERT LIVE STAGE LAYOUT */}
        {activeLayout === 'CONCERT' && (
          <div className="space-y-7">
            {/* Illuminated Concert Stage */}
            <div className="concert-stage-glow relative overflow-hidden rounded-2xl py-6 text-center text-white">
              <div className="relative z-10">
                <span className="inline-flex items-center gap-2 rounded-full bg-white/20 px-3.5 py-1.5 text-xs font-black uppercase tracking-[0.2em] backdrop-blur">
                  <Music size={15} /> CONCERT MAIN STAGE & DJ BOOTH
                </span>
                <p className="mt-2 text-xs text-purple-200">Spotlight Lighting · High-Fidelity Concert Sound Wall</p>
              </div>
            </div>

            {/* VIP Standing Fan Pit Zone */}
            <div className="rounded-2xl border-2 border-dashed border-purple-300 bg-purple-50/70 p-4 dark:border-purple-800 dark:bg-purple-950/20">
              <div className="mb-3 flex items-center justify-between">
                <span className="text-xs font-extrabold text-purple-900 dark:text-purple-300">
                  🔥 FRONT VIP STANDING FAN PIT (Stage Touch Distance)
                </span>
                <span className="text-xs font-bold text-purple-700 dark:text-purple-400">
                  ₹{basePrice.toLocaleString('en-IN')} (Unreserved Standing Zone)
                </span>
              </div>
              <div className="grid grid-cols-4 gap-2.5 sm:grid-cols-8">
                {Array.from({ length: 8 }, (_, i) => `PIT-${i + 1}`).map((seatId) => (
                  <button
                    key={seatId}
                    type="button"
                    disabled={isSeatDisabled(seatId)}
                    onClick={() => onToggleSeat(seatId)}
                    className={`seat-btn h-12 flex-col !rounded-xl ${getSeatClass(seatId)}`}
                    title={`${seatId} - ₹${basePrice.toLocaleString('en-IN')}`}
                  >
                    <span className="text-[10px] opacity-80">VIP</span>
                    <span className="text-xs font-black">{seatId.replace('PIT-', 'PIT ')}</span>
                  </button>
                ))}
              </div>
            </div>

            {/* Platinum Arena & Grandstand Seating */}
            <div className="space-y-4">
              <div className="flex justify-between border-b border-slate-100 pb-2 dark:border-white/5">
                <span className="text-xs font-bold text-slate-500">ARENA FLOOR & TIERED GRANDSTAND</span>
                <span className="text-xs font-semibold text-slate-500 dark:text-slate-400">₹{basePrice.toLocaleString('en-IN')} per seat</span>
              </div>

              {['A', 'B', 'C', 'D', 'E', 'F'].map((row) => {
                const seatsCount = 12;
                const rowSeats = Array.from({ length: seatsCount }, (_, i) => `${row}${i + 1}`);
                const label = getSeatCategoryLabel(`${row}1`, 'CONCERT');

                return (
                  <div key={row} className="flex items-center gap-3">
                    <span className="w-12 text-xs font-bold text-slate-500">ROW {row}</span>
                    <div className="grid flex-1 grid-cols-6 gap-2 sm:grid-cols-12">
                      {rowSeats.map((seatId) => (
                        <button
                          key={seatId}
                          type="button"
                          disabled={isSeatDisabled(seatId)}
                          onClick={() => onToggleSeat(seatId)}
                          className={`seat-btn h-9 ${getSeatClass(seatId)}`}
                          title={`${seatId} (${label}) - ₹${basePrice.toLocaleString('en-IN')}`}
                        >
                          {seatId.slice(1)}
                        </button>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* RENDER COMEDY CLUB LAYOUT */}
        {activeLayout === 'COMEDY' && (
          <div className="space-y-7">
            {/* Comedy Stage Spotlight */}
            <div className="comedy-stage-spotlight relative rounded-2xl border border-amber-500/30 p-6 text-center text-white shadow-lg">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-amber-400 text-slate-950 shadow-glow">
                <Mic size={24} />
              </div>
              <h3 className="mt-2 text-sm font-extrabold uppercase tracking-widest text-amber-300">
                COMEDY SPOTLIGHT STAGE
              </h3>
              <p className="mt-1 text-xs text-slate-300">Intimate brick-wall setup · Live audience crowd work zone</p>
            </div>

            {/* Front Stage VIP Round Tables */}
            <div>
              <div className="mb-3 flex items-center justify-between">
                <span className="text-xs font-extrabold text-amber-900 dark:text-amber-300">
                  🥂 FRONT STAGE VIP TABLES (4 Seats Per Table)
                </span>
                <span className="text-xs font-bold text-amber-700 dark:text-amber-400">
                  ₹{basePrice.toLocaleString('en-IN')} per seat
                </span>
              </div>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                {['T1', 'T2', 'T3', 'T4'].map((tableNo) => (
                  <div
                    key={tableNo}
                    className="rounded-2xl border border-amber-200 bg-amber-50/60 p-3.5 text-center dark:border-amber-900/50 dark:bg-amber-950/30"
                  >
                    <span className="text-xs font-bold text-amber-900 dark:text-amber-200">
                      TABLE {tableNo.replace('T', '')} (VIP)
                    </span>
                    <div className="mt-3 grid grid-cols-2 gap-2">
                      {['A', 'B', 'C', 'D'].map((pos) => {
                        const seatId = `${tableNo}-${pos}`;
                        return (
                          <button
                            key={seatId}
                            type="button"
                            disabled={isSeatDisabled(seatId)}
                            onClick={() => onToggleSeat(seatId)}
                            className={`seat-btn h-9 ${getSeatClass(seatId)}`}
                            title={`${seatId} - ₹${basePrice.toLocaleString('en-IN')}`}
                          >
                            Seat {pos}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Main Cabaret Floor Tables */}
            <div>
              <div className="mb-3 flex items-center justify-between">
                <span className="text-xs font-extrabold text-slate-700 dark:text-slate-300">
                  🍸 CABARET FLOOR TABLES (Tables 5 to 10)
                </span>
                <span className="text-xs font-semibold text-slate-500 dark:text-slate-400">₹{basePrice.toLocaleString('en-IN')} per seat</span>
              </div>
              <div className="grid gap-4 sm:grid-cols-3 lg:grid-cols-6">
                {['T5', 'T6', 'T7', 'T8', 'T9', 'T10'].map((tableNo) => (
                  <div
                    key={tableNo}
                    className="rounded-2xl border border-slate-200 bg-slate-50/80 p-3 text-center dark:border-white/10 dark:bg-white/5"
                  >
                    <span className="text-xs font-bold text-slate-600 dark:text-slate-300">
                      T-{tableNo.replace('T', '')}
                    </span>
                    <div className="mt-2 grid grid-cols-2 gap-1.5">
                      {['A', 'B', 'C', 'D'].map((pos) => {
                        const seatId = `${tableNo}-${pos}`;
                        return (
                          <button
                            key={seatId}
                            type="button"
                            disabled={isSeatDisabled(seatId)}
                            onClick={() => onToggleSeat(seatId)}
                            className={`seat-btn h-8 text-[11px] ${getSeatClass(seatId)}`}
                            title={`${seatId} - ₹${basePrice.toLocaleString('en-IN')}`}
                          >
                            {pos}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Rear High-Top Bar Stools */}
            <div className="rounded-2xl border border-emerald-200/70 bg-emerald-50/50 p-4 dark:border-emerald-900/40 dark:bg-emerald-950/20">
              <div className="mb-3 flex items-center justify-between">
                <span className="text-xs font-extrabold text-emerald-900 dark:text-emerald-300">
                  🍺 REAR HIGH-TOP BAR STOOLS
                </span>
                <span className="text-xs font-bold text-emerald-700 dark:text-emerald-400">
                  ₹{basePrice.toLocaleString('en-IN')} (Back Wall Raised Stools)
                </span>
              </div>
              <div className="grid grid-cols-6 gap-2 sm:grid-cols-12">
                {Array.from({ length: 12 }, (_, i) => `H${i + 1}`).map((seatId) => (
                  <button
                    key={seatId}
                    type="button"
                    disabled={isSeatDisabled(seatId)}
                    onClick={() => onToggleSeat(seatId)}
                    className={`seat-btn h-9 ${getSeatClass(seatId)}`}
                    title={`${seatId} - ₹${basePrice.toLocaleString('en-IN')}`}
                  >
                    {seatId}
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* RENDER THEATRE PROSCENIUM LAYOUT */}
        {activeLayout === 'THEATRE' && (
          <div className="space-y-7">
            {/* Red Velvet Stage */}
            <div className="theatre-proscenium-glow relative rounded-2xl p-6 text-center text-white shadow-xl">
              <span className="inline-flex items-center gap-2 rounded-full bg-black/30 px-4 py-1 text-xs font-black uppercase tracking-[0.2em]">
                <Theater size={16} /> PROSCENIUM VELVET CURTAIN STAGE
              </span>
              <div className="mt-3 text-xs text-rose-200">Orchestra Pit Located Direct Front of Stage</div>
            </div>

            {/* Royal Boxes on Wings */}
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="rounded-2xl border border-amber-300 bg-amber-50/70 p-3.5 dark:border-amber-800 dark:bg-amber-950/30">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-extrabold text-amber-900 dark:text-amber-300">🏰 ROYAL BOX (LEFT WING)</span>
                  <span className="text-xs font-bold text-amber-700 dark:text-amber-400">₹{basePrice.toLocaleString('en-IN')}</span>
                </div>
                <div className="mt-2.5 grid grid-cols-4 gap-2">
                  {['Box-L1', 'Box-L2', 'Box-L3', 'Box-L4'].map((seatId) => (
                    <button
                      key={seatId}
                      type="button"
                      disabled={isSeatDisabled(seatId)}
                      onClick={() => onToggleSeat(seatId)}
                      className={`seat-btn h-9 ${getSeatClass(seatId)}`}
                      title={`${seatId} - ₹${basePrice.toLocaleString('en-IN')}`}
                    >
                      {seatId.replace('Box-', '')}
                    </button>
                  ))}
                </div>
              </div>

              <div className="rounded-2xl border border-amber-300 bg-amber-50/70 p-3.5 dark:border-amber-800 dark:bg-amber-950/30">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-extrabold text-amber-900 dark:text-amber-300">🏰 ROYAL BOX (RIGHT WING)</span>
                  <span className="text-xs font-bold text-amber-700 dark:text-amber-400">₹{basePrice.toLocaleString('en-IN')}</span>
                </div>
                <div className="mt-2.5 grid grid-cols-4 gap-2">
                  {['Box-R1', 'Box-R2', 'Box-R3', 'Box-R4'].map((seatId) => (
                    <button
                      key={seatId}
                      type="button"
                      disabled={isSeatDisabled(seatId)}
                      onClick={() => onToggleSeat(seatId)}
                      className={`seat-btn h-9 ${getSeatClass(seatId)}`}
                      title={`${seatId} - ₹${basePrice.toLocaleString('en-IN')}`}
                    >
                      {seatId.replace('Box-', '')}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            {/* Orchestra Stalls */}
            <div className="space-y-3">
              <div className="flex justify-between border-b border-slate-100 pb-2 dark:border-white/5">
                <span className="text-xs font-bold text-slate-500">ORCHESTRA STALLS & BALCONY CIRCLE</span>
                <span className="text-xs font-semibold text-slate-500 dark:text-slate-400">₹{basePrice.toLocaleString('en-IN')} per seat</span>
              </div>
              {['A', 'B', 'C', 'D', 'E', 'F'].map((row) => (
                <div key={row} className="flex items-center gap-3">
                  <span className="w-12 text-xs font-bold text-slate-500">ROW {row}</span>
                  <div className="grid flex-1 grid-cols-5 gap-2 sm:grid-cols-10">
                    {Array.from({ length: 10 }, (_, i) => `${row}${i + 1}`).map((seatId) => (
                      <button
                        key={seatId}
                        type="button"
                        disabled={isSeatDisabled(seatId)}
                        onClick={() => onToggleSeat(seatId)}
                        className={`seat-btn h-9 ${getSeatClass(seatId)}`}
                        title={`${seatId} - ₹${basePrice.toLocaleString('en-IN')}`}
                      >
                        {seatId.slice(1)}
                      </button>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* RENDER SPORTS STADIUM LAYOUT */}
        {activeLayout === 'SPORTS' && (
          <div className="space-y-7">
            {/* Stadium Pitch Center */}
            <div className="sports-field-gradient relative rounded-2xl py-8 text-center text-white shadow-lg">
              <span className="inline-flex items-center gap-2 rounded-full bg-white/20 px-4 py-1 text-xs font-extrabold uppercase tracking-widest backdrop-blur">
                <Trophy size={16} /> MAIN STADIUM PITCH / ARENA GROUND
              </span>
              <p className="mt-2 text-xs text-emerald-100">Live Boundary Line View · High-Impact Audio</p>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="rounded-2xl border border-sky-200 bg-sky-50/50 p-4 dark:border-sky-900/40 dark:bg-sky-950/20">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-extrabold text-sky-900 dark:text-sky-300">🏏 NORTH PAVILION STAND (VIP)</span>
                  <span className="text-xs font-bold text-sky-700 dark:text-sky-400">₹{basePrice.toLocaleString('en-IN')}</span>
                </div>
                <div className="mt-3 grid grid-cols-5 gap-2">
                  {Array.from({ length: 10 }, (_, i) => `VIP-${i + 1}`).map((seatId) => (
                    <button
                      key={seatId}
                      type="button"
                      disabled={isSeatDisabled(seatId)}
                      onClick={() => onToggleSeat(seatId)}
                      className={`seat-btn h-9 ${getSeatClass(seatId)}`}
                      title={`${seatId} - ₹${basePrice.toLocaleString('en-IN')}`}
                    >
                      {seatId}
                    </button>
                  ))}
                </div>
              </div>

              <div className="rounded-2xl border border-indigo-200 bg-indigo-50/50 p-4 dark:border-indigo-900/40 dark:bg-indigo-950/20">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-extrabold text-indigo-900 dark:text-indigo-300">🏟️ EAST STAND (GOLD)</span>
                  <span className="text-xs font-bold text-indigo-700 dark:text-indigo-400">₹{basePrice.toLocaleString('en-IN')}</span>
                </div>
                <div className="mt-3 grid grid-cols-5 gap-2">
                  {Array.from({ length: 10 }, (_, i) => `E${i + 1}`).map((seatId) => (
                    <button
                      key={seatId}
                      type="button"
                      disabled={isSeatDisabled(seatId)}
                      onClick={() => onToggleSeat(seatId)}
                      className={`seat-btn h-9 ${getSeatClass(seatId)}`}
                      title={`${seatId} - ₹${basePrice.toLocaleString('en-IN')}`}
                    >
                      {seatId}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            {/* South Stand */}
            <div className="rounded-2xl border border-slate-200 bg-slate-50/80 p-4 dark:border-white/10 dark:bg-white/5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-extrabold text-slate-700 dark:text-slate-300">📣 SOUTH STAND (SILVER)</span>
                <span className="text-xs font-bold text-slate-600 dark:text-slate-400">₹{basePrice.toLocaleString('en-IN')}</span>
              </div>
              <div className="mt-3 grid grid-cols-6 gap-2 sm:grid-cols-12">
                {Array.from({ length: 12 }, (_, i) => `S${i + 1}`).map((seatId) => (
                  <button
                    key={seatId}
                    type="button"
                    disabled={isSeatDisabled(seatId)}
                    onClick={() => onToggleSeat(seatId)}
                    className={`seat-btn h-9 ${getSeatClass(seatId)}`}
                    title={`${seatId} - ₹${basePrice.toLocaleString('en-IN')}`}
                  >
                    {seatId}
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* RENDER CONFERENCE HALL LAYOUT */}
        {activeLayout === 'CONFERENCE' && (
          <div className="space-y-7">
            {/* Keynote Presentation Screen */}
            <div className="rounded-2xl bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 p-6 text-center text-white shadow-md">
              <span className="inline-flex items-center gap-2 rounded-full bg-white/10 px-4 py-1 text-xs font-bold uppercase tracking-wider">
                <Presentation size={15} /> KEYNOTE STAGE & PRESENTATION SCREEN
              </span>
            </div>

            <div className="space-y-3">
              <div className="flex justify-between border-b border-slate-100 pb-2 dark:border-white/5">
                <span className="text-xs font-bold text-slate-500">EXECUTIVE & DELEGATE CLASSROOM SEATING</span>
                <span className="text-xs font-semibold text-slate-500 dark:text-slate-400">₹{basePrice.toLocaleString('en-IN')} per seat</span>
              </div>
              {['A', 'B', 'C', 'D', 'E'].map((row) => (
                <div key={row} className="flex items-center gap-3">
                  <span className="w-12 text-xs font-bold text-slate-500">ROW {row}</span>
                  <div className="grid flex-1 grid-cols-5 gap-2 sm:grid-cols-10">
                    {Array.from({ length: 10 }, (_, i) => `${row}${i + 1}`).map((seatId) => (
                      <button
                        key={seatId}
                        type="button"
                        disabled={isSeatDisabled(seatId)}
                        onClick={() => onToggleSeat(seatId)}
                        className={`seat-btn h-9 ${getSeatClass(seatId)}`}
                        title={`${seatId} - ₹${basePrice.toLocaleString('en-IN')}`}
                      >
                        {seatId.slice(1)}
                      </button>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Seat Legend */}
        <div className="mt-8 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-5 text-xs text-slate-600 dark:border-white/10 dark:text-slate-300">
          <div className="flex flex-wrap gap-4">
            <span className="flex items-center gap-1.5">
              <i className="h-3.5 w-3.5 rounded-md bg-sky-200 dark:bg-sky-950" /> Available
            </span>
            <span className="flex items-center gap-1.5">
              <i className="h-3.5 w-3.5 rounded-md bg-amber-200 dark:bg-amber-900" /> Premium Section
            </span>
            <span className="flex items-center gap-1.5">
              <i className="h-3.5 w-3.5 rounded-md bg-brand-600" /> Selected ({selectedSeats.length}/{maxSeats})
            </span>
            <span className="flex items-center gap-1.5">
              <i className="h-3.5 w-3.5 rounded-md bg-rose-500" /> Booked
            </span>
          </div>
          <span className="font-bold text-brand-600 dark:text-brand-400">
            Fixed Price: ₹{basePrice.toLocaleString('en-IN')} / seat
          </span>
        </div>
      </div>
    </div>
  );
}
