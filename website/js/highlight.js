/**
 * Minimal, dependency-free Java syntax highlighter for DejaView.
 *
 * <p>The site is fully static and must work without a network connection, so
 * instead of a CDN syntax library this tokenizer highlights the small subset
 * of Java used across the website and the playground: comments, strings,
 * char literals, annotations, keywords, numbers and types.</p>
 */
(function () {
    'use strict';

    var KEYWORDS = [
        'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch',
        'char', 'class', 'const', 'continue', 'default', 'do', 'double',
        'else', 'enum', 'extends', 'final', 'finally', 'float', 'for',
        'if', 'implements', 'import', 'instanceof', 'int', 'interface',
        'long', 'native', 'new', 'non-sealed', 'package', 'permits',
        'private', 'protected', 'public', 'record', 'return', 'sealed',
        'short', 'static', 'strictfp', 'super', 'switch', 'synchronized',
        'this', 'throw', 'throws', 'transient', 'try', 'var', 'void',
        'volatile', 'while', 'true', 'false', 'null', 'yield'
    ];

    var PATTERN = new RegExp(
        '(\\/\\/[^\\n]*|\\/\\*[\\s\\S]*?\\*\\/|"(?:[^"\\\\\\n]|\\\\[\\s\\S])*"|\'(?:[^\'\\\\\\n]|\\\\[\\s\\S])*\')' + // 1 comment or string
        '|(@[A-Za-z_$][\\w$]*)' +                                          // 2 annotation
        '|\\b(' + KEYWORDS.join('|') + ')\\b' +                            // 3 keyword
        '|\\b(\\d[\\d_]*(?:\\.\\d[\\d_]*)?[fFdDlL]?|0[xX][0-9a-fA-F_]+)\\b' + // 4 number
        '|\\b([A-Z][A-Za-z0-9_$]*)\\b',                                    // 5 type
        'g'
    );

    function escapeHtml(text) {
        return text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    /**
     * Highlights Java source and returns an HTML string. Input is escaped,
     * never trusted as raw HTML.
     */
    function highlightJava(source) {
        return escapeHtml(String(source)).replace(
            PATTERN,
            function (match, str, ann, keyword, num, type) {
                if (str) {
                    if (str.charAt(0) === '/') {
                        return '<span class="tok-com">' + str + '</span>';
                    }
                    return '<span class="tok-str">' + str + '</span>';
                }
                if (ann) {
                    return '<span class="tok-ann">' + ann + '</span>';
                }
                if (keyword) {
                    return '<span class="tok-kw">' + keyword + '</span>';
                }
                if (num) {
                    return '<span class="tok-num">' + num + '</span>';
                }
                if (type) {
                    return '<span class="tok-type">' + type + '</span>';
                }
                return match;
            }
        );
    }

    function highlightAll(root) {
        var blocks = (root || document).querySelectorAll('[data-highlight-java]');
        for (var i = 0; i < blocks.length; i++) {
            var block = blocks[i];
            block.innerHTML = highlightJava(block.textContent);
        }
    }

    window.dejaviewHighlight = {
        java: highlightJava,
        highlightAll: highlightAll
    };
})();
