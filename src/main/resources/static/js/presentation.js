(function () {
    const slides = Array.prototype.slice.call(document.querySelectorAll('.slide'));
    const counter = document.getElementById('slideCounter');
    let index = 0;

    function show(i) {
        index = (i + slides.length) % slides.length;
        slides.forEach(function (s, n) {
            s.classList.toggle('active', n === index);
        });
        if (counter) {
            counter.textContent = (index + 1) + ' / ' + slides.length;
        }
    }

    function next() { show(index + 1); }
    function prev() { show(index - 1); }

    document.getElementById('nextBtn').addEventListener('click', next);
    document.getElementById('prevBtn').addEventListener('click', prev);

    document.addEventListener('keydown', function (e) {
        if (e.key === 'ArrowRight' || e.key === ' ' || e.key === 'PageDown') {
            e.preventDefault();
            next();
        } else if (e.key === 'ArrowLeft' || e.key === 'PageUp') {
            e.preventDefault();
            prev();
        } else if (e.key === 'Escape') {
            window.location.href = '/';
        } else if (e.key === 'Home') {
            show(0);
        } else if (e.key === 'End') {
            show(slides.length - 1);
        }
    });

    // Click right half of slide to advance (except links/buttons)
    document.getElementById('deck').addEventListener('click', function (e) {
        if (e.target.closest('a, button')) {
            return;
        }
        const mid = window.innerWidth / 2;
        if (e.clientX >= mid) {
            next();
        } else {
            prev();
        }
    });

    show(0);
})();
