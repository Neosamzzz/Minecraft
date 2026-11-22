#!/usr/bin/env python3
"""Double-click to publish the local NewEarth client's mod files to OSS."""

import sys

from publish import main


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, RuntimeError) as error:
        sys.exit("发布失败：" + str(error))
    finally:
        if len(sys.argv) == 1 and sys.stdin.isatty():
            try:
                input("按 Enter 关闭窗口…")
            except (EOFError, KeyboardInterrupt):
                pass
