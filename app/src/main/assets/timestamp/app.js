// DOM Elements
const uploadZone = document.getElementById('uploadZone');
const fileInput = document.getElementById('fileInput');
const canvasContainer = document.getElementById('canvasContainer');
const canvas = document.getElementById('previewCanvas');
const ctx = canvas.getContext('2d');

const dateInput = document.getElementById('dateInput');
const timeInput = document.getElementById('timeInput');
const latInput = document.getElementById('latInput');
const lngInput = document.getElementById('lngInput');
const addressInput = document.getElementById('addressInput');

const btnApply = document.getElementById('btnApply');
const btnDownload = document.getElementById('btnDownload');

let currentImage = null;
let currentMapImg = null;

// === Inisialisasi Peta Interaktif (Leaflet) dengan Tampilan Google Maps ===
let uiMap = L.map('uiMap').setView([-6.200000, 106.816666], 13); // Default Jakarta
// Menggunakan server tile publik Google agar tampilannya 100% Google Maps
L.tileLayer('https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
    attribution: '&copy; Google Maps',
    maxZoom: 20
}).addTo(uiMap);

// Tambahkan fitur pencarian (Geocoder) dari Leaflet
const customGeocoder = {
    geocode: async function(query, cb, context) {
        try {
            const res = await fetch(`${GAS_URL}?action=search&q=${encodeURIComponent(query)}`);
            const data = await res.json();
            if (data && data.status === 'success' && data.results) {
                const results = data.results.map(r => ({
                    name: r.name,
                    center: L.latLng(r.center.lat, r.center.lng),
                    bbox: L.latLngBounds(
                        L.latLng(r.bbox._southWest.lat, r.bbox._southWest.lng),
                        L.latLng(r.bbox._northEast.lat, r.bbox._northEast.lng)
                    )
                }));
                cb.call(context, results);
            } else {
                cb.call(context, []);
            }
        } catch (e) {
            console.error(e);
            cb.call(context, []);
        }
    },
    reverse: async function(location, scale, cb, context) {
        cb.call(context, []);
    }
};

const geocoder = L.Control.geocoder({
    defaultMarkGeocode: false,
    placeholder: "Cari lokasi atau ketik Lat, Lng...",
    geocoder: customGeocoder
}).on('markgeocode', async function(e) {
    const latlng = e.geocode.center;
    uiMap.setView(latlng, 15);
    await updateLocation(latlng.lat, latlng.lng);
}).addTo(uiMap);

let uiMarker = L.marker([-6.200000, 106.816666], {draggable: true}).addTo(uiMap);

// Update lokasi saat peta diklik
uiMap.on('click', async (e) => {
    await updateLocation(e.latlng.lat, e.latlng.lng);
});

// Update lokasi saat pin digeser
uiMarker.on('dragend', async (e) => {
    const pos = uiMarker.getLatLng();
    await updateLocation(pos.lat, pos.lng);
});

async function updateLocation(lat, lng, doNotFetchAddress = false) {
    uiMarker.setLatLng([lat, lng]);
    uiMap.setView([lat, lng]);
    latInput.value = lat.toFixed(6);
    lngInput.value = lng.toFixed(6);
    
    if (!doNotFetchAddress) {
        addressInput.placeholder = "Sedang mencari alamat...";
        await fetchAddress(lat, lng);
    }
}

// Event listener manual input lat/lng
latInput.addEventListener('change', async () => {
    const lat = parseFloat(latInput.value);
    const lng = parseFloat(lngInput.value);
    if(!isNaN(lat) && !isNaN(lng)) {
        await updateLocation(lat, lng);
    }
});

lngInput.addEventListener('change', async () => {
    const lat = parseFloat(latInput.value);
    const lng = parseFloat(lngInput.value);
    if(!isNaN(lat) && !isNaN(lng)) {
        await updateLocation(lat, lng);
    }
});

