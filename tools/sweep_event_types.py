import argparse
import io
import os
import re
import sys

BUKKIT_EVENT = 'org.bukkit.event.Event'
PLATFORM_EVENT = 'org.skriptlang.skript.lang.event.PlatformEvent'
MINESTOM_EVENT = 'net.minestom.server.event.Event'
MOVED = {
    'org.bukkit.event.EventPriority': 'org.skriptlang.skript.lang.event.EventPriority',
    'org.bukkit.event.Cancellable': 'org.skriptlang.skript.lang.event.Cancellable',
}
QUALIFIED_BUKKIT_EVENT = re.compile(r'\borg\.bukkit\.event\.Event\b')
LINK = re.compile(r'(\{@link(?:plain)?\s+[^}(]*\(|@see\s+[^\n(]*\()([^)]*)(\))')
LINK_EVENT = re.compile(r'(?<![.\w])Event\b')
LINK_TARGET_QUALIFIED = re.compile(r'(\{@link(?:plain)?\s+)org\.bukkit\.event\.Event(?=[\s}#])()')
LINK_TARGET = re.compile(r'(\{@link(?:plain)?\s+)Event(?=[\s}#])()')

CLASSDECL = re.compile(r'(\bclass\s+\w+(?:<[^<>]*(?:<[^<>]*>)?[^<>]*>)?\s+extends\s+)Event\b')
NESTED = re.compile(r'\.\s*Event\b')
BARE = re.compile(r'\bEvent\b')
JAVADOC_REF = re.compile(r'\{@link[^}]*\}|\{@linkplain[^}]*\}|@see[^\n]*|#\w+\([^)]*\)')

CODE, VERBATIM = 'code', 'verbatim'

MANUAL = {
}


def scan(src):
    spans, buf, i, n = [], [], 0, len(src)

    def flush():
        if buf:
            spans.append((CODE, ''.join(buf)))
            buf.clear()

    def take(j):
        flush()
        spans.append((VERBATIM, src[i:j]))
        return j

    while i < n:
        c, nxt = src[i], src[i + 1] if i + 1 < n else ''
        if c == '/' and nxt == '/':
            j = src.find('\n', i)
            i = take(n if j == -1 else j)
        elif c == '/' and nxt == '*':
            j = src.find('*/', i + 2)
            i = take(n if j == -1 else j + 2)
        elif src.startswith('"""', i):
            j = i + 3
            while j < n:
                if src[j] == '\\':
                    j += 2
                elif src.startswith('"""', j):
                    j += 3
                    break
                else:
                    j += 1
            i = take(min(j, n))
        elif c in '"\'':
            j = i + 1
            while j < n and src[j] != '\n':
                if src[j] == '\\':
                    j += 2
                    continue
                if src[j] == c:
                    j += 1
                    break
                j += 1
            i = take(min(j, n))
        else:
            buf.append(c)
            i += 1
    flush()
    return spans


def move_types(text):
    for old, new in MOVED.items():
        text = re.sub(r'\b%s\b' % re.escape(old), new, text)
    return text


def rewrite_links(text):
    def retype(m):
        args = QUALIFIED_BUKKIT_EVENT.sub('PlatformEvent', m.group(2))
        return m.group(1) + LINK_EVENT.sub('PlatformEvent', args) + m.group(3)
    text = LINK.sub(retype, text)
    text = LINK_TARGET_QUALIFIED.sub(lambda m: m.group(1) + PLATFORM_EVENT + m.group(2), text)
    return LINK_TARGET.sub(lambda m: m.group(1) + 'PlatformEvent' + m.group(2), text)


def rewrite_code(code):
    code = move_types(code)
    code = code.replace(MINESTOM_EVENT, '\x00M\x00')
    code = QUALIFIED_BUKKIT_EVENT.sub('\x00B\x00', code)
    code = NESTED.sub('\x00N\x00', code)
    has_decl = bool(CLASSDECL.search(code))
    code = CLASSDECL.sub(lambda m: m.group(1) + '\x00C\x00', code)
    code = code.replace('\x00B\x00', PLATFORM_EVENT)
    code = BARE.sub('PlatformEvent', code)
    code = code.replace('\x00M\x00', MINESTOM_EVENT)
    code = code.replace('\x00N\x00', '.Event')
    code = code.replace('\x00C\x00', 'Event')
    return code, has_decl


