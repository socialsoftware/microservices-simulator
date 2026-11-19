"""
JMeter 5.6.3 semantics needed by the functionality tests migrated from the thesis-case .jmx files.

Pure functions (no Locust, no gevent), so they can be checked against JMeter itself with
src/test/locust/jmeter-semantics/check_semantics.py --python. FUNCTIONALITY-MIGRATION-PLAN.md §9 describes the
behaviour reproduced here; every rule below was confirmed against JMeter on fixtures.

Scope: the JSONPath forms used by the 17 .jmx files, i.e. a deep scan followed by property names, array indexes and
an optional trailing .length():  $..participants[0].name, $..participants.length(), $..quiz.aggregateId, ...
"""
import json
import re

_NO_VALUE = object()


class JsonPathError(Exception):
    """What Jayway / JMeter raise: empty body, invalid JSON, unsupported path."""


# ---------------------------------------------------------------------------------------------------------------------
# Parsing the response body (json-smart, permissive mode, as used by Jayway's default configuration)
# ---------------------------------------------------------------------------------------------------------------------

def parse_body(body):
    """Parses a response body the way JsonPath.read does in JMeter.

    - empty body: error (JMeter: "json can not be null or empty")
    - valid JSON: the value, with object key order preserved
    - text that is not JSON and does not start with '{' or '[' (e.g. "Error on ...", an HTML page, "502 Bad
      Gateway"): json-smart's permissive mode reads it as a scalar, so every $.. path yields nothing
    - text starting with '{' or '[' that is not valid JSON (e.g. a truncated body): error
    """
    if body is None or body == "":
        raise JsonPathError("json can not be null or empty")
    text = body.strip()
    try:
        return json.loads(text)
    except ValueError:
        if text[:1] in ("{", "["):
            raise JsonPathError("invalid JSON")
        return text


# ---------------------------------------------------------------------------------------------------------------------
# Path evaluation (Jayway json-path 2.8.0)
# ---------------------------------------------------------------------------------------------------------------------

_PATH_RE = re.compile(r"^\$\.\.([A-Za-z_][A-Za-z0-9_]*)((?:\.[A-Za-z_][A-Za-z0-9_]*|\[\d+\])*)(\.length\(\))?$")
_TOKEN_RE = re.compile(r"\.([A-Za-z_][A-Za-z0-9_]*)|\[(\d+)\]")


def _compile(path):
    m = _PATH_RE.match(path)
    if not m:
        raise JsonPathError(f"unsupported JSONPath: {path}")
    first, rest, length = m.groups()
    tokens = [("prop", first)]
    for name, index in _TOKEN_RE.findall(rest):
        tokens.append(("prop", name) if name else ("index", int(index)))
    return tokens, bool(length)


def _scan(node, key):
    """Deep scan in Jayway's order: an object's own matching key first, then its children, depth first."""
    if isinstance(node, dict):
        if key in node:
            yield node[key]
        for value in node.values():
            yield from _scan(value, key)
    elif isinstance(node, list):
        for value in node:
            yield from _scan(value, key)


def _step(value, token):
    kind, arg = token
    if kind == "prop":
        if isinstance(value, dict) and arg in value:
            return value[arg]
        return _NO_VALUE
    if isinstance(value, list) and arg < len(value):
        return value[arg]
    return _NO_VALUE


def evaluate(doc, path):
    """Value JsonPath.read returns for `path` on the parsed document `doc`.

    A $.. path returns the list of every match (possibly empty); with a trailing .length() it returns one integer:
    the number of elements under all matches, 0 when nothing matches.
    """
    tokens, length = _compile(path)
    results = []
    for candidate in _scan(doc, tokens[0][1]):
        value = candidate
        for token in tokens[1:]:
            value = _step(value, token)
            if value is _NO_VALUE:
                break
        if value is not _NO_VALUE:
            results.append(value)
    if length:
        return sum(len(v) for v in results if isinstance(v, (list, dict)))
    return results


def read(body, path):
    return evaluate(parse_body(body), path)


# ---------------------------------------------------------------------------------------------------------------------
# Value rendering and comparison (JSONPathAssertion.objectToString, json-smart JSONValue.parse, Java equals)
# ---------------------------------------------------------------------------------------------------------------------

