# Claude Desktop Configuration Guide

This guide will help you configure Claude Desktop to use the Lighthouse CRM MCP server.

## Prerequisites

1. **Python Environment**: Make sure Python is installed and accessible from command line
2. **Dependencies Installed**: Run `pip install -r requirements.txt` in the Backend directory
3. **Environment Variables**: Ensure your `.env` file is configured (MongoDB, Firebase, etc.)

## Step 1: Find Your Project Path

First, note the absolute path to your `Lighthouse-CRM/Backend` directory.

**Windows Example:**
```
D:\projects\CSE327\Lighthouse-CRM\Backend
```

**Mac/Linux Example:**
```
/Users/yourname/projects/CSE327/Lighthouse-CRM/Backend
```

## Step 2: Locate Claude Desktop Config File

### Windows
The config file is located at:
```
%APPDATA%\Claude\claude_desktop_config.json
```

Or navigate to:
```
C:\Users\YOUR_USERNAME\AppData\Roaming\Claude\claude_desktop_config.json
```

### macOS
```
~/Library/Application Support/Claude/claude_desktop_config.json
```

### Linux
```
~/.config/Claude/claude_desktop_config.json
```

## Step 3: Create or Edit the Config File

1. **If the file doesn't exist**, create it with this content:

```json
{
  "mcpServers": {
    "lighthouse-crm": {
      "command": "python",
      "args": [
        "-m",
        "mcp.server"
      ],
      "cwd": "D:\\projects\\CSE327\\Lighthouse-CRM\\Backend",
      "env": {}
    }
  }
}
```

2. **If the file already exists**, add the `lighthouse-crm` entry to the existing `mcpServers` object:

```json
{
  "mcpServers": {
    "existing-server": {
      ...
    },
    "lighthouse-crm": {
      "command": "python",
      "args": [
        "-m",
        "mcp.server"
      ],
      "cwd": "D:\\projects\\CSE327\\Lighthouse-CRM\\Backend",
      "env": {}
    }
  }
}
```

### Important Notes:

- **Windows Path Format**: Use double backslashes `\\` or forward slashes `/` in the path
  - ✅ Correct: `"D:\\projects\\CSE327\\Lighthouse-CRM\\Backend"`
  - ✅ Also correct: `"D:/projects/CSE327/Lighthouse-CRM/Backend"`
  - ❌ Wrong: `"D:\projects\CSE327\Lighthouse-CRM\Backend"` (single backslashes)

- **Replace the path**: Change `D:\\projects\\CSE327\\Lighthouse-CRM\\Backend` to your actual path

- **Python Command**: If `python` doesn't work, try:
  - `python3` (Mac/Linux)
  - `py` (Windows if Python Launcher is installed)
  - Full path: `"C:\\Python\\python.exe"` (Windows)

## Step 4: Add Environment Variables (Optional)

If you need to pass environment variables to the MCP server, add them to the `env` object:

```json
{
  "mcpServers": {
    "lighthouse-crm": {
      "command": "python",
      "args": [
        "-m",
        "mcp.server"
      ],
      "cwd": "D:\\projects\\CSE327\\Lighthouse-CRM\\Backend",
      "env": {
        "MONGO_URI": "your_mongo_uri",
        "FIREBASE_PROJECT_ID": "your_project_id"
      }
    }
  }
}
```

**Note**: If you're using a `.env` file, you don't need to add these here (the MCP server will load from `.env` automatically).

## Step 5: Restart Claude Desktop

1. **Completely quit Claude Desktop** (not just close the window)
   - Windows: Right-click the system tray icon → Quit
   - Mac: Cmd+Q or right-click dock icon → Quit
   - Linux: Close all windows and ensure process is terminated

2. **Reopen Claude Desktop**

3. **Verify Connection**: 
   - Open a new chat in Claude Desktop
   - You should see MCP tools available
   - Try asking: "What tools do you have available for Lighthouse CRM?"

## Step 6: Test the Connection

Try these commands in Claude Desktop:

1. **List available tools:**
   ```
   What CRM tools do you have access to?
   ```

