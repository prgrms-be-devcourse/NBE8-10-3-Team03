#!/usr/bin/env python3
import argparse
import json
import random
import re
import string
from typing import Any, Dict, List, Optional, Tuple

import requests
from concurrent.futures import ThreadPoolExecutor, as_completed

try:
    import yaml  # pyyaml 있으면 yaml 스펙도 읽음
except Exception:
    yaml = None


def rand_str(n: int) -> str:
    return "".join(random.choice(string.ascii_lowercase + string.digits) for _ in range(n))


def _is_bad_run3(pwd: str) -> bool:
    # 1) 동일 문자 3연속
    for i in range(2, len(pwd)):
        if pwd[i] == pwd[i - 1] == pwd[i - 2]:
            return True

    # 2) 문자/숫자 연속(증가/감소) 3연속: abc, cba, 123, 321
    for i in range(2, len(pwd)):
        a, b, c = pwd[i - 2], pwd[i - 1], pwd[i]
        if a.isdigit() and b.isdigit() and c.isdigit():
            oa, ob, oc = ord(a), ord(b), ord(c)
            if (ob == oa + 1 and oc == ob + 1) or (ob == oa - 1 and oc == ob - 1):
                return True
        elif a.isalpha() and b.isalpha() and c.isalpha():
            # 대/소문자 섞여도 연속으로 보지 않게, 전부 소문자로 비교
            oa, ob, oc = ord(a.lower()), ord(b.lower()), ord(c.lower())
            if (ob == oa + 1 and oc == ob + 1) or (ob == oa - 1 and oc == ob - 1):
                return True

    return False


def rand_password(min_len: int = 8, max_len: int = 20) -> str:
    upper = string.ascii_uppercase
    lower = string.ascii_lowercase
    digits = string.digits
    special = "!@#$%^&*()-_=+[]{};:,.<>?"

    groups = [upper, lower, digits, special]
    all_chars = upper + lower + digits + special

    length = random.randint(min_len, max_len)

    # 최소 3종 강제
    chosen_groups = random.sample(groups, 3)
    required_chars = [random.choice(g) for g in chosen_groups]

    # 생성: 규칙 위반이면 다시 생성(일회성이 아니라 "무조건 통과"용)
    for _ in range(50):
        slots = ["REQ"] * len(required_chars) + ["ANY"] * (length - len(required_chars))
        random.shuffle(slots)

        req_idx = 0
        pwd_chars: list[str] = []

        for slot in slots:
            if slot == "REQ":
                c = required_chars[req_idx]
                req_idx += 1
            else:
                c = random.choice(all_chars)

            # 3연속 동일 문자 방지
            if len(pwd_chars) >= 2 and pwd_chars[-1] == pwd_chars[-2]:
                bad = pwd_chars[-1]
                while c == bad:
                    c = random.choice(all_chars)

            # 연속 증가/감소 3개 방지: 마지막 2개와 합쳐서 bad면 다시 뽑기
            if len(pwd_chars) >= 2:
                for _try in range(30):
                    trial = "".join(pwd_chars[-2:] + [c])
                    if not _is_bad_run3(trial):
                        break
                    c = random.choice(all_chars)

            pwd_chars.append(c)

        pwd = "".join(pwd_chars)
        if not _is_bad_run3(pwd):
            return pwd

    # 여기까지 오면 거의 안 오는데, 안전장치
    raise RuntimeError("Failed to generate password satisfying policy")


def http_get_json_or_yaml(url: str, session: requests.Session) -> Optional[Dict[str, Any]]:
    r = session.get(url, timeout=10)
    if r.status_code < 200 or r.status_code >= 300:
        return None

    ct = (r.headers.get("Content-Type") or "").lower()
    text = r.text

    # JSON 우선
    try:
        return r.json()
    except Exception:
        pass

    # YAML
    if yaml is not None:
        try:
            return yaml.safe_load(text)
        except Exception:
            return None

    # yaml 라이브러리 없으면 포기
    return None


