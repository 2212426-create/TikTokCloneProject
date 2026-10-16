# -*- coding: utf-8 -*-
"""
Chạy RecSys API và mở Cloudflare Quick Tunnel, rồi ghi URL công khai vào
Firebase RTDB server_config/recsys_url để app TopTop tự phát hiện.

    venv\\Scripts\\python.exe -m src.api.tunnel --port 8000
Cần có lệnh `cloudflared` trong PATH (hoặc --cloudflared ĐƯỜNG_DẪN).
Project: ViFarm (services/recsys)
"""

import argparse
import re
import subprocess
import threading
import time

import uvicorn

from src.api.firebase_bridge import publish_server_url

TUNNEL_URL = re.compile(r"https://[a-z0-9-]+\.trycloudflare\.com")


def start_tunnel(port, cloudflared="cloudflared", timeout=60):
    """Khởi động cloudflared và đọc URL công khai từ log của nó."""
    proc = subprocess.Popen([cloudflared, "tunnel", "--url", f"http://127.0.0.1:{port}"],
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    deadline = time.time() + timeout
    while time.time() < deadline:
        line = proc.stdout.readline()
        if not line and proc.poll() is not None:
            break
        match = TUNNEL_URL.search(line)
        if match:
            # Tiếp tục xả log để pipe không bị đầy.
            threading.Thread(target=lambda: [None for _ in proc.stdout], daemon=True).start()
            return proc, match.group(0)
    proc.kill()
    raise RuntimeError("cloudflared không trả về URL công khai")


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=8000)
    parser.add_argument("--cloudflared", default="cloudflared")
    parser.add_argument("--no-publish", action="store_true", help="Không ghi URL lên Firebase RTDB")
    args = parser.parse_args(argv)

    server = uvicorn.Server(uvicorn.Config("src.api.app:app", host="127.0.0.1", port=args.port))
    threading.Thread(target=server.run, daemon=True).start()
    deadline = time.time() + 30
    while not server.started:
        if time.time() > deadline:
            raise RuntimeError("Uvicorn không khởi động được")
        time.sleep(0.2)

    proc, url = start_tunnel(args.port, args.cloudflared)
    print(f"Public URL: {url}  (Swagger: {url}/docs)", flush=True)
    if not args.no_publish:
        publish_server_url(url)
        print("Đã ghi URL vào server_config/recsys_url", flush=True)
    try:
        proc.wait()
    except KeyboardInterrupt:
        proc.terminate()


if __name__ == "__main__":
    main()
