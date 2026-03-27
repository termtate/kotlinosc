import argparse
import json
import sys
from typing import Any

from pythonosc import dispatcher, osc_server


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--bind", required=True)
    parser.add_argument("--port", required=True, type=int)
    parser.add_argument("--path", required=False, default="")
    parser.add_argument("--out", required=True)
    parser.add_argument("--ready", required=True)
    parser.add_argument("--timeout-ms", type=int, default=5000)
    args = parser.parse_args()

    captured: dict[str, Any] = {"received": False}

    def handler(address: str, *osc_args):
        captured["received"] = True
        captured["address"] = address
        captured["args"] = list(osc_args)

    disp = dispatcher.Dispatcher()
    disp.set_default_handler(handler)
    if args.path:
        disp.map(args.path, handler)

    server = osc_server.BlockingOSCUDPServer((args.bind, args.port), disp)
    server.timeout = args.timeout_ms / 1000.0
    with open(args.ready, "w", encoding="utf-8") as f:
        f.write("READY\n")

    try:
        server.handle_request()
    finally:
        server.server_close()

    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(captured, f)

    if not captured["received"]:
        print("No OSC packet received before timeout", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