def load_openapi(base_url: str) -> Dict[str, Any]:
    s = requests.Session()
    base = base_url.rstrip("/")
    candidates = [
        f"{base}/v3/api-docs",
        f"{base}/v3/api-docs.yaml",
        f"{base}/v3/api-docs.yml",
        f"{base}/api-docs",
        f"{base}/api-docs.yaml",
        f"{base}/api-docs.yml",
    ]
    for u in candidates:
        spec = http_get_json_or_yaml(u, s)
        if isinstance(spec, dict) and spec.get("openapi"):
            return spec
    raise RuntimeError(f"OpenAPI spec not found. Tried: {', '.join(candidates)}")


def resolve_ref(spec: Dict[str, Any], ref: str) -> Optional[Dict[str, Any]]:
    # ex: "#/components/schemas/LoginRequest"
    if not ref.startswith("#/"):
        return None
    cur: Any = spec
    for part in ref[2:].split("/"):
        if not isinstance(cur, dict) or part not in cur:
            return None
        cur = cur[part]
    return cur if isinstance(cur, dict) else None


def schema_to_props(spec: Dict[str, Any], schema: Dict[str, Any]) -> Dict[str, Any]:
    if "$ref" in schema:
        resolved = resolve_ref(spec, schema["$ref"])
        if resolved:
            return schema_to_props(spec, resolved)
        return {}

    if "allOf" in schema and isinstance(schema["allOf"], list):
        merged: Dict[str, Any] = {}
        for s in schema["allOf"]:
            merged.update(schema_to_props(spec, s))
        return merged

    if schema.get("type") == "object" and isinstance(schema.get("properties"), dict):
        return schema["properties"]

    return {}


def extract_request_schema(spec: Dict[str, Any], op: Dict[str, Any]) -> Dict[str, Any]:
    rb = op.get("requestBody", {})
    if not isinstance(rb, dict):
        return {}
    if "$ref" in rb:
        rb = resolve_ref(spec, rb["$ref"]) or {}

    content = rb.get("content", {})
    if not isinstance(content, dict):
        return {}

    # json 우선
    for mt in ("application/json", "application/*+json"):
        if mt in content:
            schema = content[mt].get("schema", {})
            if isinstance(schema, dict):
                return schema
    # 그 외 아무거나
    for _, v in content.items():
        if isinstance(v, dict) and isinstance(v.get("schema"), dict):
            return v["schema"]
    return {}


def pick_field(props: List[str], prefer: List[str]) -> Optional[str]:
    # prefer 순서대로, 없으면 휴리스틱으로 고름
    lower_map = {p.lower(): p for p in props}
    for k in prefer:
        if k.lower() in lower_map:
            return lower_map[k.lower()]
    return None


def score_op(path: str, method: str, op: Dict[str, Any], target: str) -> int:
    """
    target: 'join' or 'login'
    """
    s = 0
    p = path.lower()
    m = method.lower()
    summary = str(op.get("summary", "")).lower()
    opid = str(op.get("operationId", "")).lower()
    tags = " ".join([str(t).lower() for t in op.get("tags", []) if isinstance(t, (str, int))])

    # 공통: members 관련
    if "member" in p or "/members" in p:
        s += 5
    if "member" in tags:
        s += 3

    # join/login 키워드
    if target == "join":
        if "join" in p or "signup" in p or "register" in p:
            s += 20
        if "join" in summary or "가입" in summary or "회원가입" in summary or "signup" in summary or "register" in summary:
            s += 20
        if "join" in opid or "signup" in opid or "register" in opid:
            s += 10
    else:
        if "login" in p or "signin" in p:
            s += 20
        if "login" in summary or "로그인" in summary or "signin" in summary:
            s += 20
        if "login" in opid or "signin" in opid:
            s += 10

    # POST 선호
    if m == "post":
        s += 5

    return s


