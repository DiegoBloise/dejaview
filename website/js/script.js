/* Shared site navigation script for the DejaView website.
   It keeps the documentation navigation synchronized with the sections on the page. */
(function () {
    "use strict";

    var docNav = document.querySelector('.doc-nav');
    var links = Array.from(docNav.querySelectorAll('a'));

    function updateActive() {
        var sections = Array.from(document.querySelectorAll('.doc-section'));
        var currentId = sections.length ? sections[0].id : null;
        for (var i = 0; i < sections.length; i++) {
            if (sections[i].getBoundingClientRect().top <= 0) {
                currentId = sections[i].id;
            }
        }
        links.forEach(function (link) {
            if (link.getAttribute('href') === '#' + currentId) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });
    }

    if (docNav && links.length) {
        window.addEventListener('scroll', updateActive);
        updateActive();
    }

    // Expose helper for playground page.
    window.dejaviewSite = {
        addScrollHighlight: updateActive,
    };
})();