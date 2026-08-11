(function () {
    function parseMaybe(value) {
        return Array.isArray(value) ? value : [];
    }

    function escapeHtml(text) {
        return String(text == null ? '' : text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function formatInr(amount) {
        try {
            return new Intl.NumberFormat('en-IN', {
                style: 'currency',
                currency: 'INR',
                maximumFractionDigits: 0
            }).format(Number(amount || 0));
        } catch (e) {
            return '₹' + amount;
        }
    }

    function setLoading(el, label) {
        el.classList.remove('text-muted-light');
        el.innerHTML = '<div class="comms-loading">' + escapeHtml(label || 'Generating with AI…') + '</div>';
    }

    function setError(el, message) {
        el.innerHTML = '<div class="text-danger">' + escapeHtml(message) + '</div>';
    }

    async function fetchJson(url) {
        const res = await fetch(url);
        if (!res.ok) {
            throw new Error('Request failed (' + res.status + ')');
        }
        return res.json();
    }

    function renderEmail(data) {
        return ''
            + '<div class="comms-meta"><span>Tone: ' + escapeHtml(data.tone) + '</span>'
            + '<span>Language: ' + escapeHtml(data.language) + '</span></div>'
            + '<div class="comms-block"><div class="comms-label">Subject</div>'
            + '<div class="comms-value">' + escapeHtml(data.subject) + '</div></div>'
            + '<div class="comms-block"><div class="comms-label">Body</div>'
            + '<pre class="comms-pre">' + escapeHtml(data.body) + '</pre></div>';
    }

    function renderCall(data) {
        return ''
            + '<div class="comms-meta"><span>Tone: ' + escapeHtml(data.tone) + '</span></div>'
            + '<div class="comms-block"><div class="comms-label">Opening</div>'
            + '<div class="comms-value">' + escapeHtml(data.opening) + '</div></div>'
            + '<div class="comms-block"><div class="comms-label">Full script</div>'
            + '<pre class="comms-pre">' + escapeHtml(data.fullScript) + '</pre></div>'
            + '<div class="comms-block"><div class="comms-label">Closing</div>'
            + '<div class="comms-value">' + escapeHtml(data.closing) + '</div></div>';
    }

    function renderPlans(data) {
        const options = Array.isArray(data.options) ? data.options : [];
        const cards = options.map(function (opt) {
            return ''
                + '<div class="plan-card' + (opt.recommended ? ' plan-best' : '') + '">'
                + (opt.recommended ? '<div class="plan-badge">Best recommendation</div>' : '')
                + '<div class="plan-months">' + escapeHtml(opt.months) + ' months</div>'
                + '<div class="plan-monthly">' + formatInr(opt.monthlyAmount) + '<span> / month</span></div>'
                + '<div class="plan-total">Total ' + formatInr(opt.totalAmount) + '</div>'
                + '<div class="plan-note">' + escapeHtml(opt.note || '') + '</div>'
                + '</div>';
        }).join('');
        return ''
            + '<p class="comms-value mb-3">' + escapeHtml(data.rationale || '') + '</p>'
            + '<div class="plan-grid">' + cards + '</div>';
    }

    function initAnalysisComms() {
        const page = window.analysisPage;
        if (!page || !page.policyId) {
            return;
        }
        const id = page.policyId;

        const emailBtn = document.getElementById('btnGenerateEmail');
        const callBtn = document.getElementById('btnGenerateCall');
        const planBtn = document.getElementById('btnGeneratePlans');
        const emailResult = document.getElementById('emailResult');
        const callResult = document.getElementById('callResult');
        const planResult = document.getElementById('planResult');

        if (emailBtn && emailResult) {
            emailBtn.addEventListener('click', async function () {
                emailBtn.disabled = true;
                setLoading(emailResult, 'Drafting email…');
                try {
                    const data = await fetchJson('/api/ai/policies/' + id + '/email');
                    emailResult.innerHTML = renderEmail(data);
                } catch (e) {
                    setError(emailResult, e.message || 'Failed to generate email');
                } finally {
                    emailBtn.disabled = false;
                }
            });
        }

        if (callBtn && callResult) {
            callBtn.addEventListener('click', async function () {
                callBtn.disabled = true;
                setLoading(callResult, 'Writing call script…');
                try {
                    const data = await fetchJson('/api/ai/policies/' + id + '/call-script');
                    callResult.innerHTML = renderCall(data);
                } catch (e) {
                    setError(callResult, e.message || 'Failed to generate call script');
                } finally {
                    callBtn.disabled = false;
                }
            });
        }

        if (planBtn && planResult) {
            planBtn.addEventListener('click', async function () {
                planBtn.disabled = true;
                setLoading(planResult, 'Building installment options…');
                try {
                    const data = await fetchJson('/api/ai/policies/' + id + '/payment-plans');
                    planResult.innerHTML = renderPlans(data);
                } catch (e) {
                    setError(planResult, e.message || 'Failed to generate payment plans');
                } finally {
                    planBtn.disabled = false;
                }
            });
        }
    }

    function heatColor(value, max) {
        const t = max <= 0 ? 0 : Math.min(1, value / max);
        // teal (cool) → amber → red (hot)
        if (t < 0.45) {
            return 'rgba(20, 184, 166,' + (0.25 + t * 0.7).toFixed(2) + ')';
        }
        if (t < 0.75) {
            return 'rgba(251, 191, 36,' + (0.35 + t * 0.5).toFixed(2) + ')';
        }
        return 'rgba(248, 113, 113,' + (0.45 + t * 0.5).toFixed(2) + ')';
    }

    function renderRegionHeatmap(labels, counts) {
        const host = document.getElementById('regionHeatmap');
        if (!host) {
            return;
        }
        const vals = parseMaybe(counts).map(Number);
        const names = parseMaybe(labels);
        if (!names.length) {
            host.innerHTML = '<div class="text-muted-light">No at-risk region data yet.</div>';
            return;
        }
        const max = Math.max.apply(null, vals.concat([1]));
        host.innerHTML = names.map(function (name, i) {
            const v = vals[i] || 0;
            return ''
                + '<div class="heatmap-cell" style="background:' + heatColor(v, max) + '" title="'
                + escapeHtml(name) + ': ' + v + ' at risk">'
                + '<div class="heatmap-name">' + escapeHtml(name) + '</div>'
                + '<div class="heatmap-count">' + v + '</div>'
                + '</div>';
        }).join('');
    }

    function initDashboardCharts() {
        if (!window.dashboardCharts) {
            return;
        }
        const data = window.dashboardCharts;
        const grid = 'rgba(157, 176, 195, 0.15)';
        const tick = '#9db0c3';

        renderRegionHeatmap(data.regionLabels, data.regionCounts);

        if (!window.Chart) {
            return;
        }

        const riskCtx = document.getElementById('riskChart');
        if (riskCtx) {
            const riskCounts = parseMaybe(data.riskCounts).map(Number);
            const totalRisk = riskCounts.reduce(function (a, b) { return a + b; }, 0);
            new Chart(riskCtx, {
                type: 'doughnut',
                data: {
                    labels: parseMaybe(data.riskLabels),
                    datasets: [{
                        data: riskCounts,
                        backgroundColor: ['#14b8a6', '#38bdf8', '#fbbf24', '#f87171'],
                        borderWidth: 0,
                        hoverOffset: 6
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    animation: { animateRotate: true, duration: 900 },
                    plugins: {
                        legend: {
                            position: 'bottom',
                            labels: { color: tick, boxWidth: 12, padding: 14 }
                        },
                        tooltip: {
                            callbacks: {
                                label: function (ctx) {
                                    const v = Number(ctx.raw || 0);
                                    const pct = totalRisk ? Math.round((v * 100) / totalRisk) : 0;
                                    return ' ' + ctx.label + ': ' + v + ' (' + pct + '%)';
                                }
                            }
                        }
                    },
                    cutout: '64%'
                }
            });
        }

        const trendCtx = document.getElementById('trendChart');
        if (trendCtx) {
            new Chart(trendCtx, {
                type: 'line',
                data: {
                    labels: parseMaybe(data.trendLabels),
                    datasets: [{
                        label: 'Collection %',
                        data: parseMaybe(data.trendValues),
                        borderColor: '#2dd4bf',
                        backgroundColor: 'rgba(45, 212, 191, 0.18)',
                        fill: true,
                        tension: 0.4,
                        pointRadius: 4,
                        pointBackgroundColor: '#5eead4',
                        pointBorderColor: '#0b1c2c',
                        pointBorderWidth: 2
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    animation: { duration: 900 },
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            callbacks: {
                                label: function (ctx) {
                                    return ' Collection rate: ' + ctx.raw + '%';
                                }
                            }
                        }
                    },
                    scales: {
                        x: { ticks: { color: tick }, grid: { color: grid } },
                        y: {
                            ticks: {
                                color: tick,
                                callback: function (v) { return v + '%'; }
                            },
                            grid: { color: grid },
                            suggestedMin: 80,
                            suggestedMax: 100
                        }
                    }
                }
            });
        }

        // Keep optional bar chart available (hidden) for fallback/debug
        const regionCtx = document.getElementById('regionChart');
        if (regionCtx && !regionCtx.classList.contains('d-none')) {
            new Chart(regionCtx, {
                type: 'bar',
                data: {
                    labels: parseMaybe(data.regionLabels),
                    datasets: [{
                        label: 'At-risk policies',
                        data: parseMaybe(data.regionCounts),
                        backgroundColor: '#2dd4bf',
                        borderRadius: 6
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: {
                        x: { ticks: { color: tick }, grid: { display: false } },
                        y: { ticks: { color: tick }, grid: { color: grid }, beginAtZero: true }
                    }
                }
            });
        }
    }

    function initChatWidget() {
        const fab = document.getElementById('chatFab');
        const panel = document.getElementById('chatPanel');
        const closeBtn = document.getElementById('chatClose');
        const form = document.getElementById('chatForm');
        const input = document.getElementById('chatInput');
        const messages = document.getElementById('chatMessages');
        if (!fab || !panel || !form || !input || !messages) {
            return;
        }

        function appendBubble(text, who) {
            const div = document.createElement('div');
            div.className = 'chat-bubble ' + who;
            div.textContent = text;
            messages.appendChild(div);
            messages.scrollTop = messages.scrollHeight;
        }

        fab.addEventListener('click', function () {
            panel.classList.toggle('d-none');
            if (!panel.classList.contains('d-none')) {
                input.focus();
            }
        });
        if (closeBtn) {
            closeBtn.addEventListener('click', function () {
                panel.classList.add('d-none');
            });
        }

        form.addEventListener('submit', async function (e) {
            e.preventDefault();
            const message = (input.value || '').trim();
            if (!message) {
                return;
            }
            appendBubble(message, 'user');
            input.value = '';
            appendBubble('Thinking…', 'bot');
            const thinking = messages.lastChild;
            try {
                const body = { message: message };
                if (window.analysisPage && window.analysisPage.policyId) {
                    body.policyId = window.analysisPage.policyId;
                }
                const res = await fetch('/api/chat', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(body)
                });
                if (!res.ok) {
                    throw new Error('Chat failed (' + res.status + ')');
                }
                const data = await res.json();
                thinking.textContent = data.answer || 'No response.';
            } catch (err) {
                thinking.textContent = err.message || 'Unable to reach AI assistant.';
            }
        });
    }

    function formatInrFull(amount) {
        try {
            return new Intl.NumberFormat('en-IN', {
                style: 'currency',
                currency: 'INR',
                maximumFractionDigits: 0
            }).format(Number(amount || 0));
        } catch (e) {
            return '₹' + amount;
        }
    }

    function animateScan(durationMs, onTick, onDone) {
        const start = performance.now();
        function frame(now) {
            const t = Math.min(1, (now - start) / durationMs);
            const eased = 1 - Math.pow(1 - t, 3);
            onTick(Math.round(eased * 100));
            if (t < 1) {
                requestAnimationFrame(frame);
            } else {
                onDone();
            }
        }
        requestAnimationFrame(frame);
    }

    function initSimulator() {
        const btn = document.getElementById('btnRunAi');
        if (!btn) {
            return;
        }
        const bar = document.getElementById('scanBar');
        const status = document.getElementById('scanStatus');
        const detail = document.getElementById('scanDetail');
        const findings = document.getElementById('findingsRow');
        const recPanel = document.getElementById('recPanel');
        const stream = document.getElementById('recStream');

        btn.addEventListener('click', async function () {
            btn.disabled = true;
            findings.classList.add('d-none');
            recPanel.classList.add('d-none');
            stream.innerHTML = '';
            status.textContent = 'Scanning…';
            detail.textContent = 'Scanning policies…';

            let result = null;
            const fetchPromise = fetch('/api/simulator/run', { method: 'POST' })
                .then(function (res) {
                    if (!res.ok) {
                        throw new Error('Simulator failed');
                    }
                    return res.json();
                })
                .then(function (data) {
                    result = data;
                });

            await new Promise(function (resolve) {
                animateScan(2200, function (pct) {
                    bar.style.width = pct + '%';
                    const scanned = result && result.totalScanned
                        ? Math.round((pct / 100) * result.totalScanned)
                        : pct;
                    detail.textContent = 'Scanning ' + scanned + ' policies… ████' + '█'.repeat(Math.floor(pct / 8));
                }, resolve);
            });

            try {
                await fetchPromise;
            } catch (e) {
                status.textContent = 'Failed';
                detail.textContent = e.message || 'Could not run simulator';
                btn.disabled = false;
                return;
            }

            status.textContent = 'Complete';
            detail.textContent = 'AI scanned ' + result.totalScanned + ' policies.';
            document.getElementById('statRisky').textContent = result.riskyCustomers;
            document.getElementById('statLeak').textContent = result.premiumLeakages;
            document.getElementById('statLapse').textContent = result.likelyToLapse;
            document.getElementById('statRecovery').textContent = formatInrFull(result.potentialRecovery);
            findings.classList.remove('d-none');
            recPanel.classList.remove('d-none');

            const recs = Array.isArray(result.recommendations) ? result.recommendations : [];
            recs.forEach(function (label, index) {
                const li = document.createElement('li');
                li.textContent = '✔ ' + label;
                stream.appendChild(li);
                setTimeout(function () {
                    li.classList.add('show');
                }, 350 * (index + 1));
            });
            btn.disabled = false;
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        initDashboardCharts();
        initAnalysisComms();
        initChatWidget();
        initSimulator();
    });
})();