def find_best_endpoint(spec: Dict[str, Any], target: str) -> Tuple[str, str, Dict[str, Any]]:
    paths = spec.get("paths", {})
    if not isinstance(paths, dict):
        raise RuntimeError("OpenAPI spec has no paths")

    best = (-1, "", "", {})  # score, path, method, op
    for path, item in paths.items():
        if not isinstance(item, dict):
            continue
        for method, op in item.items():
            if method.lower() not in ("post", "put", "patch", "get", "delete"):
                continue
            if not isinstance(op, dict):
                continue
            sc = score_op(path, method, op, target)
            if sc > best[0]:
                best = (sc, path, method.lower(), op)

    if best[0] < 10:
        raise RuntimeError(f"Could not confidently find {target} endpoint from OpenAPI")
    return best[1], best[2], best[3]


def is_success_rsdata(payload: Any) -> bool:
    if not isinstance(payload, dict):
        return False
    rc = str(payload.get("resultCode", "")).strip()

    # "201-1", "200-1" 같이 앞에 HTTP 코드가 붙는 형태면 2xx면 성공
    m = re.match(r"^(\d{3})-", rc)
    if m:
        code = int(m.group(1))
        return 200 <= code < 300

    # "S-1" 같이 성공 접두어를 쓰는 형태도 성공
    if rc.startswith("S-"):
        return True

    return False


def parse_tokens_from_authorization(auth_value: str) -> Optional[Tuple[str, str]]:
    # "Bearer {apiKey} {accessToken}"
    if not auth_value:
        return None
    parts = auth_value.strip().split()
    if len(parts) >= 3 and parts[0].lower() == "bearer":
        api_key = parts[1].strip()
        access_token = parts[2].strip()
        if api_key and access_token:
            return api_key, access_token
    return None


def extract_tokens(resp: requests.Response) -> Optional[Tuple[str, str]]:
    # 1) Authorization 헤더 우선
    auth = resp.headers.get("Authorization") or resp.headers.get("authorization")
    if auth:
        got = parse_tokens_from_authorization(auth)
        if got:
            return got

    # 2) 쿠키 폴백
    api_key = resp.cookies.get("apiKey")
    access_token = resp.cookies.get("accessToken")
    if api_key and access_token:
        return api_key, access_token

    # 3) JSON 바디 폴백 (data.apiKey/data.accessToken 또는 apiKey/accessToken)
    try:
        payload = resp.json()
        if isinstance(payload, dict):
            data = payload.get("data")
            if isinstance(data, dict):
                ak = data.get("apiKey")
                at = data.get("accessToken")
                if ak and at:
                    return str(ak), str(at)
            ak = payload.get("apiKey")
            at = payload.get("accessToken")
            if ak and at:
                return str(ak), str(at)
    except Exception:
        pass

    return None


def post_json(session: requests.Session, url: str, payload: Dict[str, Any]) -> Tuple[requests.Response, Optional[Dict[str, Any]]]:
    r = session.post(url, json=payload, timeout=10)
    body = None
    try:
        body = r.json()
    except Exception:
        body = None
    return r, body


def build_join_payload(props: List[str], login_id: str, password: str, nickname: str) -> Dict[str, Any]:
    # 프로젝트에서 흔히 쓰는 필드 후보
    id_field = pick_field(props, ["loginId", "username", "userId", "email", "id", "login_id"])
    pw_field = pick_field(props, ["password", "pw", "pass"])
    nick_field = pick_field(props, ["nickname", "nickName", "name", "displayName"])

    if not id_field or not pw_field:
        raise RuntimeError(f"Join request schema doesn't include recognizable id/password fields: {props}")

    payload: Dict[str, Any] = {
        id_field: login_id,
        pw_field: password,
    }
    if nick_field:
        payload[nick_field] = nickname
    return payload


