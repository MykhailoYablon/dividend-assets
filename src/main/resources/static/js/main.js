// ── Sidebar toggle ──
const sidebar = document.getElementById('sidebar');
const toggleBtn = document.getElementById('sidebarToggle');

toggleBtn.addEventListener('click', () => {
    sidebar.classList.toggle('collapsed');
    localStorage.setItem('sidebar-collapsed', sidebar.classList.contains('collapsed'));
});

if (localStorage.getItem('sidebar-collapsed') === 'true') {
    sidebar.classList.add('collapsed');
}

// ── File upload ──
const dropZone     = document.getElementById('dropZone');
const fileInput    = document.getElementById('fileInput');
const fileSelected = document.getElementById('fileSelected');
const fileName     = document.getElementById('fileName');
const fileSize     = document.getElementById('fileSize');
const fileRemove   = document.getElementById('fileRemove');
const errorMsg     = document.getElementById('errorMsg');

const ALLOWED = ['.csv', '.xml'];

function formatBytes(b) {
    if (b < 1024) return b + ' B';
    if (b < 1048576) return (b / 1024).toFixed(1) + ' KB';
    return (b / 1048576).toFixed(1) + ' MB';
}

function isValid(name) {
    return ALLOWED.some(ext => name.toLowerCase().endsWith(ext));
}

function showFile(file) {
    if (!isValid(file.name)) {
        errorMsg.textContent = '⚠ Only .csv and .xml files are accepted.';
        errorMsg.classList.add('visible');
        fileSelected.classList.remove('visible');
        return;
    }
    errorMsg.classList.remove('visible');
    fileName.textContent = file.name;
    fileSize.textContent = formatBytes(file.size);
    fileSelected.classList.add('visible');
}

fileInput.addEventListener('change', () => { if (fileInput.files.length) showFile(fileInput.files[0]); });

fileRemove.addEventListener('click', () => {
    fileInput.value = '';
    fileSelected.classList.remove('visible');
    errorMsg.classList.remove('visible');
});

dropZone.addEventListener('dragover', e => { e.preventDefault(); dropZone.classList.add('drag-over'); });
dropZone.addEventListener('dragleave', () => dropZone.classList.remove('drag-over'));
dropZone.addEventListener('drop', e => {
    e.preventDefault();
    dropZone.classList.remove('drag-over');
    const file = e.dataTransfer.files[0];
    if (!file) return;
    const dt = new DataTransfer();
    dt.items.add(file);
    fileInput.files = dt.files;
    showFile(file);
});

document.getElementById('uploadForm').addEventListener('submit', e => {
    if (!fileInput.files.length) {
        e.preventDefault();
        errorMsg.textContent = '⚠ Please select a file first.';
        errorMsg.classList.add('visible');
        return;
    }
    if (!isValid(fileInput.files[0].name)) {
        e.preventDefault();
        errorMsg.textContent = '⚠ Only .csv and .xml files are accepted.';
        errorMsg.classList.add('visible');
    }
});