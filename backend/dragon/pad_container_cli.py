from __future__ import annotations

import argparse
import json

from pad_container_db import export_battle_snapshot, get_status, init_db, token_for


def j(data):
    print(json.dumps(data, indent=2, ensure_ascii=False))


def main():
    parser = argparse.ArgumentParser(description="DigiPad Container CLI")
    sub = parser.add_subparsers(dest="cmd", required=True)

    sub.add_parser("seed")

    t = sub.add_parser("token")
    t.add_argument("profile_id")

    st = sub.add_parser("status")
    st.add_argument("profile_id")

    ex = sub.add_parser("export")
    ex.add_argument("profile_id")

    args = parser.parse_args()

    if args.cmd == "seed":
        j(init_db())
    elif args.cmd == "token":
        token = token_for(args.profile_id)
        if not token:
            raise SystemExit("unknown profile")
        print(token)
    elif args.cmd == "status":
        j(get_status(args.profile_id))
    elif args.cmd == "export":
        j(export_battle_snapshot(args.profile_id))


if __name__ == "__main__":
    main()