def build_login_payload(props: List[str], login_id: str, password: str) -> Dict[str, Any]:
    id_field = pick_field(props, ["loginId", "username", "userId", "email", "id", "login_id"])
    pw_field = pick_field(props, ["password", "pw", "pass"])

    if not id_field or not pw_field:
        raise RuntimeError(f"Login request schema doesn't include recognizable id/password fields: {props}")

    return {id_field: login_id, pw_field: password}


def create_and_login_one(
    base: str,
    join_url: str,
    login_url: str,
    join_props: list,
    login_props: list,
    prefix: str,
    idx: int,
) -> tuple[bool, str]:
    login_id = f"{prefix}{idx}_{rand_str(6)}"
    nickname = login_id
    password = rand_password(8, 20)

    s = requests.Session()

    # join (비번 정책 실패 대비 재시도)
    for _ in range(5):
        password = rand_password(8, 20)
        join_payload = build_join_payload(join_props, login_id, password, nickname)
        jr, jb = post_json(s, join_url, join_payload)

        if 200 <= jr.status_code < 300 and (not isinstance(jb, dict) or "resultCode" not in jb or is_success_rsdata(jb)):
            break
    else:
        return False, f"[JOIN FAIL] {login_id} status={jr.status_code} body={jb}"

    # login
    login_payload = build_login_payload(login_props, login_id, password)
    lr, lb = post_json(s, login_url, login_payload)
    if lr.status_code < 200 or lr.status_code >= 300:
        return False, f"[LOGIN FAIL] {login_id} status={lr.status_code} body={lb}"
    if isinstance(lb, dict) and "resultCode" in lb and not is_success_rsdata(lb):
        return False, f"[LOGIN FAIL] {login_id} status={lr.status_code} body={lb}"

    tokens = extract_tokens(lr)
    if not tokens:
        return False, f"[TOKEN FAIL] {login_id} no tokens"

    api_key, access_token = tokens
    return True, f"{api_key} {access_token}"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base-url", required=True, help="ex) http://host.docker.internal:8080")
    ap.add_argument("--count", type=int, default=100)
    ap.add_argument("--out", required=True)
    ap.add_argument("--prefix", default="k6user")
    args = ap.parse_args()

    base = args.base_url.rstrip("/")

    spec = load_openapi(base)

    join_path, join_method, join_op = find_best_endpoint(spec, "join")
    login_path, login_method, login_op = find_best_endpoint(spec, "login")

    if join_method != "post" or login_method != "post":
        raise RuntimeError(f"Expected POST for join/login, got join={join_method}, login={login_method}")

    join_url = f"{base}{join_path}"
    login_url = f"{base}{login_path}"

    # join schema -> props
    join_schema = extract_request_schema(spec, join_op)
    join_props = list(schema_to_props(spec, join_schema).keys())

    # login schema -> props
    login_schema = extract_request_schema(spec, login_op)
    login_props = list(schema_to_props(spec, login_schema).keys())

    print(f"[OPENAPI] join  = POST {join_path}  fields={join_props}")
    print(f"[OPENAPI] login = POST {login_path} fields={login_props}")

    workers = 20  # 10~30 중에 선택. 20부터.
    results: List[str] = []
    fail = 0

    with ThreadPoolExecutor(max_workers=workers) as ex:
        futures = [
            ex.submit(
                create_and_login_one,
                base,
                join_url,
                login_url,
                join_props,
                login_props,
                args.prefix,
                i,
            )
            for i in range(1, args.count + 1)
        ]

        for fu in as_completed(futures):
            ok, msg = fu.result()
            if ok:
                results.append(msg)
            else:
                fail += 1
                print(msg)

    with open(args.out, "w", encoding="utf-8") as f:
        f.write("\n".join(results) + ("\n" if results else ""))

    print(f"[DONE] ok={len(results)} fail={fail} out={args.out}")



if __name__ == "__main__":
    main()
