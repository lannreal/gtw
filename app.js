// ==========================================
// LK21 PREMIUM - SINGLE FILE HYBRID APP
// (Bunglon: Server & CLI jadi satu)
// 100% HEADLESS-FREE / ZERO-BROWSER ENGINE
// DYNAMIC AUTO-DETECT DOMAIN & HEALTH-HEALING
// UNIFIED PURE JSON REST API & CLI
// ==========================================
const cmd = process.argv[2];

if (cmd && cmd !== 'serve') {
    // ------------------------------------------
    // MODE 1: TERMINAL CLI (PELANGGAN)
    // ------------------------------------------
    const axios = require('axios');
    const url = 'http://localhost:3000/api';
    const query = process.argv.slice(3).join(' ');

    async function run() {
        try {
            let endpoint = '';
            if (cmd === 'home') endpoint = '/home';
            else if (cmd === 'trending' || cmd === 'populer') endpoint = '/trending';
            else if (cmd === 'series') endpoint = '/series';
            else if (cmd === 'status') endpoint = '/status';
            else if (cmd === 'search') endpoint = `/search?q=${encodeURIComponent(query)}`;
            else if (cmd === 'detail') endpoint = `/detail?url=${encodeURIComponent(query)}`;
            else if (cmd === 'extract') endpoint = `/extract?url=${encodeURIComponent(query)}`;
            else {
                console.log(JSON.stringify({
                    error: "Perintah tidak valid",
                    usage: "node app.js [home|trending|series|status|search|detail|extract] [parameter]",
                    examples: [
                        "node app.js search batman",
                        "node app.js detail /blue-beetle-2023",
                        "node app.js status",
                        "node app.js home",
                        "node app.js trending",
                        "node app.js series",
                        "node app.js extract https://videonode.de/iframe/cast/..."
                    ]
                }, null, 2));
                return;
            }

            const res = await axios.get(url + endpoint);
            console.log(JSON.stringify(res.data, null, 2));
        } catch (e) {
            if (e.response && e.response.data) {
                console.log(JSON.stringify(e.response.data, null, 2));
            } else {
                console.log(JSON.stringify({ 
                    success: false, 
                    error: "Gagal terhubung. Pastikan server aktif dengan 'node app.js serve'" 
                }, null, 2));
            }
        }
    }
    run();

} else {
    // ------------------------------------------
    // MODE 2: WEB SERVER (PELAYAN & REST API)
    // ------------------------------------------
    const express = require('express');
    const axios = require('axios');
    const cors = require('cors');
    const cheerio = require('cheerio');
    const path = require('path');
    const crypto = require('crypto');
    const { URL } = require('url');

    const app = express();
    const PORT = process.env.PORT || 3000;
    app.use(cors());
    app.use(express.static(path.join(__dirname, 'public')));

    // ==========================================
    // DYNAMIC DOMAIN AUTO-DETECTOR & HEALER
    // ==========================================
    class DomainManager {
        constructor() {
            this.activeBase = 'https://tv12.lk21official.cc';
            this.mirrors = [
                'https://tv12.lk21official.cc',
                'https://tv11.lk21official.cc',
                'https://tv10.lk21official.cc',
                'https://tv13.lk21official.cc',
                'https://tv14.lk21official.cc',
                'https://tv15.lk21official.cc',
                'https://lk21official.biz',
                'https://lk21official.co',
                'https://lk21official.site'
            ];
            this.userAgent = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36';
            this.isDetecting = false;
        }

        async testCandidate(candidateUrl) {
            try {
                const res = await axios.get(candidateUrl, {
                    headers: { 'User-Agent': this.userAgent, 'Accept': 'text/html,application/xhtml+xml' },
                    timeout: 4500,
                    maxRedirects: 5
                });

                if (res.status === 200 && typeof res.data === 'string') {
                    const finalUrl = res.request?.res?.responseUrl || candidateUrl;
                    const parsed = new URL(finalUrl);
                    const baseOrigin = `${parsed.protocol}//${parsed.host}`;
                    
                    if (res.data.includes('poster-title') || res.data.includes('grid-archive') || res.data.includes('item') || res.data.includes('lk21')) {
                        return baseOrigin;
                    }
                }
            } catch (e) {}
            return null;
        }

        async detectActiveDomain() {
            if (this.isDetecting) return this.activeBase;
            this.isDetecting = true;

            // 1. Cek domain aktif saat ini
            const currentWorking = await this.testCandidate(this.activeBase);
            if (currentWorking) {
                this.activeBase = currentWorking;
                this.isDetecting = false;
                return this.activeBase;
            }

            console.log('[DomainManager] 🔍 Mendeteksi mirror domain aktif baru...');

            // 2. Buat kandidat dinamis (tv10 s/d tv30)
            const candidates = [...this.mirrors];
            for (let i = 10; i <= 30; i++) {
                const mirrorUrl = `https://tv${i}.lk21official.cc`;
                if (!candidates.includes(mirrorUrl)) candidates.push(mirrorUrl);
            }

            // 3. Scan paralel untuk menemukan domain tercepat
            const promises = candidates.map(c => this.testCandidate(c));
            const results = await Promise.allSettled(promises);

            for (const r of results) {
                if (r.status === 'fulfilled' && r.value) {
                    this.activeBase = r.value;
                    this.isDetecting = false;
                    console.log(`[DomainManager] ✅ Domain aktif baru berhasil terpasang: ${this.activeBase}`);
                    return this.activeBase;
                }
            }

            this.isDetecting = false;
            return this.activeBase;
        }

        getBaseUrl() {
            return this.activeBase;
        }
    }

    const domainManager = new DomainManager();

    // Auto health check background setiap 15 menit
    setInterval(() => {
        domainManager.detectActiveDomain().catch(() => {});
    }, 15 * 60 * 1000);

    // In-memory Stream Sessions
    const streamSessions = new Map();

    function normalizeSlug(str) {
        if (!str) return '';
        return str.replace(/^\/+|\/+$/g, '').replace(/\.m3u8$/i, '').trim();
    }

    function saveSession(sessionData) {
        const slug = normalizeSlug(sessionData.slug);
        const server = (sessionData.server || 'cast').toLowerCase();
        
        sessionData.createdAt = Date.now();

        if (slug) {
            streamSessions.set(`${slug}?server=${server}`, sessionData);
            streamSessions.set(`${slug}/${server}`, sessionData);
            streamSessions.set(`${slug}-${server}`, sessionData);
            if (!streamSessions.has(slug) || server === 'cast') {
                streamSessions.set(slug, sessionData);
            }
        }

        if (sessionData.code) {
            streamSessions.set(sessionData.code, sessionData);
        }

        if (sessionData.id) {
            streamSessions.set(sessionData.id, sessionData);
        }

        // Auto cleanup > 24 jam
        if (streamSessions.size > 500) {
            const now = Date.now();
            for (const [k, v] of streamSessions.entries()) {
                if (now - v.createdAt > 24 * 3600 * 1000) {
                    streamSessions.delete(k);
                }
            }
        }

        return sessionData;
    }

    // ==== KONFIGURASI SCRAPER LK21 DENGAN SELF-HEALING ====
    const USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36';
    const CF_CLEARANCE_API = '5FCiBEn8.ACfagSDoiZM9dvxMaQu6WfIZq5ReSzU4Y4-1783865133-1.2.1.1-iAcNmr7CRzYzKAapOLdHjS3UjBv2FDKZRE8eJoWoPcdZREZQamOxzZU.S.ZpKWlzyUXDsxhPVF1OF7ySkV7q3FqxSuE411ufYxffuWklWsKXjAAPr.NUUvniJ2ekBaDVRcVC0syhvmA2oS3ORyMTVnsQaLJjmgrQ9KwhDRWhahAhjNuJL4YslOSGxBUUq2zsA1gQMdU2ZMsL3X7MdS4ljqQIoZPQdFFBiWMJjQVMKQrF.PVb3aNxA1myJCcDWowQbafwwTbuVrmuYz0mpMk6LpwkF8aQfV9x0Qwn4AUll9beI3V_ngD7Jd2VqrjQEAsfMwvZ3jY2Ufn7.ncq1Q47yQ';

    async function getCheerio(urlPath, retryCount = 0) {
        const currentBase = domainManager.getBaseUrl();
        const fullUrl = urlPath.startsWith('http') ? urlPath : `${currentBase}${urlPath.startsWith('/') ? '' : '/'}${urlPath}`;

        try {
            const res = await axios.get(fullUrl, {
                timeout: 12000,
                headers: {
                    "User-Agent": USER_AGENT,
                    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
                    "Referer": `${currentBase}/`
                },
                maxRedirects: 5
            });

            // Tangkap auto-redirect jika domain berganti saat request
            if (res.request?.res?.responseUrl) {
                const finalParsed = new URL(res.request.res.responseUrl);
                const finalOrigin = `${finalParsed.protocol}//${finalParsed.host}`;
                if (finalOrigin !== currentBase && finalOrigin.includes('lk21')) {
                    domainManager.activeBase = finalOrigin;
                }
            }

            return cheerio.load(res.data);
        } catch (err) {
            // Self-Healing: Jika domain error/ganti, scan domain baru dan coba lagi sekali
            if (retryCount === 0) {
                console.log(`[Domain-Watcher] Request gagal pada ${currentBase}, mencari domain aktif baru...`);
                await domainManager.detectActiveDomain();
                return await getCheerio(urlPath, retryCount + 1);
            }
            throw err;
        }
    }

    async function scrapeDetail(url) {
        const cleanPath = url.replace(/^https?:\/\/[^\/]+/, "");
        let $ = await getCheerio(cleanPath);
        
        // Handle TV Series Redirect (e.g. series.lk21.de)
        const redirectLink = $('#openNow').attr('href');
        if (redirectLink) {
            const redirectRes = await axios.get(redirectLink, {
                headers: { 'User-Agent': USER_AGENT }
            });
            $ = cheerio.load(redirectRes.data);
        }
        
        let title = $("h1").first().text().trim().replace(/Nonton\s/i, "").replace(/\sSub Indo di Lk21/i, "");
        
        if (!title || title.toLowerCase().includes("gratis di layarkaca21") || title.toLowerCase().includes("lk21")) {
            throw new Error("URL Slug tidak valid atau film tidak ditemukan.");
        }
        
        // Cover Poster yang akurat dari OpenGraph meta tag
        let poster = $('meta[property="og:image"]').attr('content') || 
                     $('meta[name="twitter:image"]').attr('content') || 
                     $('link[rel="image_src"]').attr('href') || 
                     $('img[itemprop="image"]').attr('src') || 
                     $('img[itemprop="image"]').attr('data-src') || '';
        if (poster && poster.startsWith('//')) poster = 'https:' + poster;

        let synopsis = $(".synopsis").attr("data-full");
        if (!synopsis) synopsis = $(".synopsis").text().trim();
        
        // Tag Info: Kualitas, Durasi, Batas Usia
        let quality = "HD";
        let duration = "-";
        let ageRating = "-";
        
        $('.info-tag span').each((_, el) => {
            const txt = $(el).text().trim();
            if (txt.match(/^\d{1,2}\+$/i) || ['SU', 'PG', 'PG-13', 'R', 'NC-17'].includes(txt.toUpperCase())) {
                ageRating = txt;
            } else if (txt.match(/^\d+h(?:\s*\d+m)?$/i) || txt.match(/^\d+\s*min/i)) {
                duration = txt;
            } else if (txt.match(/^(?:WEBDL|HD|HDCAM|CAM|BLURAY|DVDRIP|TS)$/i)) {
                quality = txt.toUpperCase();
            }
        });

        // Genres & Countries khusus film ini
        const genres = [];
        $('.tag-list a[href*="/genre/"]').each((_, el) => {
            const g = $(el).text().trim();
            if (g && !genres.includes(g)) genres.push(g);
        });

        const countries = [];
        $('.tag-list a[href*="/country/"]').each((_, el) => {
            const c = $(el).text().trim();
            if (c && !countries.includes(c)) countries.push(c);
        });

        // Directors
        const directors = [];
        $('a[href*="/director/"]').each((_, el) => {
            const d = $(el).text().trim();
            if (d && !directors.includes(d)) directors.push(d);
        });

        // Rating
        let rating = $('.rating span[itemprop="ratingValue"]').text().trim();
        if (!rating) rating = $('.rating').first().text().trim().replace(/[^0-9.]/g, '');
        if (!rating || rating === "0" || rating === "0.0") rating = "-";

        // Release Year
        let year = "-";
        const ym = title.match(/\((\d{4})\)/);
        if (ym) year = ym[1];
        if (year === "-") {
            const ymUrl = url.match(/-(\d{4})(?:$|\/)/);
            if (ymUrl) year = ymUrl[1];
        }

        const streams = {};
        $("a[data-server], a[data-url], .server-item a, #load-server a, .tab-content a, .server-list a").each((_, el) => {
            let serverName = $(el).attr("data-server");
            let serverUrl = $(el).attr("data-url") || $(el).attr("href");
            
            if (!serverName && serverUrl) {
                if (serverUrl.includes('/cast/')) serverName = 'cast';
                else if (serverUrl.includes('/turbovip/')) serverName = 'turbovip';
                else if (serverUrl.includes('/p2p/')) serverName = 'p2p';
                else if (serverUrl.includes('/hydrax/')) serverName = 'hydrax';
                else {
                    const text = $(el).text().trim().toLowerCase();
                    if (text && !text.includes('nonton') && !text.includes('download')) {
                        serverName = text;
                    }
                }
            }

            if (serverName && serverUrl && serverUrl.startsWith('http')) {
                const sKey = serverName.toLowerCase().trim();
                if (!streams[sKey]) {
                    streams[sKey] = serverUrl;
                }
            }
        });

        // Deteksi iframe default utama jika belum terdaftar
        const mainIframe = $('iframe').attr('src') || $('iframe').attr('data-src');
        if (mainIframe && mainIframe.startsWith('http')) {
            let defServer = 'p2p';
            if (mainIframe.includes('/cast/')) defServer = 'cast';
            else if (mainIframe.includes('/turbovip/')) defServer = 'turbovip';
            else if (mainIframe.includes('/hydrax/')) defServer = 'hydrax';
            if (!streams[defServer]) {
                streams[defServer] = mainIframe;
            }
        }

        const episodes = [];
        const seasonDataElem = $('#season-data');
        if (seasonDataElem.length > 0) {
            try {
                const seasonData = JSON.parse(seasonDataElem.html());
                for (const seasonKey of Object.keys(seasonData)) {
                    for (const ep of seasonData[seasonKey]) {
                        episodes.push({ 
                            url: '/' + ep.slug, 
                            title: ep.title,
                            season: seasonKey
                        });
                    }
                }
            } catch (e) {}
        } else {
            $('.col-episode a, .episode-list a, .list-episode a, a[href*="episode"]').each((_, el) => {
                let epUrl = $(el).attr('href');
                let epText = $(el).text().trim();
                if (epUrl && epText && !epText.toLowerCase().includes("play terbaru") && !epText.toLowerCase().includes("play awal")) {
                    if (!episodes.some(e => e.url === epUrl)) {
                        epUrl = epUrl.replace(/^https?:\/\/[^\/]+/, '');
                        let seasonMatch = epText.match(/Season (\d+)/i) || epUrl.match(/season-(\d+)/i);
                        let season = seasonMatch ? seasonMatch[1] : '1';
                        episodes.push({ url: epUrl, title: epText, season: season });
                    }
                }
            });
        }

        return { 
            title, 
            year,
            poster, 
            rating,
            quality,
            duration,
            age_rating: ageRating,
            genres,
            countries,
            directors,
            synopsis, 
            streams, 
            episodes 
        };
    }

    async function getResolutionsFromM3U8(m3u8Url, referer = '') {
        try {
            const res = await axios.get(m3u8Url, {
                headers: { 'Referer': referer, 'User-Agent': USER_AGENT },
                timeout: 8000
            });
            const matches = res.data.match(/RESOLUTION=\d+x(\d+)/g);
            if (matches) {
                const resSet = new Set();
                matches.forEach(m => {
                    const height = m.split('x')[1];
                    resSet.add(height + 'p');
                });
                return Array.from(resSet).sort((a, b) => parseInt(a) - parseInt(b));
            }
            
            if (res.data.includes('.ts') || res.data.includes('#EXTINF')) {
                const heightMatch = m3u8Url.match(/\/(\d{3,4})\.m3u8/);
                if (heightMatch) {
                    const h = heightMatch[1];
                    if (h === '480' || h === '720') return ['480p', '720p', '1080p'];
                    return [h + 'p'];
                }
                return ['480p', '720p', '1080p'];
            }
        } catch (e) {}
        return ['480p', '720p', '1080p'];
    }

    // ==========================================
    // ZERO-BROWSER REVERSE-ENGINEERED EXTRACTORS
    // ==========================================

    function bufferToBase64Url(buf) {
        return Buffer.from(buf).toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    }

    function base64UrlToBuffer(str) {
        const base64 = str.replace(/-/g, '+').replace(/_/g, '/');
        const padded = base64.padEnd(base64.length + (4 - base64.length % 4) % 4, '=');
        return Buffer.from(padded, 'base64');
    }

    // Proof-of-Work (PoW) Solver for Cast / Byse Engine (gn1r5n.org)
    const be = 512, lt = be - 1, dr = 2, lr = 2654435761, hr = 2246822519;
    const re = (t, e) => (t << e | t >>> 32 - e) >>> 0;
    const ht = (t, e) => Math.imul(t, e) >>> 0;

    function ye(t) {
        t[0] = t[0] + t[1] >>> 0; t[3] = re(t[3] ^ t[0], 16);
        t[2] = t[2] + t[3] >>> 0; t[1] = re(t[1] ^ t[2], 12);
        t[0] = t[0] + t[1] >>> 0; t[3] = re(t[3] ^ t[0], 8);
        t[2] = t[2] + t[3] >>> 0; t[1] = re(t[1] ^ t[2], 7);
    }

    function gr(t) {
        const e = new Uint32Array([1779033703, 3144134277, 1013904242, 2773480762]);
        for (let i = 0; i < t.length; i++) { e[0] = e[0] + t[i] >>> 0; e[0] = re(e[0], 7); ye(e); }
        for (let i = 0; i < 8; i++) ye(e);
        const r = new Uint32Array(be);
        for (let i = 0; i < be; i++) { ye(e); r[i] = (e[0] ^ e[2]) >>> 0; }
        for (let i = 0; i < dr; i++) {
            for (let s = 0; s < be; s++) {
                const a = r[s] & lt;
                let c = r[s] + r[a] >>> 0;
                c = re(c, 13);
                c = (c ^ ht(r[s + 1 & lt], lr)) >>> 0;
                r[s] = c; e[0] = (e[0] ^ c) >>> 0; ye(e);
            }
        }
        const n = new Uint32Array(8);
        const o = be / 8;
        for (let i = 0; i < 8; i++) {
            ye(e); let s = e[0]; const a = i * o;
            for (let c = 0; c < o; c++) { const d = r[a + c]; s = s + d >>> 0; s = re(s, 5); s = (s ^ ht(d, hr)) >>> 0; }
            n[i] = (s ^ e[2]) >>> 0;
        }
        return n;
    }

    function wr(t) {
        let e = 0;
        for (let r = 0; r < t.length; r++) {
            const n = t[r];
            if (n === 0) { e += 32; continue; }
            return e + Math.clz32(n);
        }
        return e;
    }

    function yr(t) {
        const e = new Uint8Array(t.length);
        for (let r = 0; r < t.length; r++) e[r] = t.charCodeAt(r) & 255;
        return e;
    }

    function solvePoW(nonce, difficulty, timeoutMs = 20000) {
        if (difficulty <= 0) return "0";
        const prefix = nonce + ":";
        const start = Date.now();
        let s = 0;
        while (true) {
            for (let c = 0; c < 1024; c++) {
                const d = gr(yr(prefix + s));
                if (wr(d) >= difficulty) return String(s);
                s++;
            }
            if (Date.now() - start > timeoutMs) return null;
        }
    }

    // Dynamic AES-256-GCM Decryption for Playback Config
    function getQa() {
        const e = {};
        for (let n = 1; n <= 20; n += 1) { e[String(n)] = [n ^ 0, (31 - n) ^ 0]; }
        return e;
    }

    function getEa(version, keyPartsLen) {
        const r = typeof version === 'string' ? version.trim() : '';
        const o = getQa()[r];
        if (!o) return [];
        const [a, i] = o;
        if (a < 1 || i < 1 || a > keyPartsLen || i > keyPartsLen) return [];
        return [a, i];
    }

    function ws(payload) {
        const t = Array.isArray(payload.key_parts) ? payload.key_parts : [];
        const r = getEa(payload.version, t.length);
        if (r.length === 0) return t;
        const n = r
            .map(o => Number(o))
            .filter(o => Number.isInteger(o) && o >= 1 && o <= t.length)
            .map(o => t[o - 1])
            .filter(o => typeof o === 'string' && o.length > 0);
        return n.length > 0 ? n : t;
    }

    function decryptPlaybackPayload(playback) {
        if (!playback || !Array.isArray(playback.key_parts) || playback.key_parts.length === 0) {
            throw new Error('Invalid encrypted payload');
        }
        const key = Buffer.concat(ws(playback).map(base64UrlToBuffer));
        const iv = base64UrlToBuffer(playback.iv);
        const encryptedData = base64UrlToBuffer(playback.payload);

        const ciphertext = encryptedData.subarray(0, encryptedData.length - 16);
        const authTag = encryptedData.subarray(encryptedData.length - 16);

        const decipher = crypto.createDecipheriv('aes-256-gcm', key, iv);
        decipher.setAuthTag(authTag);
        let decrypted = decipher.update(ciphertext, null, 'utf8');
        decrypted += decipher.final('utf8');
        return JSON.parse(decrypted);
    }

    // WebCrypto Device Attestation
    async function generateDeviceAttestation(baseUrl = 'https://gn1r5n.org') {
        const commonHeaders = {
            'User-Agent': USER_AGENT,
            'Referer': `${baseUrl}/`,
            'Origin': baseUrl,
            'Content-Type': 'application/json'
        };

        const keyPair = await crypto.webcrypto.subtle.generateKey(
            { name: 'ECDSA', namedCurve: 'P-256' },
            true,
            ['sign', 'verify']
        );
        const publicJwk = await crypto.webcrypto.subtle.exportKey('jwk', keyPair.publicKey);

        const challengeRes = await axios.post(`${baseUrl}/api/videos/access/challenge`, {}, { headers: commonHeaders, timeout: 10000 });
        const { challenge_id, nonce } = challengeRes.data;

        const signatureBuffer = await crypto.webcrypto.subtle.sign(
            { name: 'ECDSA', hash: { name: 'SHA-256' } },
            keyPair.privateKey,
            new TextEncoder().encode(nonce)
        );
        const signature = bufferToBase64Url(signatureBuffer);

        const client = {
            user_agent: USER_AGENT,
            pixel_ratio: 1,
            screen_width: 1920,
            screen_height: 1080,
            color_depth: 24,
            languages: ['en-US', 'en', 'id'],
            platform: 'Win32',
            hardware_concurrency: 8,
            device_memory: 8,
            touch_support: false
        };

        const attestPayload = {
            viewer_id: '',
            device_id: '',
            challenge_id,
            nonce,
            signature,
            public_key: publicJwk,
            client,
            storage: {},
            attributes: { entropy: 'user_agent,languages,platform,screen,color_depth,hardware_concurrency,device_memory' }
        };

        const attestRes = await axios.post(`${baseUrl}/api/videos/access/attest`, attestPayload, { headers: commonHeaders, timeout: 10000 });

        return {
            token: attestRes.data.token,
            viewer_id: attestRes.data.viewer_id,
            device_id: attestRes.data.device_id,
            confidence: attestRes.data.confidence
        };
    }

    // 1. Ekstraktor CAST (gn1r5n.org / Byse Engine)
    async function extractCastStream(wrapperUrl, slug = '', serverName = 'cast', title = '') {
        const code = wrapperUrl.split('/').pop();
        const baseUrl = 'https://gn1r5n.org';
        const commonHeaders = {
            'User-Agent': USER_AGENT,
            'Referer': `${baseUrl}/e/${code}`,
            'Origin': baseUrl,
            'X-Embed-Origin': 'videonode.de',
            'X-Embed-Referer': `https://videonode.de/iframe/cast/${code}`,
            'X-Embed-Parent': `https://gn1r5n.org/e/${code}`,
            'Content-Type': 'application/json'
        };

        const fingerprint = await generateDeviceAttestation(baseUrl);
        const settingsRes = await axios.get(`${baseUrl}/api/videos/${code}/embed/settings`, { headers: commonHeaders, timeout: 10000 });
        const settings = settingsRes.data;

        let captchaToken = null;
        if (settings.captcha_required) {
            const captchaRes = await axios.post(
                `${baseUrl}/api/videos/${code}/embed/captcha`,
                { fingerprint },
                { headers: commonHeaders, timeout: 10000 }
            );
            const { pow_nonce, pow_difficulty, pow_token } = captchaRes.data;
            const solution = solvePoW(pow_nonce, pow_difficulty);

            const verifyRes = await axios.post(
                `${baseUrl}/api/videos/${code}/embed/captcha/verify`,
                { pow_token, solution, fingerprint },
                { headers: commonHeaders, timeout: 10000 }
            );
            captchaToken = verifyRes.data.token;
        }

        const playbackHeaders = { ...commonHeaders };
        if (captchaToken) {
            playbackHeaders['X-Captcha-Token'] = captchaToken;
        }

        const playbackRes = await axios.post(
            `${baseUrl}/api/videos/${code}/embed/playback`,
            { fingerprint },
            { headers: playbackHeaders, timeout: 10000 }
        );

        let decrypted = playbackRes.data;
        if (playbackRes.data.playback) {
            decrypted = decryptPlaybackPayload(playbackRes.data.playback);
        }

        const sources = decrypted.sources || [];
        if (sources.length > 0) {
            const rawUrl = sources[sources.length - 1].url || sources[0].url;
            const resolutions = sources.map(s => s.label || `${s.height}p`);
            
            const cleanSlug = normalizeSlug(slug) || code;
            const playPath = `/play/${cleanSlug}`;
            const streamPath = `/stream/${cleanSlug}`;

            saveSession({
                type: 'm3u8',
                slug: cleanSlug,
                server: serverName,
                code: code,
                raw_url: rawUrl,
                resolutions: resolutions,
                referer: 'https://gn1r5n.org/',
                title: title
            });

            return { 
                success: true, 
                slug: cleanSlug,
                server: serverName,
                play_url: `http://localhost:${PORT}${playPath}`,
                stream_url: `http://localhost:${PORT}${streamPath}`,
                raw_url: rawUrl, 
                resolutions, 
                sources 
            };
        }
        return { success: false, message: 'Gagal menemukan source video pada Cast server' };
    }

    // 2. Ekstraktor P2P (playcdn.de / cloud.hownetwork.xyz)
    async function extractP2PStream(wrapperUrl, slug = '', serverName = 'p2p', title = '') {
        const headers = { 'User-Agent': USER_AGENT };
        let id = wrapperUrl.split('/').pop();
        let host = 'playcdn.de';
        let referer = 'https://videonode.de/';

        try {
            const pageRes = await axios.get(wrapperUrl, { headers: { ...headers, 'Referer': domainManager.getBaseUrl() + '/' }, timeout: 8000 });
            const iframeMatch = pageRes.data.match(/<iframe.*?src=["'](.*?)["']/i);
            if (iframeMatch && iframeMatch[1]) {
                const nestedUrl = new URL(iframeMatch[1]);
                host = nestedUrl.hostname;
                const queryId = nestedUrl.searchParams.get('id');
                if (queryId) id = queryId;
                referer = wrapperUrl;
            }
        } catch (e) {}

        const postData = `r=${encodeURIComponent(referer)}&d=${host}`;
        const apiRes = await axios.post(
            `https://${host}/api2.php?id=${id}`,
            postData,
            {
                headers: {
                    ...headers,
                    'Referer': `https://${host}/video.php?id=${id}`,
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                timeout: 10000
            }
        );

        if (apiRes.data && apiRes.data.file) {
            let rawUrl = apiRes.data.file;
            if (!rawUrl.startsWith('http')) {
                rawUrl = `https://${host}${rawUrl}`;
            }
            const resolutions = await getResolutionsFromM3U8(rawUrl, `https://${host}/`);
            
            const cleanSlug = normalizeSlug(slug) || id;
            const playPath = `/play/${cleanSlug}?server=p2p`;
            const streamPath = `/stream/${cleanSlug}?server=p2p`;

            saveSession({
                type: 'm3u8',
                slug: cleanSlug,
                server: serverName,
                id: id,
                raw_url: rawUrl,
                resolutions: resolutions,
                referer: `https://${host}/`,
                title: title || apiRes.data.title,
                poster: apiRes.data.poster
            });

            return { 
                success: true, 
                slug: cleanSlug,
                server: serverName,
                play_url: `http://localhost:${PORT}${playPath}`,
                stream_url: `http://localhost:${PORT}${streamPath}`,
                raw_url: rawUrl, 
                resolutions, 
                title: apiRes.data.title, 
                poster: apiRes.data.poster 
            };
        }
        return { success: false, message: 'Gagal mendapatkan file dari API P2P' };
    }

    // 3. Ekstraktor TURBOVIP (emturbovid.com / turboviplay.com)
    async function extractTurbovipStream(wrapperUrl, slug = '', serverName = 'turbovip', title = '') {
        const headers = { 'User-Agent': USER_AGENT };
        const code = wrapperUrl.split('/').pop();
        const targetUrls = [wrapperUrl];
        if (!wrapperUrl.includes('emturbovid.com')) {
            targetUrls.push(`https://emturbovid.com/t/${code}`);
        }

        for (const tUrl of targetUrls) {
            try {
                const r1 = await axios.get(tUrl, {
                    headers: { ...headers, 'Referer': domainManager.getBaseUrl() + '/' },
                    timeout: 8000
                });

                let rawUrl = null;
                const directMatch = r1.data.match(/urlPlay\s*=\s*['"](.*?)['"]/);
                if (directMatch && directMatch[1]) {
                    rawUrl = directMatch[1];
                } else {
                    const iframeMatch = r1.data.match(/<iframe.*?src=["'](.*?)["']/i);
                    if (iframeMatch && iframeMatch[1]) {
                        const nested = iframeMatch[1].startsWith('http') ? iframeMatch[1] : `https://emturbovid.com${iframeMatch[1]}`;
                        const r2 = await axios.get(nested, {
                            headers: { ...headers, 'Referer': 'https://videonode.de/' },
                            timeout: 8000
                        });
                        const urlMatch = r2.data.match(/urlPlay\s*=\s*['"](.*?)['"]/);
                        if (urlMatch && urlMatch[1]) {
                            rawUrl = urlMatch[1];
                        }
                    }
                }

                if (rawUrl) {
                    const resolutions = await getResolutionsFromM3U8(rawUrl, 'https://emturbovid.com/');
                    const cleanSlug = normalizeSlug(slug) || code;
                    const playPath = `/play/${cleanSlug}?server=turbovip`;
                    const streamPath = `/stream/${cleanSlug}?server=turbovip`;

                    saveSession({
                        type: 'm3u8',
                        slug: cleanSlug,
                        server: serverName,
                        raw_url: rawUrl,
                        resolutions: resolutions.length ? resolutions : ['480p', '720p', '1080p'],
                        referer: 'https://emturbovid.com/',
                        title: title
                    });

                    return { 
                        success: true, 
                        slug: cleanSlug,
                        server: serverName,
                        play_url: `http://localhost:${PORT}${playPath}`,
                        stream_url: `http://localhost:${PORT}${streamPath}`,
                        raw_url: rawUrl, 
                        resolutions: resolutions.length ? resolutions : ['480p', '720p', '1080p']
                    };
                }
            } catch (e) {}
        }
        return { success: false, message: 'Gagal mengekstrak stream Turbovip' };
    }

    // 4. Ekstraktor HYDRAX (abyssplayer.com)
    async function extractHydraxStream(wrapperUrl, slug = '', serverName = 'hydrax', title = '') {
        const headers = { 'User-Agent': USER_AGENT };
        const r = await axios.get(wrapperUrl, {
            headers: { ...headers, 'Referer': domainManager.getBaseUrl() + '/' },
            timeout: 8000
        });
        const match = r.data.match(/<iframe.*?src=["'](.*?)["']/i);
        if (match && match[1]) {
            const iframeUrl = match[1];
            const cleanSlug = normalizeSlug(slug) || wrapperUrl.split('/').pop();
            const playPath = `/play/${cleanSlug}?server=hydrax`;

            saveSession({
                type: 'iframe',
                slug: cleanSlug,
                server: serverName,
                iframe: iframeUrl,
                resolutions: ['Auto (Hydrax Native)'],
                title: title
            });

            return { 
                success: true, 
                slug: cleanSlug,
                server: serverName,
                play_url: `http://localhost:${PORT}${playPath}`,
                iframe: iframeUrl, 
                resolutions: ['Auto (Hydrax Native)'] 
            };
        }
        return { success: false, message: 'Gagal mengekstrak iframe Hydrax' };
    }

    // Master Dispatcher (100% Zero Headless Browser with Auto-Retry)
    async function extractStream(wrapperUrl, slug = '', serverName = '', title = '', retry = 0) {
        try {
            if (wrapperUrl.includes('/iframe/cast/') || wrapperUrl.includes('/e/')) {
                return await extractCastStream(wrapperUrl, slug, serverName || 'cast', title);
            } else if (wrapperUrl.includes('/turbovip/')) {
                return await extractTurbovipStream(wrapperUrl, slug, serverName || 'turbovip', title);
            } else if (wrapperUrl.includes('/iframe/hydrax/')) {
                return await extractHydraxStream(wrapperUrl, slug, serverName || 'hydrax', title);
            } else {
                return await extractP2PStream(wrapperUrl, slug, serverName || 'p2p', title);
            }
        } catch (err) {
            if (retry < 1) {
                return await extractStream(wrapperUrl, slug, serverName, title, retry + 1);
            }
            throw err;
        }
    }

    // Dynamic On-Demand Session Resolver
    async function getOrExtractMovieStream(slug, requestedServer = '') {
        let cleanSlug = normalizeSlug(slug);
        let server = (requestedServer || '').toLowerCase();

        if (cleanSlug.includes('/')) {
            const parts = cleanSlug.split('/');
            cleanSlug = parts[0];
            if (!server) server = parts[1];
        }

        const sessionKey = server ? `${cleanSlug}?server=${server}` : cleanSlug;
        if (streamSessions.has(sessionKey)) {
            return streamSessions.get(sessionKey);
        }
        if (streamSessions.has(cleanSlug)) {
            return streamSessions.get(cleanSlug);
        }

        try {
            const detail = await scrapeDetail('/' + cleanSlug);
            const streams = detail.streams || {};
            
            if (Object.keys(streams).length > 0) {
                let targetServer = server;
                if (!targetServer || !streams[targetServer]) {
                    if (streams['cast']) targetServer = 'cast';
                    else if (streams['p2p']) targetServer = 'p2p';
                    else if (streams['turbovip']) targetServer = 'turbovip';
                    else targetServer = Object.keys(streams)[0];
                }

                if (streams[targetServer]) {
                    const extResult = await extractStream(streams[targetServer], cleanSlug, targetServer, detail.title);
                    if (extResult && extResult.success) {
                        return streamSessions.get(cleanSlug) || extResult;
                    }
                }
            }
        } catch (e) {}

        try {
            const castRes = await extractCastStream(`https://gn1r5n.org/e/${cleanSlug}`, cleanSlug, 'cast');
            if (castRes && castRes.success) {
                return castRes;
            }
        } catch (e) {}

        return null;
    }

    function getStreamReferer(targetUrl) {
        if (targetUrl.includes('playcdn.de')) return 'https://playcdn.de/';
        if (targetUrl.includes('cloud.hownetwork.xyz')) return 'https://cloud.hownetwork.xyz/';
        if (targetUrl.includes('turboviplay.com') || targetUrl.includes('turbosplayer.com') || targetUrl.includes('emturbovid.com')) return 'https://emturbovid.com/';
        if (targetUrl.includes('sprintcdn') || targetUrl.includes('owphbf24.com') || targetUrl.includes('r66nv9ed.com') || targetUrl.includes('gn1r5n.org')) return 'https://gn1r5n.org/';
        try {
            const u = new URL(targetUrl);
            return `${u.protocol}//${u.host}/`;
        } catch (e) {
            return domainManager.getBaseUrl() + '/';
        }
    }

    async function handleProxyStream(targetUrl, req, res) {
        const referer = getStreamReferer(targetUrl);

        try {
            const reqHeaders = {
                'Referer': referer,
                'User-Agent': USER_AGENT
            };
            if (req.headers.range) {
                reqHeaders['Range'] = req.headers.range;
            }

            if (targetUrl.includes('.m3u8')) {
                const response = await axios.get(targetUrl, {
                    headers: reqHeaders,
                    timeout: 12000
                });
                
                const parsedUrl = new URL(targetUrl);
                const baseUrl = targetUrl.substring(0, targetUrl.lastIndexOf('/') + 1);

                const rewritten = response.data.split('\n').map(line => {
                    const trimmed = line.trim();
                    if (!trimmed) return line;

                    if (trimmed.startsWith('#EXT-X-KEY') || trimmed.startsWith('#EXT-X-MAP')) {
                        return line.replace(/URI=["']([^"']+)["']/g, (match, uri) => {
                            let absoluteUri = uri;
                            if (uri.startsWith('http://') || uri.startsWith('https://')) {
                                absoluteUri = uri;
                            } else if (uri.startsWith('/')) {
                                absoluteUri = parsedUrl.origin + uri;
                            } else {
                                absoluteUri = baseUrl + uri;
                            }
                            return `URI="/proxy-stream?url=${encodeURIComponent(absoluteUri)}"`;
                        });
                    }

                    if (!trimmed.startsWith('#')) {
                        let absoluteUrl = trimmed;
                        if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
                            absoluteUrl = trimmed;
                        } else if (trimmed.startsWith('/')) {
                            absoluteUrl = parsedUrl.origin + trimmed;
                        } else {
                            absoluteUrl = baseUrl + trimmed;
                        }
                        return '/proxy-stream?url=' + encodeURIComponent(absoluteUrl);
                    }
                    return line;
                }).join('\n');

                res.setHeader('Content-Type', 'application/vnd.apple.mpegurl');
                return res.send(rewritten);
            }

            const response = await axios.get(targetUrl, {
                responseType: 'stream',
                headers: reqHeaders,
                timeout: 20000
            });

            if (response.headers['content-type'] === 'image/png' || targetUrl.includes('turboviplay.com')) {
                res.setHeader('Content-Type', 'video/MP2T');
                const { Transform } = require('stream');
                let bytesStripped = 0;
                const PNG_HEADER_SIZE = 941;

                const stripPngHeader = new Transform({
                    transform(chunk, encoding, callback) {
                        if (bytesStripped < PNG_HEADER_SIZE) {
                            const remainingToStrip = PNG_HEADER_SIZE - bytesStripped;
                            if (chunk.length <= remainingToStrip) {
                                bytesStripped += chunk.length;
                                callback();
                            } else {
                                bytesStripped += remainingToStrip;
                                this.push(chunk.slice(remainingToStrip));
                                callback();
                            }
                        } else {
                            this.push(chunk);
                            callback();
                        }
                    }
                });

                response.data.pipe(stripPngHeader).pipe(res);
            } else {
                if (response.headers['content-type']) {
                    res.setHeader('Content-Type', response.headers['content-type']);
                }
                if (response.headers['content-range']) {
                    res.setHeader('Content-Range', response.headers['content-range']);
                }
                if (response.status === 206) {
                    res.status(206);
                }
                response.data.pipe(res);
            }
        } catch (error) {
            console.error('[Stream Proxy Error]:', error.message);
            res.status(500).end();
        }
    }

    // ==== REST API ENDPOINTS ====

    async function scrapeMovieList(path) {
        const $ = await getCheerio(path);
        const movies = [];
        const items = $('article, .search-item, .item, .grid-archive .item, .poster-title').closest('div, article, li').filter(function() {
            return $(this).find('a').length > 0;
        });

        const seenUrls = new Set();
        items.each((_, el) => {
            let titleElem = $(el).find('h3.poster-title, h2 a, a[title]').first();
            if (!titleElem.length) return;
            
            let title = titleElem.attr('title') || titleElem.text().trim();
            const urlElem = titleElem.closest('a').length ? titleElem.closest('a') : (titleElem.find('a').length ? titleElem.find('a') : $(el).find('a').first());
            const url = urlElem.attr("href") || "";
            
            if (!title || !url || seenUrls.has(url)) return;
            seenUrls.add(url);
            
            title = title.replace(/^Nonton\s+(?:movie|series|film)?\s*/i, "")
                         .replace(/\s+streaming\s+gratis.*/i, "")
                         .replace(/\s+sub\s+indo.*/i, "")
                         .trim();
            
            let poster = $(el).find("img").attr("data-src") || $(el).find("img").attr("src") || "";
            if (poster && poster.startsWith('//')) poster = 'https:' + poster;

            const quality = $(el).find(".quality").text().trim() || "HD";
            let synopsis = $(el).find(".synopsis").attr("data-full") || $(el).find(".synopsis").text().trim() || "";
            let year = $(el).find('.year').text().trim();
            
            let rating = $(el).find('.rating span[itemprop="ratingValue"]').text().trim();
            if (!rating) rating = $(el).find('.rating').text().trim().replace(/[^0-9.]/g, '');
            if (!rating || rating === "0" || rating === "0.0") rating = "-";
            
            let duration = $(el).find('.duration').text().trim() || "-";
            
            let genreStr = $(el).find('meta[itemprop="genre"]').attr('content') || $(el).find('.genre').text().trim() || "";
            let genres = genreStr ? genreStr.split(/,\s*|\/\s*/).map(g => g.trim()).filter(Boolean) : [];

            if (!year) {
                const ym = title.match(/\((\d{4})\)/);
                if (ym) year = ym[1];
            }
            if (!year) {
                const ymUrl = url.match(/-(\d{4})(?:$|\/)/);
                if (ymUrl) year = ymUrl[1];
            }
            
            title = title.replace(/\s*\(\d{4}\)/, '').trim();
            
            movies.push({ 
                title, 
                year: year || "-",
                poster, 
                rating, 
                quality, 
                duration, 
                genres,
                synopsis, 
                url 
            });
        });

        movies.sort((a, b) => {
            const yearA = parseInt(a.year) || 0;
            const yearB = parseInt(b.year) || 0;
            return yearB - yearA;
        });

        return movies;
    }

    // 0. GET /api/status (Status Server & Active Target Domain)
    app.get('/api/status', (req, res) => {
        res.json({
            success: true,
            status: "ONLINE",
            engine: "100% Zero-Headless / Native Attestation Engine",
            active_target_domain: domainManager.getBaseUrl(),
            uptime_seconds: Math.floor(process.uptime()),
            memory_usage: process.memoryUsage().rss
        });
    });

    // 1. GET /api/home (Film Rilis Terbaru)
    app.get('/api/home', async (req, res) => {
        try {
            const movies = await scrapeMovieList("/");
            res.json({ success: true, total: movies.length, movies });
        } catch (e) {
            res.status(500).json({ success: false, error: e.message });
        }
    });

    // GET /api/trending (Film Populer)
    app.get(['/api/trending', '/api/populer'], async (req, res) => {
        try {
            const movies = await scrapeMovieList("/populer/");
            res.json({ success: true, total: movies.length, movies });
        } catch (e) {
            res.status(500).json({ success: false, error: e.message });
        }
    });

    // GET /api/series (TV Series)
    app.get('/api/series', async (req, res) => {
        try {
            const movies = await scrapeMovieList("/latest-series");
            res.json({ success: true, total: movies.length, movies });
        } catch (e) {
            res.status(500).json({ success: false, error: e.message });
        }
    });

    // 2. GET /api/search?q=keyword
    app.get('/api/search', async (req, res) => {
        const query = req.query.q;
        if (!query) return res.status(400).json({ error: 'Query parameter q is required' });

        try {
            const currentBase = domainManager.getBaseUrl();
            const response = await axios.get(`https://gudangvape.com/search.php?s=${encodeURIComponent(query)}&page=1`, {
                headers: {
                    "User-Agent": USER_AGENT,
                    "Cookie": `cf_clearance=${CF_CLEARANCE_API}`,
                    "Origin": currentBase,
                    "Referer": `${currentBase}/`
                }
            });
            
            const movies = [];
            const items = response.data.data || response.data.items || [];
            
            for (let item of items) {
                let title = item.title || "";
                let year = "";
                
                const ym = title.match(/\((\d{4})\)/);
                if (ym) year = ym[1];
                if (!year && item.slug) {
                    const ymUrl = item.slug.match(/-(\d{4})(?:$|\/)/);
                    if (ymUrl) year = ymUrl[1];
                }
                
                title = title.replace(/\s*\(\d{4}\)/, '').trim();

                let poster = item.poster || "";
                if (poster && !poster.startsWith('http')) {
                    poster = `https://poster.showcdnx.com/wp-content/uploads/${poster}`;
                }

                let genreStr = item.genre || "";
                let genres = genreStr ? genreStr.split(/,\s*|\/\s*/).map(g => g.trim()).filter(Boolean) : [];

                movies.push({
                    title: title,
                    year: year || "-",
                    poster: poster,
                    rating: item.rating || "-",
                    quality: item.quality || "HD",
                    duration: item.duration || "-",
                    genres: genres,
                    synopsis: item.synopsis || "",
                    url: `/${item.slug}`
                });
            }
            
            res.json({ success: true, total: movies.length, movies });
        } catch (e) {
            res.status(500).json({ success: false, error: e.message });
        }
    });

    // 3. GET /api/detail?url=/slug (Lengkap dengan Metadata & Instant Stream Mapping)
    app.get('/api/detail', async (req, res) => {
        const url = req.query.url;
        if (!url) return res.status(400).json({ error: 'URL is required' });
        
        try {
            const detailData = await scrapeDetail(url);
            const cleanSlug = normalizeSlug(url);
            const streamsList = [];

            if (detailData.streams && Object.keys(detailData.streams).length > 0) {
                const serverKeys = Object.keys(detailData.streams);
                const defaultServer = serverKeys.includes('cast') ? 'cast' : serverKeys[0];

                for (const serverName of serverKeys) {
                    const isDefault = (serverName.toLowerCase() === defaultServer.toLowerCase());
                    const playUrl = isDefault 
                        ? `http://localhost:${PORT}/play/${cleanSlug}`
                        : `http://localhost:${PORT}/play/${cleanSlug}?server=${serverName}`;

                    let resolutions = ['1080p', '720p', '480p'];
                    if (serverName.toLowerCase() === 'hydrax') {
                        resolutions = ['Auto (Hydrax Native)'];
                    }

                    // Ambil resolusi presisi jika sudah ada dalam sesi cache
                    const sessionKey = `${cleanSlug}?server=${serverName.toLowerCase()}`;
                    if (streamSessions.has(sessionKey) && Array.isArray(streamSessions.get(sessionKey).resolutions)) {
                        resolutions = streamSessions.get(sessionKey).resolutions;
                    }

                    streamsList.push({
                        server: serverName,
                        resolutions: resolutions,
                        play_url: playUrl
                    });
                }
            }

            // Urutkan streams agar cast, p2p, turbovip, hydrax rapi
            streamsList.sort((a, b) => {
                const order = { 'cast': 1, 'p2p': 2, 'turbovip': 3, 'hydrax': 4 };
                return (order[a.server] || 99) - (order[b.server] || 99);
            });

            res.json({
                success: true,
                data: {
                    title: detailData.title,
                    year: detailData.year,
                    poster: detailData.poster,
                    rating: detailData.rating,
                    quality: detailData.quality,
                    duration: detailData.duration,
                    age_rating: detailData.age_rating,
                    genres: detailData.genres,
                    countries: detailData.countries,
                    directors: detailData.directors,
                    synopsis: detailData.synopsis,
                    streams: streamsList,
                    episodes: detailData.episodes || []
                }
            });
        } catch (e) {
            res.status(500).json({ success: false, error: e.message });
        }
    });

    // 4. GET /api/extract?url=serverUrl
    app.get('/api/extract', async (req, res) => {
        const playerUrl = req.query.url; 
        const slug = req.query.slug || '';
        const server = req.query.server || '';
        const title = req.query.title || '';

        if (!playerUrl) return res.status(400).json({ success: false, message: 'Missing url parameter' });

        try {
            const result = await extractStream(playerUrl, slug, server, title);
            res.json(result);
        } catch (e) {
            console.error('[Extract Error]:', e.message);
            res.status(500).json({ success: false, error: e.message });
        }
    });

    // 5. GET /api/session/:slug
    app.get('/api/session/:slug', async (req, res) => {
        const slug = req.params.slug;
        const server = req.query.server || '';

        const session = await getOrExtractMovieStream(slug, server);

        if (session) {
            const cleanSlug = normalizeSlug(session.slug || slug);
            const serverName = session.server || server || 'cast';
            return res.json({
                success: true,
                slug: cleanSlug,
                server: serverName,
                type: session.type || 'm3u8',
                iframe: session.iframe,
                resolutions: session.resolutions || [],
                title: session.title || cleanSlug.replace(/-/g, ' '),
                play_url: `http://localhost:${PORT}/play/${cleanSlug}${server ? '?server=' + server : ''}`,
                stream_url: `http://localhost:${PORT}/stream/${cleanSlug}${server ? '?server=' + server : ''}`
            });
        }
        res.status(404).json({ success: false, message: 'Film atau stream video tidak ditemukan.' });
    });

    // 6. Route Pemutaran Video di Browser (/play/:slug)
    app.get('/play/:slug', (req, res) => {
        res.sendFile(path.join(__dirname, 'public', 'play.html'));
    });

    // 7. Route Master Stream (/stream/:slug)
    app.get(['/stream/:slug', '/stream/:slug.m3u8'], async (req, res) => {
        const slug = req.params.slug;
        const server = req.query.server || '';

        const session = await getOrExtractMovieStream(slug, server);

        if (!session || !session.raw_url) {
            return res.status(404).send('Stream not found for movie: ' + slug);
        }

        return handleProxyStream(session.raw_url, req, res);
    });

    // 8. Endpoint Proxy HLS (/proxy-stream?url=...)
    app.get('/proxy-stream', async (req, res) => {
        const targetUrl = req.query.url;
        if (!targetUrl) return res.status(400).send('Missing url parameter');
        return handleProxyStream(targetUrl, req, res);
    });

    // Inisialisasi Domain & Jalankan Server
    (async () => {
        await domainManager.detectActiveDomain().catch(() => {});
        app.listen(PORT, () => {
            console.log(`✅ LK21 Premium Server (Zero-Headless Engine) berjalan di http://localhost:${PORT}`);
            console.log(`📡 Domain Target Aktif: ${domainManager.getBaseUrl()}`);
        });
    })();

}
