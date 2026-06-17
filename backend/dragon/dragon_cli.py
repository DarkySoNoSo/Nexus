from __future__ import annotations

import argparse
import json
from dragon_db import apply_action, codex, get_state, init_db

def print_json(data):
    print(json.dumps(data, indent=2, ensure_ascii=False))

def main():
    parser = argparse.ArgumentParser(description="Digi Dragon local core CLI")
    sub = parser.add_subparsers(dest="command", required=True)

    sub.add_parser("seed")
    sub.add_parser("status")
    sub.add_parser("feed")
    sub.add_parser("care")
    sub.add_parser("freefight")
    sub.add_parser("arena")
    sub.add_parser("evolve")
    sub.add_parser("codex")

    train = sub.add_parser("train")
    train.add_argument("training_type", nargs="?", default="focus", choices=["strength", "endurance", "speed", "focus", "instinct"])

    args = parser.parse_args()

    if args.command == "seed":
        print_json(init_db())
    elif args.command == "status":
        print_json({"ok": True, "state": get_state()})
    elif args.command == "feed":
        print_json(apply_action("feed"))
    elif args.command == "care":
        print_json(apply_action("care"))
    elif args.command == "train":
        print_json(apply_action("train", {"training_type": args.training_type}))
    elif args.command == "freefight":
        print_json(apply_action("freefight"))
    elif args.command == "arena":
        print_json(apply_action("arena"))
    elif args.command == "evolve":
        print_json(apply_action("evolve"))
    elif args.command == "codex":
        print_json(codex())

if __name__ == "__main__":
    main()
