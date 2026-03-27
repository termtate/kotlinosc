import argparse
from pythonosc import udp_client


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--target-host", required=True)
    parser.add_argument("--target-port", required=True, type=int)
    parser.add_argument("--path", required=True)
    parser.add_argument("--int", dest="int_value", required=True, type=int)
    parser.add_argument("--text", required=True)
    args = parser.parse_args()

    client = udp_client.SimpleUDPClient(args.target_host, args.target_port)
    client.send_message(args.path, [args.int_value, args.text])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
