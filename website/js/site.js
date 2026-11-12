/**
 * DejaView website — progressive enhancement.
 * Everything works without JS; this adds code highlighting and copy buttons.
 */
(function () {
    'use strict';

    if (window.dejaviewHighlight) {
        window.dejaviewHighlight.highlightAll(document);
    }

    document.querySelectorAll('[data-copy]').forEach(function (button) {
        button.addEventListener('click', function () {
            var text = button.getAttribute('data-copy');
            navigator.clipboard.writeText(text).then(function () {
                var original = button.textContent;
                button.textContent = 'Copied';
                setTimeout(function () {
                    button.textContent = original;
                }, 1600);
            });
        });
    });
})();
