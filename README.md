# VAD-Speaker Recognition-for-Android

1. Demonstration of multi-speakers recognition on Android device.
2. The demo models were uploaded to the drive:: https://drive.google.com/drive/folders/1p_qIVEO5ZPhHAsyxaJsHk4Ykf7cMm5PN?usp=sharing
3. After downloading, place the model into the assets folder.
4. Remember to decompress the *.so zip file stored in the libs/arm64-v8a folder.
5. The demo models, named 'FSMN-VAD' and 'ERes2Net', were converted from ModelScope and underwent code optimizations to achieve extreme execution speed.
6. Therefore, the inputs & outputs of the demo models are slightly different from the original one.
7. We will make the exported method public later.
8. You have to approve the recorder permissions first, and then relaunch the app again.
9. The system will automatically assign a new ID to the unfamiliar voice.
10. You can rename the ID by the specific format. For example: 0.John or 1:John or John/Handsome_John...Use a sign or whitespace to separate the previous ID and new name.
11. The time cost of speaker recognition inference is about 25ms. VAD takes about 2ms.
# VAD-说话人识别-安卓
1. 在安卓设备上展示多说话人识别。
2. 演示模型已上传到云端硬盘：https://drive.google.com/drive/folders/1p_qIVEO5ZPhHAsyxaJsHk4Ykf7cMm5PN?usp=sharing
3. 百度: https://pan.baidu.com/s/1IqJCRzlsFFidXyxf9-954A?pwd=s648 提取码: s648
4. 下载后，将模型放入assets文件夹。
5. 记得解压存储在libs/arm64-v8a文件夹中的*.so zip文件。
6. 演示模型，命名为'FSMN-VAD'和'ERes2Net'，是从ModelScope转换来的，并经过代码优化以实现极致的执行速度。
7. 因此，演示模型的输入和输出与原始模型略有不同。
8. 我们未来会提供转换导出的方法。
9. 首次使用时，您需要先授权录音权限，然后再重新启动应用程序。
10. 系统将自动为陌生的声音分配一个新的ID。
11. 您可以按照特定格式重命名ID。例如：0.John 或 1:John 或 John/Handsome_John...使用标记或空格来分隔之前的ID和新名称。
12. 说话人识别推理的时间消耗约为25ms。VAD大约需要2ms。
# 演示结果 Demo Results
