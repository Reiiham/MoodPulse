// ── CONFIG ────────────────────────────────────────────────────
const GATEWAY_URL = 'https://moodpulse-production.up.railway.app';

// ── Constants ─────────────────────────────────────────────────
const MOOD_META = {
    joy:     { emoji: '😄', color: '#f4d03f', label: 'Joy'     },
    sadness: { emoji: '😢', color: '#5dade2', label: 'Sadness' },
    anger:   { emoji: '😠', color: '#e74c3c', label: 'Anger'   },
    fear:    { emoji: '😨', color: '#8e44ad', label: 'Fear'    },
    love:    { emoji: '💖', color: '#e91e8c', label: 'Love'    },
};

const LOADING_MSGS = [
    'Fetching your top tracks...',
    'Reading the lyrics...',
    'Running emotion model...',
    'Fusing audio features...',
    'Crafting your mood report...',
];

// ── State ─────────────────────────────────────────────────────
let radarChart    = null;
let selectedPeriod = 'medium_term';

// ── Period selector ───────────────────────────────────────────
function selectPeriod(btn) {
    document.querySelectorAll('.period-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    selectedPeriod = btn.dataset.period;
}

// ── Spotify OAuth ─────────────────────────────────────────────
function connectSpotify() {
    window.location.href = `${GATEWAY_URL}/login?period=${selectedPeriod}`;
}

// ── On page load ──────────────────────────────────────────────
window.addEventListener('load', () => {
    const params = new URLSearchParams(window.location.search);
    const token  = params.get('access_token');
    const error  = params.get('error');
    const period = params.get('period') || 'medium_term';

    if (error) { alert('Spotify error: ' + error); return; }
    if (token)  { analyzeWithToken(token, period); }
});

// ── Loading ───────────────────────────────────────────────────
function showLoading() {
    document.getElementById('hero').style.display    = 'none';
    document.getElementById('how').style.display     = 'none';
    document.getElementById('results').style.display = 'none';
    document.getElementById('loading').style.display = 'flex';

    let i = 0;
    const el = document.getElementById('loading-text');
    const iv = setInterval(() => {
        el.style.opacity = 0;
        setTimeout(() => {
            el.textContent   = LOADING_MSGS[i % LOADING_MSGS.length];
            el.style.opacity = 1;
            i++;
        }, 300);
    }, 1800);
    return iv;
}

function hideLoading(iv) {
    clearInterval(iv);
    document.getElementById('loading').style.display = 'none';
}

// ── Analyze with real Spotify token ──────────────────────────
async function analyzeWithToken(accessToken, period = 'medium_term') {
    const iv = showLoading();
    try {
        const resp = await fetch(
            `${GATEWAY_URL}/api/mood/analyze?period=${period}`, {
            method: 'GET',
            headers: { 'Authorization': `Bearer ${accessToken}` }
        });

        const responseText = await resp.text();
        if (!resp.ok) throw new Error(`${resp.status}: ${responseText}`);

        const data = JSON.parse(responseText);
        hideLoading(iv);
        renderResults(data, period);

    } catch (err) {
        console.error('Error:', err.message);
        hideLoading(iv);
        alert('Error: ' + err.message);
        reset();
    }
}

// ── Demo ──────────────────────────────────────────────────────
function loadDemo() {
    const iv = showLoading();
    setTimeout(() => {
        hideLoading(iv);
        renderResults({
            dominant_mood:  'sadness',
            dominant_emoji: '😢',
            confidence:     0.61,
            summary:        'Your music taste leans melancholic these past 6 months. Sometimes sad songs are the most honest.',
            radar_scores: {
                joy: 0.08, sadness: 0.61, anger: 0.05,
                fear: 0.07, love: 0.14, surprise: 0.05
            },
            songs: [
                { title: 'The Night We Met',     artist: 'Lord Huron', emotion: 'sadness', emoji: '😢', confidence: 0.82, image: 'assets/TheNightWeMet.jpg' },
                { title: 'I Bet on Losing Dogs', artist: 'Mitski',     emotion: 'sadness', emoji: '😢', confidence: 0.74, image: 'assets/Mitski-IBetonLosingDogs.jpg' },
                { title: 'Jump',                 artist: 'BLACKPINK',  emotion: 'joy',     emoji: '😄', confidence: 0.79, image: 'assets/AlbumCover_BLACKPINK-JUMP.jpg' },
                { title: 'Nobody',               artist: 'Mitski',     emotion: 'sadness', emoji: '😢', confidence: 0.68, image: 'assets/Mitski-BeTheCowbow.jpg' },
                { title: 'Liability',            artist: 'Lorde',      emotion: 'sadness', emoji: '😢', confidence: 0.71, image: 'assets/Lorde-Melodrama.jpg' },
            ]
        }, 'medium_term');
    }, 3000);
}

// ── Render results ────────────────────────────────────────────
function renderResults(data, period = 'medium_term') {
    document.getElementById('hero').style.display    = 'none';
    document.getElementById('how').style.display     = 'none';
    document.getElementById('results').style.display = 'block';

    const meta = MOOD_META[data.dominant_mood] || MOOD_META.joy;
    const periodLabel = {
        'short_term':  'this month',
        'medium_term': 'these past 6 months',
        'long_term':   'all time'
    }[period] || 'recently';

    document.getElementById('mood-emoji').textContent      = meta.emoji;
    document.getElementById('mood-title').textContent      = `${periodLabel} you've been feeling ${meta.label}`;
    document.getElementById('mood-summary').textContent    = data.summary;
    document.getElementById('mood-confidence').textContent = `${Math.round(data.confidence * 100)}% confidence`;

    renderRadar(data.radar_scores);
    renderBars(data.radar_scores);
    renderSongs(data.songs);
}

// ── Radar chart ───────────────────────────────────────────────
function renderRadar(scores) {
    if (radarChart) { radarChart.destroy(); }

    const filteredScores = Object.fromEntries(
        Object.entries(scores).filter(([k]) => k !== 'surprise')
    );

    const labels = Object.keys(filteredScores).map(k => `${MOOD_META[k].emoji} ${MOOD_META[k].label}`);
    const values = Object.values(filteredScores).map(v => Math.round(v * 100));

    radarChart = new Chart(document.getElementById('radar-chart'), {
        type: 'radar',
        data: {
            labels,
            datasets: [{
                data: values,
                backgroundColor:      'rgba(165,148,249,0.2)',
                borderColor:          '#a594f9',
                borderWidth:          2.5,
                pointBackgroundColor: '#a594f9',
                pointRadius:          5,
                pointHoverRadius:     7,
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            scales: {
                r: {
                    min: 0, max: 100,
                    ticks:       { display: false },
                    grid:        { color: 'rgba(255,255,255,0.06)' },
                    pointLabels: { color: '#9b91b8', font: { size: 12, family: 'DM Sans' } },
                    angleLines:  { color: 'rgba(255,255,255,0.06)' },
                }
            },
            plugins: { legend: { display: false } }
        }
    });
}

// ── Emotion bars ──────────────────────────────────────────────
function renderBars(scores) {
    const container = document.getElementById('score-bars');
    container.innerHTML = '';

    const sorted = Object.entries(scores)
        .filter(([k]) => k !== 'surprise')
        .sort((a, b) => b[1] - a[1]);

    sorted.forEach(([label, score]) => {
        const meta = MOOD_META[label];
        const pct  = Math.round(score * 100);
        const row  = document.createElement('div');
        row.className = 'score-row';
        row.innerHTML = `
            <span class="score-emoji">${meta.emoji}</span>
            <span class="score-label">${meta.label}</span>
            <div class="score-track">
                <div class="score-fill" style="width:0%;background:${meta.color}" data-w="${pct}%"></div>
            </div>
            <span class="score-pct">${pct}%</span>
        `;
        container.appendChild(row);
    });

    setTimeout(() => {
        container.querySelectorAll('.score-fill').forEach(el => { el.style.width = el.dataset.w; });
    }, 100);
}

// ── Song cards ────────────────────────────────────────────────
function renderSongs(songs) {
    const grid = document.getElementById('songs-grid');
    grid.innerHTML = '';
    songs.forEach(song => {
        const meta = MOOD_META[song.emotion] || MOOD_META.joy;
        const card = document.createElement('div');
        card.className = 'song-card';
        card.innerHTML = `
            ${song.image
                ? `<img class="song-img" src="${song.image}" alt="${song.title}"
                     onerror="this.style.background='var(--surface2)';this.removeAttribute('src')">`
                : `<div class="song-img"></div>`}
            <div class="song-title" title="${song.title}">${song.title}</div>
            <div class="song-artist">${song.artist}</div>
            <span class="song-emotion">${meta.emoji} ${meta.label}</span>
        `;
        grid.appendChild(card);
    });
}

// ── Reset ─────────────────────────────────────────────────────
function reset() {
    document.getElementById('results').style.display = 'none';
    document.getElementById('hero').style.display    = 'flex';
    document.getElementById('how').style.display     = 'block';
    if (radarChart) { radarChart.destroy(); radarChart = null; }
}