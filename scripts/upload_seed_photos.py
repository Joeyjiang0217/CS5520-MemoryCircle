"""Upload local photos to Firebase Storage so SeedRepository can hand out
real Firebase download URLs instead of picsum URLs that fail to load.

Run once locally:
    python scripts/upload_seed_photos.py

The script:
  1. Signs in as 1@test.com (password 123456) via the Firebase Auth REST API.
  2. Uploads every file under PHOTO_DIR to seed_photos/{N}.{ext} in the
     project's Storage bucket using the Firebase Storage REST API. Each
     upload returns a downloadTokens value that lets anyone with the URL
     read the file, regardless of storage rules.
  3. Prints the resulting URLs as a Kotlin list literal that can be pasted
     into SeedRepository.kt.

Requirements: Python 3.8+, `requests` (pip install requests).
"""

import json
import os
import sys
import urllib.parse
from pathlib import Path

try:
    import requests
except ImportError:
    print("ERROR: pip install requests", file=sys.stderr)
    sys.exit(1)

API_KEY = "AIzaSyCHr2r4YDVH0k_AzrQU6VoSktRRjbQFYbs"
BUCKET = "cs5520-group15-memorycircle.firebasestorage.app"
EMAIL = "1@test.com"
PASSWORD = "123456"
PHOTO_DIR = Path(r"C:\Users\Joe\Downloads\photo")
STORAGE_PREFIX = "seed_photos"

EXT_TO_MIME = {
    ".jpg":  "image/jpeg",
    ".jpeg": "image/jpeg",
    ".jfif": "image/jpeg",
    ".png":  "image/png",
    ".webp": "image/webp",
    ".avif": "image/avif",
    ".gif":  "image/gif",
}


def sign_in() -> str:
    url = f"https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key={API_KEY}"
    resp = requests.post(url, json={
        "email": EMAIL,
        "password": PASSWORD,
        "returnSecureToken": True,
    }, timeout=30)
    if not resp.ok:
        print(f"Sign-in failed: {resp.status_code} {resp.text}", file=sys.stderr)
        sys.exit(2)
    return resp.json()["idToken"]


def upload(path: Path, storage_path: str, id_token: str) -> str:
    mime = EXT_TO_MIME.get(path.suffix.lower(), "application/octet-stream")
    encoded = urllib.parse.quote(storage_path, safe="")
    url = f"https://firebasestorage.googleapis.com/v0/b/{BUCKET}/o?name={encoded}"
    with path.open("rb") as f:
        body = f.read()
    resp = requests.post(
        url,
        data=body,
        headers={
            "Authorization": f"Bearer {id_token}",
            "Content-Type": mime,
        },
        timeout=120,
    )
    if not resp.ok:
        raise RuntimeError(f"upload failed for {path.name}: {resp.status_code} {resp.text}")
    payload = resp.json()
    token = payload.get("downloadTokens")
    if not token:
        raise RuntimeError(f"no downloadTokens in response for {path.name}: {payload}")
    return f"https://firebasestorage.googleapis.com/v0/b/{BUCKET}/o/{encoded}?alt=media&token={token}"


def main() -> None:
    if not PHOTO_DIR.is_dir():
        print(f"ERROR: {PHOTO_DIR} not found", file=sys.stderr)
        sys.exit(1)
    files = sorted(p for p in PHOTO_DIR.iterdir() if p.is_file())
    if not files:
        print(f"ERROR: no files in {PHOTO_DIR}", file=sys.stderr)
        sys.exit(1)

    print(f"Signing in as {EMAIL} ...", file=sys.stderr)
    token = sign_in()
    print(f"  OK", file=sys.stderr)

    results = []
    for i, p in enumerate(files):
        ext = p.suffix.lower()
        if ext == ".jfif":
            ext = ".jpg"
        elif ext not in EXT_TO_MIME:
            ext = ".jpg"
        storage_path = f"{STORAGE_PREFIX}/{i}{ext}"
        print(f"[{i+1:2d}/{len(files)}] {p.name} -> {storage_path}", file=sys.stderr)
        url = upload(p, storage_path, token)
        results.append((i, p.name, url))

    out_path = Path(__file__).with_name("seed_photo_urls.json")
    out_path.write_text(json.dumps(
        [{"index": i, "src": name, "url": u} for i, name, u in results],
        indent=2,
    ), encoding="utf-8")
    print(f"\nWrote {len(results)} URLs to {out_path}", file=sys.stderr)

    print("\n// --- paste into SeedRepository.kt ---")
    print("private val SEED_PHOTO_URLS: List<String> = listOf(")
    for _, name, u in results:
        print(f'    "{u}",  // {name}')
    print(")")


if __name__ == "__main__":
    main()
