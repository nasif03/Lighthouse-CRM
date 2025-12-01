#!/usr/bin/env python
"""Entry point script for running the MCP server"""
import sys
import os
from pathlib import Path

backend_dir = Path(__file__).parent.absolute()
os.chdir(backend_dir)

if str(backend_dir) not in sys.path:
    sys.path.insert(0, str(backend_dir))

from mcp_server.server import mcp

if __name__ == "__main__":
    mcp.run()

