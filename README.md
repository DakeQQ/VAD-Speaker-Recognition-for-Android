---

# VAD-Speaker Recognition for Android

## Overview

This project demonstrates multi-speaker recognition on Android devices, using optimized models to achieve high-speed performance.

## Getting Started

1. **Download the Models:**
   - The demo models are available for download [here](https://drive.google.com/drive/folders/1p_qIVEO5ZPhHAsyxaJsHk4Ykf7cMm5PN?usp=sharing).

2. **Setup:**
   - After downloading, place the models into the `assets` folder.
   - Decompress the `*.so` zip file stored in the `libs/arm64-v8a` folder.

3. **Model Information:**
   - The demo models, named **FSMN-VAD** and **ERes2Net**, are converted from ModelScope and have undergone code optimizations for extreme execution speed.
   - Note that the inputs and outputs of these demo models differ slightly from the original versions.

4. **ONNX Runtime Adaptation:**
   - To better adapt to ONNX Runtime on Android, dynamic axes were not used during export. As a result, the exported ONNX model may not be optimal for x86_64 systems.
   - We plan to make the export method public in the future.

5. **Permissions:**
   - Ensure to approve the recorder permissions first, then relaunch the app.

6. **Speaker Recognition:**
   - The system will automatically assign a new ID to unfamiliar voices.
   - You can rename the ID using a specific format, such as `0.John`, `1:John`, or `John/Handsome_John`. Use a sign or whitespace to separate the previous ID from the new name.

7. **Performance:**
   - The time cost for speaker recognition inference is approximately 25ms, while VAD takes about 2ms.

## Additional Resources

- Explore more projects: [https://dakeqq.github.io/overview/](https://dakeqq.github.io/overview/)

## 演示结果 Demo Results

![Demo Animation](https://github.com/DakeQQ/VAD-Speaker-Recognition-for-Android/blob/main/vad_sr.gif?raw=true)

---

# VAD-说话人识别-安卓

## 概述

该项目展示了在安卓设备上进行多说话人识别，使用经过优化的模型以实现高速度性能。

## 快速开始

1. **下载模型：**
   - 演示模型已上传到云端硬盘：[点击这里下载](https://drive.google.com/drive/folders/1p_qIVEO5ZPhHAsyxaJsHk4Ykf7cMm5PN?usp=sharing)
   - 百度链接: [点击这里](https://pan.baidu.com/s/1IqJCRzlsFFidXyxf9-954A?pwd=s648) 提取码: s648

2. **设置：**
   - 下载后，将模型放入`assets`文件夹。
   - 解压存储在`libs/arm64-v8a`文件夹中的`*.so` zip文件。

3. **模型信息：**
   - 演示模型，命名为**FSMN-VAD**和**ERes2Net**，是从ModelScope转换而来，并经过代码优化以实现极致的执行速度。
   - 因此，演示模型的输入和输出与原始模型略有不同。

4. **ONNX Runtime 适配：**
   - 为了更好地适配ONNX Runtime-Android，导出时未使用动态轴。因此，导出的ONNX模型对x86_64可能不是最佳选择。
   - 我们计划在未来公开转换导出的方法。

5. **权限：**
   - 首次使用时，您需要先授权录音权限，然后再重新启动应用程序。

6. **说话人识别：**
   - 系统将自动为陌生的声音分配一个新的ID。
   - 您可以按照特定格式重命名ID，例如：`0.John` 或 `1:John` 或 `John/Handsome_John`。使用标记或空格来分隔之前的ID和新名称。

7. **性能：**
   - 说话人识别推理的时间消耗约为25ms，VAD大约需要2ms。

## 其他资源

- 看更多项目: [https://dakeqq.github.io/overview/](https://dakeqq.github.io/overview/)

---