// === Event Listeners for File Upload ===
uploadZone.addEventListener('click', () => fileInput.click());

uploadZone.addEventListener('dragover', (e) => {
    e.preventDefault();
    uploadZone.classList.add('dragover');
});

uploadZone.addEventListener('dragleave', () => {
    uploadZone.classList.remove('dragover');
});

uploadZone.addEventListener('drop', (e) => {
    e.preventDefault();
    uploadZone.classList.remove('dragover');
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
        handleFile(e.dataTransfer.files[0]);
    }
});

fileInput.addEventListener('change', (e) => {
    if (e.target.files && e.target.files.length > 0) {
        handleFile(e.target.files[0]);
    }
});

btnApply.addEventListener('click', async () => {
    await generateStamp();
});

btnDownload.addEventListener('click', () => {
    if (!canvas) return;
    const link = document.createElement('a');
    link.download = `geostamp_${Date.now()}.jpg`;
    link.href = canvas.toDataURL('image/jpeg', 0.9);
    link.click();
});

async function handleFile(file) {
    if (!file.type.startsWith('image/')) {
        alert('Mohon upload file gambar!');
        return;
    }

    currentImage = new Image();
    currentImage.src = URL.createObjectURL(file);
    
    currentImage.onload = async () => {
        uploadZone.style.display = 'none';
        canvasContainer.classList.remove('hidden');
        
        setTimeout(() => { uiMap.invalidateSize(); }, 300);
        
        let lat = null, lng = null, date = new Date();
        try {
            const exifData = await exifr.parse(file, {pick: ['latitude', 'longitude', 'DateTimeOriginal']});
            if (exifData) {
                if (exifData.latitude !== undefined) lat = exifData.latitude;
                if (exifData.longitude !== undefined) lng = exifData.longitude;
                if (exifData.DateTimeOriginal) date = new Date(exifData.DateTimeOriginal);
            }
        } catch (e) {
            console.log("Tidak dapat membaca EXIF", e);
        }

        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        dateInput.value = `${year}-${month}-${day}`;

        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');
        timeInput.value = `${hours}:${minutes}:${seconds}`;

        if (lat !== null && lng !== null) {
            await updateLocation(lat, lng);
        } else {
            if (navigator.geolocation) {
                navigator.geolocation.getCurrentPosition(async (pos) => {
                    await updateLocation(pos.coords.latitude, pos.coords.longitude);
                    await generateStamp();
                }, async () => {
                    alert("Gunakan peta untuk memilih lokasi secara manual.");
                    await generateStamp();
                });
                return;
            }
        }
        
        await generateStamp();
    };
}

const GAS_URL = "https://script.google.com/macros/s/AKfycbyvUXTPcHdkn91I4xdgeZ4j-ouqbZv0ICD2sfokoQ5USAIc5A3slPVlkNEocR03gZp9SA/exec";

async function fetchAddress(lat, lng) {
    try {
        const res = await fetch(`${GAS_URL}?action=geocode&lat=${lat}&lng=${lng}`);
        const data = await res.json();
        if (data && data.status === 'success' && data.address) {
            addressInput.value = data.address;
        }
    } catch (e) {
        console.error("Gagal mendapatkan alamat:", e);
    }
}

// Gunakan Google Apps Script (GAS) Web App untuk mengambil peta Google Maps secara gratis
async function fetchMapTile(lat, lng) {
    const width = 280;
    const height = 320;
    try {
        const res = await fetch(`${GAS_URL}?action=staticmap&lat=${lat}&lng=${lng}&w=${width}&h=${height}`);
        const data = await res.json();
        
        if (data && data.status === 'success' && data.image) {
            return new Promise((resolve) => {
                const img = new Image();
                img.onload = () => resolve(img);
                img.onerror = () => resolve(null);
                img.src = data.image; // Menggunakan gambar Base64 dari GAS
            });
        }
    } catch (e) {
        console.error("Gagal mengambil gambar peta:", e);
    }
    return null;
}

