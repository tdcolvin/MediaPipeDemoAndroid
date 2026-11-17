# MediaPipe Demo on Android

Example of using MediaPipe for:
* Multi-modal LLM inference (Gemma 3n)
* Image classification (efficientnet-lite)
* Hand gesture recognition via streaming video
* Image-to-image generation

Everything works on-device.

## Requirements
This uses Gemma 4, the E4B parameter version. You probably need a phone with 8GB RAM.

## Setup
To make this work:
1. Download Gemma 4 here: [gemma-4-E4B-it-litert-lm](https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/blob/main/gemma-4-E4B-it-gpu.litertlm)
2. Copy it to your device using ADB: `adb push gemma-4-E4B-it-gpu.litertlm /data/local/tmp/llm/gemma-4-E4B-it-gpu.litertlm`

That location is specified in the `init` of TerriblePoemViewModel.

## Help
Open an issue if you have trouble. If you want to engage me to write something like this for your business, then I'm available as an [Android freelancer](https://tomcolvin.co.uk/) or through my agency [Apptaura](https://apptaura.com/).

## About me
Hello! I'm Tom Colvin, freelance Android app developer based in London.