2. **Create a test lead:**
   ```
   Create a lead for John Doe, email john@example.com, from LinkedIn
   ```

3. **Get dashboard stats:**
   ```
   Show me the dashboard statistics for my CRM
   ```

## Troubleshooting

### Issue: "MCP server not found" or "Command failed"

**Solutions:**
1. **Check Python path**: Make sure Python is in your system PATH
   - Test: Open terminal and run `python --version`
   - If it fails, use full path to Python in config

2. **Check working directory**: Verify the `cwd` path is correct
   - The path should point to `Lighthouse-CRM/Backend` directory
   - Use absolute path, not relative

3. **Check Python module**: Test if the module can be imported
   ```bash
   cd D:\projects\CSE327\Lighthouse-CRM\Backend
   python -m mcp.server
   ```
   If this fails, check that all dependencies are installed.

### Issue: "Authentication required" errors

**Solution:**
- The MCP server requires Firebase authentication
- You need to provide a Firebase token in the request context
- For now, you can set `REQUIRE_AUTH=false` in `mcp/config.py` for testing (development only!)

### Issue: "Module not found" errors

**Solutions:**
1. **Install dependencies:**
   ```bash
   cd Lighthouse-CRM/Backend
   pip install -r requirements.txt
   ```

2. **Check virtual environment**: If using a virtual environment, activate it first or use the venv's Python path

### Issue: Claude Desktop doesn't show MCP tools

**Solutions:**
1. **Check config file syntax**: Ensure JSON is valid (no trailing commas, proper quotes)
   - Use a JSON validator: https://jsonlint.com/

2. **Check Claude Desktop logs:**
   - Windows: `%APPDATA%\Claude\logs\`
   - Mac: `~/Library/Logs/Claude/`
   - Look for MCP-related errors

3. **Restart Claude Desktop completely**: Make sure it's fully closed before reopening

### Issue: "Database connection failed"

**Solution:**
- Ensure MongoDB is running and accessible
- Check `MONGO_URI` in your `.env` file
- Verify network connectivity to MongoDB

## Alternative: Using Virtual Environment

If you're using a virtual environment, point to the venv's Python:

**Windows:**
```json
{
  "mcpServers": {
    "lighthouse-crm": {
      "command": "D:\\projects\\CSE327\\Lighthouse-CRM\\Backend\\venv\\Scripts\\python.exe",
      "args": ["-m", "mcp.server"],
      "cwd": "D:\\projects\\CSE327\\Lighthouse-CRM\\Backend"
    }
  }
}
```

**Mac/Linux:**
```json
{
  "mcpServers": {
    "lighthouse-crm": {
      "command": "/path/to/venv/bin/python",
      "args": ["-m", "mcp.server"],
      "cwd": "/path/to/Lighthouse-CRM/Backend"
    }
  }
}
```

## Example Full Configuration

Here's a complete example configuration file:

```json
{
  "mcpServers": {
    "lighthouse-crm": {
      "command": "python",
      "args": [
        "-m",
        "mcp.server"
      ],
      "cwd": "D:\\projects\\CSE327\\Lighthouse-CRM\\Backend",
      "env": {
        "PYTHONPATH": "D:\\projects\\CSE327\\Lighthouse-CRM\\Backend"
      }
    }
  }
}
```

## Verification Checklist

- [ ] Config file created/updated with correct path
- [ ] Path uses double backslashes (Windows) or forward slashes
- [ ] Python command is correct (`python`, `python3`, or full path)
- [ ] Dependencies installed (`pip install -r requirements.txt`)
- [ ] Claude Desktop completely restarted
- [ ] MongoDB is running and accessible
- [ ] `.env` file is configured correctly

## Next Steps

Once configured, you can use natural language commands like:

- "Create a lead for Jane Smith, email jane@example.com, from website"
- "Show me all deals in the prospecting stage"
- "Convert the top 3 qualified leads to deals"
- "Send an email to john@example.com about our new product"
- "Create a Jira issue for ticket TKT-20241201-0001"
- "What's my conversion rate this month?"

Enjoy using AI-powered CRM management! 🚀

