#!/usr/bin/env python3
"""
EfficientAT mn04_as → ONNX INT8 변환 스크립트

사용법:
    pip install torch onnx onnxruntime
    git clone https://github.com/fschmid56/EfficientAT.git
    cd EfficientAT
    python ../convert_to_onnx.py

출력:
    mn04_as_int8.onnx — Android assets 폴더에 복사하여 사용
"""

import os
import sys
import torch
import torch.nn as nn
import numpy as np

def main():
    # EfficientAT 리포지토리가 현재 디렉토리에 있다고 가정
    # https://github.com/fschmid56/EfficientAT
    sys.path.insert(0, ".")

    try:
        from models.mn.model import get_model as get_mobilenet
    except ImportError:
        print("Error: EfficientAT 리포지토리 디렉토리에서 실행하세요.")
        print("  git clone https://github.com/fschmid56/EfficientAT.git")
        print("  cd EfficientAT")
        print("  python ../convert_to_onnx.py")
        sys.exit(1)

    model_name = "mn04_as"
    output_fp32 = f"{model_name}.onnx"
    output_int8 = f"{model_name}_int8.onnx"

    print(f"[1/4] Loading {model_name} pretrained model...")
    model = get_mobilenet(
        pretrained_name=model_name,
        num_classes=527,
        head_type="mlp",
        se_dims="c",
        width_mult=0.4,
    )
    model.eval()

    param_count = sum(p.numel() for p in model.parameters())
    print(f"       Parameters: {param_count:,} (~{param_count/1e6:.2f}M)")

    # 입력: mel spectrogram [batch, 1, n_mels, time_frames]
    # 16kHz 기준 ~1초 = 100 frames (hop=160, sr=16000)
    dummy_input = torch.randn(1, 1, 128, 100)

    print(f"[2/4] Exporting to ONNX (FP32)...")
    torch.onnx.export(
        model,
        dummy_input,
        output_fp32,
        opset_version=13,
        input_names=["mel_spectrogram"],
        output_names=["logits"],
        dynamic_axes={
            "mel_spectrogram": {0: "batch", 3: "time_frames"},
            "logits": {0: "batch"},
        },
    )
    print(f"       Saved: {output_fp32} ({os.path.getsize(output_fp32)/1024/1024:.2f} MB)")

    # ONNX 모델 검증
    print(f"[3/4] Validating ONNX model...")
    import onnx
    onnx_model = onnx.load(output_fp32)
    onnx.checker.check_model(onnx_model)
    print("       ONNX model is valid ✓")

    # INT8 동적 양자화
    print(f"[4/4] Quantizing to INT8 (dynamic)...")
    from onnxruntime.quantization import quantize_dynamic, QuantType

    quantize_dynamic(
        output_fp32,
        output_int8,
        weight_type=QuantType.QUInt8,
    )
    print(f"       Saved: {output_int8} ({os.path.getsize(output_int8)/1024/1024:.2f} MB)")

    # 추론 테스트
    print("\n[검증] Running inference test...")
    import onnxruntime as ort

    session = ort.InferenceSession(output_int8)
    test_input = np.random.randn(1, 1, 128, 100).astype(np.float32)
    outputs = session.run(None, {"mel_spectrogram": test_input})
    logits = outputs[0]

    # Sigmoid 적용
    probs = 1.0 / (1.0 + np.exp(-logits))

    print(f"       Output shape: {logits.shape}")
    print(f"       Top-5 class indices: {np.argsort(probs[0])[-5:][::-1]}")
    print(f"       Top-5 confidences: {np.sort(probs[0])[-5:][::-1]}")

    # class_labels_indices.csv 다운로드 안내
    print(f"\n[완료]")
    print(f"  1. {output_int8} 파일을 app/src/main/assets/ 에 복사하세요.")
    print(f"  2. AudioSet class_labels_indices.csv 도 assets 에 복사하세요:")
    print(f"     wget http://storage.googleapis.com/us_audioset/youtube_corpus/v1/csv/class_labels_indices.csv")
    print(f"     cp class_labels_indices.csv app/src/main/assets/")

    # FP32 파일 삭제
    if os.path.exists(output_fp32):
        os.remove(output_fp32)
        print(f"\n  (FP32 중간 파일 삭제됨)")


if __name__ == "__main__":
    main()
