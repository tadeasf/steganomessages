// Global JavaScript functions for the application

// HTMX configuration
htmx.config.globalViewTransitions = true;

// File upload utilities
function updateFileName(input) {
    const fileNameDiv = document.getElementById('file-name');
    if (input.files.length > 0) {
        fileNameDiv.textContent = input.files[0].name;
        fileNameDiv.classList.remove('hidden');
    } else {
        fileNameDiv.classList.add('hidden');
    }
}

// Dark mode toggle (if needed in future)
function toggleDarkMode() {
    document.documentElement.classList.toggle('dark');
    localStorage.setItem('darkMode', document.documentElement.classList.contains('dark'));
}

// Initialize dark mode from localStorage
document.addEventListener('DOMContentLoaded', function () {
    if (localStorage.getItem('darkMode') === 'true') {
        document.documentElement.classList.add('dark');
    }
});

// HTMX error handling
document.addEventListener('htmx:responseError', function (event) {
    console.error('HTMX Request failed:', event.detail);
    alert('An error occurred. Please try again.');
});

// HTMX loading states
document.addEventListener('htmx:beforeRequest', function (event) {
    const target = event.target;
    if (target.classList.contains('htmx-request')) {
        target.disabled = true;
    }
});

document.addEventListener('htmx:afterRequest', function (event) {
    const target = event.target;
    if (target.classList.contains('htmx-request')) {
        target.disabled = false;
    }
});

// File upload drag & drop functionality
document.addEventListener('DOMContentLoaded', function () {
    const fileUploadAreas = document.querySelectorAll('.file-upload-area');

    fileUploadAreas.forEach(area => {
        area.addEventListener('dragover', function (e) {
            e.preventDefault();
            this.classList.add('dragover');
        });

        area.addEventListener('dragleave', function (e) {
            e.preventDefault();
            this.classList.remove('dragover');
        });

        area.addEventListener('drop', function (e) {
            e.preventDefault();
            this.classList.remove('dragover');

            const files = e.dataTransfer.files;
            if (files.length > 0) {
                const fileInput = this.querySelector('input[type="file"]');
                if (fileInput) {
                    fileInput.files = files;
                    // Trigger change event
                    const changeEvent = new Event('change', { bubbles: true });
                    fileInput.dispatchEvent(changeEvent);
                }
            }
        });
    });
});

// HTMX form validation helpers
document.addEventListener('htmx:responseError', function (event) {
    console.error('HTMX validation failed:', event.detail);
});

// Clear validation when user starts typing again
document.addEventListener('htmx:beforeRequest', function (event) {
    // Show loading state for validation endpoints
    if (event.detail.elt.hasAttribute('hx-indicator')) {
        const indicatorId = event.detail.elt.getAttribute('hx-indicator');
        const indicator = document.querySelector(indicatorId);
        if (indicator) {
            indicator.style.display = 'block';
        }
    }
});

document.addEventListener('htmx:afterRequest', function (event) {
    // Hide loading state after validation
    if (event.detail.elt.hasAttribute('hx-indicator')) {
        const indicatorId = event.detail.elt.getAttribute('hx-indicator');
        const indicator = document.querySelector(indicatorId);
        if (indicator) {
            indicator.style.display = 'none';
        }
    }
}); 