async function generateStamp() {
    if (!currentImage) return;

    btnApply.textContent = "⏳ Memproses...";
    btnApply.disabled = true;

    canvas.width = currentImage.width;
    canvas.height = currentImage.height;
    ctx.drawImage(currentImage, 0, 0);

    const lat = parseFloat(latInput.value);
    const lng = parseFloat(lngInput.value);
    
    const scale = Math.max(canvas.width, canvas.height) / 1400;
    
    const margin = 40 * scale;
    const mapWidth = 280 * scale;
    const mapHeight = 320 * scale;
    
    if (!isNaN(lat) && !isNaN(lng)) {
        currentMapImg = await fetchMapTile(lat, lng);
        if (currentMapImg) {
            const mapX = margin;
            const mapY = canvas.height - mapHeight - margin;
            
            ctx.drawImage(currentMapImg, mapX, mapY, mapWidth, mapHeight);
            
            const centerX = mapX + mapWidth/2;
            const centerY = mapY + mapHeight/2;
            
            // Karena gambar dari Google Static Maps sudah menyertakan pin merah asli
            // dan logo Google asli di bawah, kita tidak perlu menggambarnya dua kali.
        }
    }

    const dateStr = dateInput.value; 
    const timeStr = timeInput.value; 
    const addressStr = addressInput.value;
    
    let dateFormatted = "";
    if (dateStr) {
        const d = new Date(dateStr);
        const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        dateFormatted = `${d.getDate()} ${months[d.getMonth()]} ${d.getFullYear()}`;
    }

    function toDMS(coordinate, isLat) {
        if (isNaN(coordinate)) return "-";
        const absolute = Math.abs(coordinate);
        const degrees = Math.floor(absolute);
        const minutesNotTruncated = (absolute - degrees) * 60;
        const minutes = Math.floor(minutesNotTruncated);
        let seconds = ((minutesNotTruncated - minutes) * 60).toFixed(3);
        seconds = seconds.replace('.', ','); 
        
        let direction = "";
        if (isLat) {
            direction = coordinate >= 0 ? "N" : "S";
        } else {
            direction = coordinate >= 0 ? "E" : "W";
        }
        
        const sign = coordinate < 0 ? '-' : '';
        return `${sign}${degrees}°${minutes}'${seconds}"${direction}`;
    }

    const latDMS = toDMS(lat, true);
    const lngDMS = toDMS(lng, false);
    
    let addressLines = [];
    if (addressStr) {
        const parts = addressStr.split(',');
        for (let part of parts) {
            if (part.trim().length > 0) {
                addressLines.push(part.trim());
            }
        }
    }

    const timeFormatted = timeStr.replace(/:/g, '.');

    const textLines = [
        `Network: ${dateFormatted} ${timeFormatted} WIB`,
        `Local: ${dateFormatted} ${timeFormatted} WIB`,
        `${latDMS} ${lngDMS}`,
        ...addressLines
    ];

    const fontSize = 21 * scale;
    ctx.font = `${fontSize}px Arial, sans-serif`; 
    ctx.textAlign = 'right';
    ctx.textBaseline = 'bottom';
    
    const lineSpacing = 1.15; 
    const textX = canvas.width - margin;
    let textY = canvas.height - margin;
    
    for(let i = textLines.length - 1; i >= 0; i--) {
        ctx.lineWidth = 3.5 * scale;
        ctx.strokeStyle = 'rgba(0, 0, 0, 0.8)';
        ctx.strokeText(textLines[i], textX, textY);
        
        ctx.fillStyle = 'white';
        ctx.fillText(textLines[i], textX, textY);
        
        textY -= (fontSize * lineSpacing);
    }
    
    btnApply.textContent = "Terapkan Perubahan ke Foto";
    btnApply.disabled = false;
    btnDownload.disabled = false;
}