def rewrite_comment(text):
    kept = []

    def hold(m):
        kept.append(m.group(0))
        return '\x00K%d\x00' % (len(kept) - 1)

    t = JAVADOC_REF.sub(hold, text)
    t = t.replace(PLATFORM_EVENT, '\x00FQ\x00')
    t = t.replace('PlatformEvent', 'Event')
    t = t.replace('\x00FQ\x00', PLATFORM_EVENT)
    return re.sub(r'\x00K(\d+)\x00', lambda m: kept[int(m.group(1))], t)


def is_comment(text):
    s = text.lstrip()
    return s.startswith('//') or s.startswith('/*')


def sweep_file(path):
    src = io.open(path, encoding='utf-8').read()
    if BUKKIT_EVENT not in src and not any(old in src for old in MOVED):
        return None
    out, has_decl = [], False
    for kind, text in scan(src):
        if kind == VERBATIM:
            out.append(rewrite_links(text) if is_comment(text) else text)
        else:
            new, d = rewrite_code(text)
            has_decl = has_decl or d
            out.append(new)
    s = ''.join(out)

    imports = []
    if has_decl:
        imports.append('import %s;' % BUKKIT_EVENT)
    if 'PlatformEvent' in s:
        imports.append('import %s;' % PLATFORM_EVENT)
    s = s.replace('import %s;\n' % PLATFORM_EVENT,
                  ('\n'.join(imports) + '\n') if imports else '', 1)

    seen, kept = set(), []
    for line in s.split('\n'):
        if line.startswith('import '):
            if line in seen:
                continue
            seen.add(line)
        kept.append(line)
    s = '\n'.join(kept)
    return src, s


def audit_file(path):
    src = io.open(path, encoding='utf-8').read()
    if 'PlatformEvent' not in src:
        return []
    problems = []
    for kind, text in scan(src):
        if kind != VERBATIM or 'PlatformEvent' not in text:
            continue
        if is_comment(text):
            stripped = JAVADOC_REF.sub('', text).replace(PLATFORM_EVENT, '')
            if 'PlatformEvent' in stripped:
                problems.append(('comment-prose', text.strip()[:120]))
        else:
            problems.append(('string-literal', text.strip()[:120]))
    return problems


def walk(roots):
    for root_dir in roots:
        for root, dirs, files in os.walk(root_dir):
            if os.sep + 'org' + os.sep + 'bukkit' in root:
                continue
            if os.sep + 'platform' + os.sep + 'bukkit' in root:
                continue
            for fn in files:
                if fn.endswith('.java') and fn not in MANUAL:
                    yield os.path.join(root, fn)


def main():
    ap = argparse.ArgumentParser(
        description='Retype threaded Skript events from org.bukkit.event.Event to '
                    'PlatformEvent. Re-run after every upstream merge, across common and '
                    'every host module that overrides it.')
    ap.add_argument('roots', nargs='+',
                    help='source roots to sweep, e.g. common/src/main/java')
    ap.add_argument('--check', action='store_true',
                    help='do not write; report files that would change, and audit for '
                         'PlatformEvent leaking into string literals or comment prose')
    args = ap.parse_args()

    for name, why in sorted(MANUAL.items()):
        print('skipped (review by hand): %s - %s' % (name, why))

    changed, problems = [], []
    for path in walk(args.roots):
        if args.check:
            result = sweep_file(path)
            if result and result[0] != result[1]:
                changed.append(path)
            for kind, text in audit_file(path):
                problems.append((path, kind, text))
        else:
            result = sweep_file(path)
            if result and result[0] != result[1]:
                io.open(path, 'w', encoding='utf-8', newline='').write(result[1])
                changed.append(path)

    for path, kind, text in problems:
        print('%s: %s: %s' % (kind, path, text))
    print('%d file(s) %s' % (len(changed), 'would change' if args.check else 'swept'))
    if problems:
        print('%d suspicious span(s) - PlatformEvent must never appear in a string '
              'literal or in comment prose' % len(problems))
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main())
