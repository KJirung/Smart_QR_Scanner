import tensorflow as tf

# TensorFlow Lite 모델 파일 경로
tflite_model_path = 'C:/Users/sscp2/Desktop/QR_Code_Scanner/app/src/main/assets/best_model.tflite'

# TensorFlow Lite 모델 로드
interpreter = tf.lite.Interpreter(model_path=tflite_model_path)
interpreter.allocate_tensors()

# 입력 및 출력 텐서 정보 출력
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

print("입력 텐서 정보:")
for input_detail in input_details:
    print(input_detail)

print("\n출력 텐서 정보:")
for output_detail in output_details:
    print(output_detail)
