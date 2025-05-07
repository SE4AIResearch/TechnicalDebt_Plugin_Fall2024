# SATD Tool Plugin
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)


<!-- Plugin description -->
The Technical Debt Plugin helps you identify and resolve Self-Admitted Technical Debt (SATD) in your Java codebase at class, method, and line levels.
Key features include:
* Automated SATD detection
* Visualization of technical debt
* Code improvement suggestions
* Debt prioritization tool


IntelliJ Versions Supported:
2023.3 - 2025.1

<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "TechnicalDebt_Plugin_Fall2024"</kbd> >
  <kbd>Install</kbd>
  
- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/SE4AIResearch/TechnicalDebt_Plugin_Fall2024/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---


# Large Language Models
# OpenAI

- Sign up for OpenAI at https://auth.openai.com/create-account
- Get your OpenAI API key (Video Tutorial https://www.youtube.com/watch?v=SzPE_AE0eEo)
- Go to Settings | Tools | Large Language Models and enter your API key in the "OpenAI Key" field. If you are a member
  of only one organization, leave the "OpenAI Organization" field empty

# Gemini
- Sign up for OpenAI at https://aistudio.google.com/apikey
- Get your Gemini API key (Video Tutorial https://www.youtube.com/watch?v=T1BTyo1A4Ww)
- Go to Settings | Tools | Large Language Models and enter your API key in the "Gemini Key" field.

# Ollama

## What is Ollama?

Ollama is a lightweight framework for running large language models (LLMs) locally on your machine. It allows you to download, run, and customize various open-source models like Llama 3.2, deepseek-r1 and many others.

## System Requirements

- **Memory**: At least 8GB RAM for 7B parameter models, 16GB for 13B models, and 32GB+ for larger models
- **Storage**: Varies by model (1.3GB-400GB+ depending on model size)
- **GPU**: Optional but recommended for better performance
  - NVIDIA GPU with CUDA support
  - AMD GPU with ROCm support (Linux only)
  - Apple Silicon (built-in Metal acceleration)

## Installation

### macOS

```bash
# Download and install
curl -fsSL https://ollama.com/download/Ollama-darwin.zip -o Ollama-darwin.zip
unzip Ollama-darwin.zip
mv Ollama.app /Applications/

# Or use the one-click installer from https://ollama.com/download
```

### Windows
(Video Link: https://youtu.be/-ow4-7YdzNc)
```bash
# Download and run the installer from
# https://ollama.com/download/OllamaSetup.exe
```

### Linux

```bash
# Quick install
curl -fsSL https://ollama.com/install.sh | sh

# Manual install for x86_64
curl -L https://ollama.com/download/ollama-linux-amd64.tgz -o ollama-linux-amd64.tgz
sudo tar -C /usr -xzf ollama-linux-amd64.tgz

# Manual install for ARM64
curl -L https://ollama.com/download/ollama-linux-arm64.tgz -o ollama-linux-arm64.tgz
sudo tar -C /usr -xzf ollama-linux-arm64.tgz

# For AMD GPU support, also install:
curl -L https://ollama.com/download/ollama-linux-amd64-rocm.tgz -o ollama-linux-amd64-rocm.tgz
sudo tar -C /usr -xzf ollama-linux-amd64-rocm.tgz
```

### Docker

```bash
# Pull the official Ollama image
docker pull ollama/ollama

# Run Ollama container
docker run -d -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama
```

## Starting the Ollama Server

### macOS/Windows

1. Launch the Ollama application
2. The server will start automatically (default address: localhost:11434)

### Linux/Manual Start

```bash
# Start the Ollama server
ollama serve
```

### Run as a Systemd Service (Linux)

Create a user and group for Ollama:

```bash
sudo useradd -r -s /bin/false -U -m -d /usr/share/ollama ollama
sudo usermod -a -G ollama $(whoami)
```

Create a service file at `/etc/systemd/system/ollama.service`:

```ini
[Unit]
Description=Ollama Service
After=network-online.target

[Service]
ExecStart=/usr/bin/ollama serve
User=ollama
Group=ollama
Restart=always
RestartSec=3
Environment="PATH=$PATH"

[Install]
WantedBy=default.target
```

Enable and start the service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable ollama
sudo systemctl start ollama
```

## Using Ollama

### Pull a Model

```bash
ollama pull llama3.2
```

### Run a Model

```bash
ollama run llama3.2
```

### Chat with Multimodal Models

```bash
ollama run llama3.2-vision "What's in this image? /path/to/image.jpg"
```

### List Available Models

```bash
ollama list
```

### View Currently Running Models

```bash
ollama ps
```

### Stop a Running Model

```bash
ollama stop <model-name>
```

## Updating Ollama

### Linux

```bash
curl -fsSL https://ollama.com/install.sh | sh
```

### macOS/Windows

Update to the latest version from the [Ollama website](https://ollama.com/download).

## Troubleshooting

- **GPU not detected**: Ensure proper drivers are installed
  - NVIDIA: Check with `nvidia-smi`
  - AMD: Verify ROCm installation
- **Memory errors**: Try a smaller model or increase system swap
- **Port conflicts**: Change the port with `OLLAMA_HOST` environment variable

## Resources

- [Official Documentation](https://github.com/ollama/ollama)
- [Model Library](https://ollama.com/library)
- [API Documentation](https://github.com/ollama/ollama/blob/main/docs/api.md)
- [Community Discord](https://discord.gg/ollama)
- [Reddit Community](https://reddit.com/r/ollama)
