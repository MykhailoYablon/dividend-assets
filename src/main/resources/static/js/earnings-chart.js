(function () {
    const canvas  = document.getElementById('earningsChart');
    const ctx     = canvas.getContext('2d');
    const tooltip = document.getElementById('tooltip');
    const title   = document.getElementById('chart-title');
    const hint    = document.getElementById('chart-hint');
    const backBtn = document.getElementById('back-btn');

    // ── theme
    const C = {
        green:    '#3db554',
        greenDim: 'rgba(61,181,84,0.15)',
        leaf:     '#2d5a27',
        sun:      '#f5a623',
        sunDim:   'rgba(245,166,35,0.15)',
        muted:    '#6b8f6e',
        border:   '#d4e8d0',
        text:     '#1a2e1f',
        grid:     'rgba(212,232,208,0.6)',
    };

    let currency    = 'uah';   // 'uah' | 'usd'
    let drillYear   = null;    // null = year view
    let hoveredIdx  = -1;
    let bars        = [];      // computed bar rects for hit-testing

    // ── data helpers
    function yearBars() {
        return YEARS_DATA
            .sort((a, b) => a.year - b.year)
            .map(y => ({
                label: String(y.year),
                value: currency === 'uah' ? y.total : y.usdTotal,
                sub:   currency === 'uah'
                    ? `${fmt(y.total)} UAH / ${fmt(y.usdTotal)} USD`
                    : `${fmt(y.usdTotal)} USD / ${fmt(y.total)} UAH`,
                count: y.count,
                year:  y.year,
        }));
    }

    function monthBars(yearData) {
        const MONTHS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
        return yearData.byMonth.map(m => ({
            label: MONTHS[monthIndex(m.month)],
            value: currency === 'uah' ? m.total : m.usdTotal,
            sub:   currency === 'uah'
                ? `${fmt(m.total)} UAH / ${fmt(m.usdTotal)} USD`
                : `${fmt(m.usdTotal)} USD / ${fmt(m.total)} UAH`,
            count: m.count,
        }));
    }

    function monthIndex(name) {
        const map = {JANUARY:0,FEBRUARY:1,MARCH:2,APRIL:3,MAY:4,JUNE:5,
                     JULY:6,AUGUST:7,SEPTEMBER:8,OCTOBER:9,NOVEMBER:10,DECEMBER:11};
        return map[name.toUpperCase()] ?? 0;
    }

    function fmt(n) {
        return Number(n).toLocaleString('uk-UA', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    function currentBars() {
        if (drillYear !== null) {
            const yd = YEARS_DATA.find(y => y.year === drillYear);
            return yd ? monthBars(yd) : [];
        }
        return yearBars();
    }

    // ── draw
    function draw() {
        const W = canvas.width  = canvas.offsetWidth  * devicePixelRatio;
        const H = canvas.height = canvas.offsetHeight * devicePixelRatio;
        const s = devicePixelRatio;
        ctx.scale(s, s);
        const w = canvas.offsetWidth;
        const h = canvas.offsetHeight;

        ctx.clearRect(0, 0, w, h);

        const data    = currentBars();
        if (!data.length) return;

        const PAD     = { top: 24, right: 20, bottom: 48, left: 88 };
        const plotW   = w - PAD.left - PAD.right;
        const plotH   = h - PAD.top  - PAD.bottom;

        const maxVal  = Math.max(...data.map(d => d.value), 1);
        const niceMax = niceNumber(maxVal);

        // grid lines
        const TICKS = 5;
        ctx.strokeStyle = C.grid;
        ctx.lineWidth   = 1;
        ctx.fillStyle   = C.muted;
        ctx.font        = `${11 * (1/devicePixelRatio) + 11}px 'Space Mono', monospace`;
        ctx.textAlign   = 'right';

        for (let i = 0; i <= TICKS; i++) {
            const val = (niceMax / TICKS) * i;
            const y   = PAD.top + plotH - (val / niceMax) * plotH;
            ctx.beginPath();
            ctx.moveTo(PAD.left, y); ctx.lineTo(PAD.left + plotW, y);
            ctx.stroke();
            ctx.fillText(shortNum(val), PAD.left - 8, y + 4);
        }

        // bars
        const gap     = plotW * 0.18 / (data.length + 1);
        const barW    = (plotW - gap * (data.length + 1)) / data.length;
        bars = [];

        data.forEach((d, i) => {
            const barH   = (d.value / niceMax) * plotH;
            const x      = PAD.left + gap + i * (barW + gap);
            const y      = PAD.top + plotH - barH;
            const hovered = hoveredIdx === i;

            // bar gradient
            const grad = ctx.createLinearGradient(x, y, x, y + barH);
            if (hovered) {
                grad.addColorStop(0, C.sun);
                grad.addColorStop(1, '#e07b00');
            } else {
                grad.addColorStop(0, C.green);
                grad.addColorStop(1, C.leaf);
            }

            // glow on hover
            if (hovered) {
                ctx.shadowColor = C.sun;
                ctx.shadowBlur  = 16;
            }

            // rounded top corners
            roundRect(ctx, x, y, barW, barH, [6, 6, 0, 0]);
            ctx.fillStyle = grad;
            ctx.fill();
            ctx.shadowBlur = 0;

            // value label on top
            if (barH > 24) {
                ctx.fillStyle  = hovered ? C.sun : C.green;
                ctx.textAlign  = 'center';
                ctx.font       = `bold 11px 'Outfit', sans-serif`;
                ctx.fillText(shortNum(d.value), x + barW / 2, y - 6);
            }

            // x label
            ctx.fillStyle = hovered ? C.text : C.muted;
            ctx.font      = `${hovered ? 'bold ' : ''}12px 'Outfit', sans-serif`;
            ctx.textAlign = 'center';
            ctx.fillText(d.label, x + barW / 2, PAD.top + plotH + 20);

            bars.push({ x, y, w: barW, h: barH, data: d });
        });

        // x axis line
        ctx.strokeStyle = C.border;
        ctx.lineWidth   = 1.5;
        ctx.beginPath();
        ctx.moveTo(PAD.left, PAD.top + plotH);
        ctx.lineTo(PAD.left + plotW, PAD.top + plotH);
        ctx.stroke();
    }

    function roundRect(ctx, x, y, w, h, radii) {
        const [tl, tr, br, bl] = radii;
        ctx.beginPath();
        ctx.moveTo(x + tl, y);
        ctx.lineTo(x + w - tr, y);
        ctx.quadraticCurveTo(x + w, y, x + w, y + tr);
        ctx.lineTo(x + w, y + h - br);
        ctx.quadraticCurveTo(x + w, y + h, x + w - br, y + h);
        ctx.lineTo(x + bl, y + h);
        ctx.quadraticCurveTo(x, y + h, x, y + h - bl);
        ctx.lineTo(x, y + tl);
        ctx.quadraticCurveTo(x, y, x + tl, y);
        ctx.closePath();
    }

    function niceNumber(val) {
        const exp   = Math.floor(Math.log10(val));
        const frac  = val / Math.pow(10, exp);
        const nice  = frac <= 1 ? 1 : frac <= 2 ? 2 : frac <= 5 ? 5 : 10;
        return nice * Math.pow(10, exp);
    }

    function shortNum(n) {
        if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + 'M';
        if (n >= 1_000)     return (n / 1_000).toFixed(1) + 'K';
        return n.toFixed(0);
    }

    // ── interactions
    canvas.addEventListener('mousemove', e => {
        const rect = canvas.getBoundingClientRect();
        const mx   = e.clientX - rect.left;
        const my   = e.clientY - rect.top;

        let found = -1;
        bars.forEach((b, i) => {
            if (mx >= b.x && mx <= b.x + b.w && my >= b.y && my <= b.y + b.h) found = i;
        });

        if (found !== hoveredIdx) {
            hoveredIdx = found;
            draw();
        }

        if (found >= 0) {
            const b   = bars[found];
            const cur = currency.toUpperCase();
            tooltip.innerHTML =
                `<strong>${b.data.label}</strong><br>${b.data.sub}<br>${b.data.count} transactions`;
            tooltip.style.opacity = '1';
            tooltip.style.left    = (b.x + b.w / 2 - tooltip.offsetWidth / 2) + 'px';
            tooltip.style.top     = (b.y - tooltip.offsetHeight - 12) + 'px';
        } else {
            tooltip.style.opacity = '0';
        }
    });

    canvas.addEventListener('mouseleave', () => {
        hoveredIdx = -1;
        tooltip.style.opacity = '0';
        draw();
    });

    canvas.addEventListener('click', e => {
        if (hoveredIdx < 0 || drillYear !== null) return;
        const b = bars[hoveredIdx];
        if (!b?.data?.year) return;
        drillYear = b.data.year;
        title.textContent = `${drillYear} — Month by Month`;
        hint.style.display   = 'none';
        backBtn.style.display = 'inline-flex';
        hoveredIdx = -1;
        draw();
    });

    // ── public controls
    window.switchCurrency = function(cur) {
        currency = cur;
        document.getElementById('btn-uah').classList.toggle('active', cur === 'uah');
        document.getElementById('btn-usd').classList.toggle('active', cur === 'usd');
        draw();
    };

    window.showYears = function() {
        drillYear = null;
        title.textContent    = 'Year by Year';
        hint.style.display   = 'block';
        backBtn.style.display = 'none';
        hoveredIdx = -1;
        draw();
    };

    // ── init
    const ro = new ResizeObserver(() => draw());
    ro.observe(canvas);
    draw();
 })();