def object_to_string(value):
    """JSONPathAssertion.objectToString: compact JSON for objects and arrays, plain text for scalars."""
    if value is None:
        return "null"
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, (dict, list)):
        return json.dumps(value, separators=(",", ":"), ensure_ascii=False)
    return str(value)


def _parse_expected(text):
    """json-smart JSONValue.parse (permissive): JSON when the text is JSON, otherwise the text itself."""
    try:
        return json.loads(text)
    except ValueError:
        return text


def _java_equals(a, b):
    if isinstance(a, bool) or isinstance(b, bool):
        return isinstance(a, bool) and isinstance(b, bool) and a == b
    if isinstance(a, (int, float)) and isinstance(b, (int, float)):
        return type(a) is type(b) and a == b
    if isinstance(a, list) and isinstance(b, list):
        return len(a) == len(b) and all(_java_equals(x, y) for x, y in zip(a, b))
    if isinstance(a, dict) and isinstance(b, dict):
        return a.keys() == b.keys() and all(_java_equals(a[k], b[k]) for k in a)
    return type(a) is type(b) and a == b


def _is_equals(subject, expected, regex):
    if regex:
        try:
            return re.fullmatch(expected, object_to_string(subject)) is not None
        except re.error as e:
            raise JsonPathError(f"bad regex {expected!r}: {e}")
    return _java_equals(_parse_expected(expected), subject)


def jsonpath_assertion(body, path, expected, regex=False):
    """JSONPathAssertion with JSONVALIDATION=true, EXPECT_NULL=false, INVERT=false.

    Returns (passed, failure_message)."""
    if body is None or body == "":
        return False, "Response was null"
    try:
        value = read(body, path)
        if isinstance(value, list):
            if not value and expected == "[]":
                return True, ""
            if any(_is_equals(s, expected, regex) for s in value) or _is_equals(value, expected, regex):
                return True, ""
        elif _is_equals(value, expected, regex):
            return True, ""
    except JsonPathError as e:
        return False, str(e)
    if regex:
        return False, (f"Value in json path '{path}' expected to match regexp '{expected}', "
                       f"but it did not match: '{object_to_string(value)}'")
    return False, f"Value in json path '{path}' expected to be '{expected}', but found '{object_to_string(value)}'"


def json_extract(body, path):
    """JSON Extractor with Match No. 1 and no default value: the first match as text, "" when there is none."""
    try:
        if body is None or body == "":
            return ""
        value = read(body, path)
    except JsonPathError:
        return ""
    if isinstance(value, list):
        if not value:
            return ""
        value = value[0]
    return object_to_string(value)


# ---------------------------------------------------------------------------------------------------------------------
# Response assertion and default sample rule
# ---------------------------------------------------------------------------------------------------------------------

RESPONSE_EQUALS = 8           # Assertion.test_type: equals
RESPONSE_NOT_SUBSTRING = 20   # Assertion.test_type: NOT (4) + substring (16)


def response_code_assertion(code, pattern, test_type):
    """ResponseAssertion on the response code with "Ignore status" (assume_success=true).

    `code` is the response code as JMeter records it: "200", or "Non HTTP response code: ..." on a transport
    error. Returns (passed, failure_message)."""
    code = code or ""
    if test_type == RESPONSE_NOT_SUBSTRING:
        if code == "":
            return True, ""
        found = pattern in code
        return (not found), ("" if not found else f"Test failed: code expected not to contain /{pattern}/")
    if test_type == RESPONSE_EQUALS:
        if code == "":
            return False, "Response was null"
        found = code == pattern
        return found, ("" if found else f"Test failed: code expected to equal /{pattern}/ but was /{code}/")
    raise ValueError(f"unsupported response assertion test type {test_type}")


def sample_ok(code):
    """HTTP sampler default rule (MetricUtils.isSuccessCode): success iff the code is numeric and 200-399."""
    return code is not None and code.isdigit() and 200 <= int(code) <= 399


# ---------------------------------------------------------------------------------------------------------------------
# Wire form of a path (HttpClient 4 normalization as used by JMeter)
# ---------------------------------------------------------------------------------------------------------------------

def wire_path(path):
    """Repeated slashes in the path part are collapsed; the query string and dot segments are left untouched."""
    base, sep, query = path.partition("?")
    return re.sub(r"/{2,}", "/", base) + sep + query
