import tensorflow as tf
from tensorflow.keras.metrics import Precision, Recall
from keras import backend as K
from keras.models import load_model


# H5 모델 파일 경로
h5_model_path = 'pretrained_models/h5/best_model.h5'

# 사용자 정의 메트릭 및 함수 정의
precision = Precision(name='precision')
recall = Recall(name='recall')

def f1_score(y_true, y_pred):
    precision_val = precision(y_true, y_pred)
    recall_val = recall(y_true, y_pred)
    return 2 * ((precision_val * recall_val) / (precision_val + recall_val + K.epsilon()))

# 사용자 정의 메트릭 함수를 등록하여 모델 로드
custom_objects = {'f1_score': f1_score}
model = load_model(h5_model_path, custom_objects=custom_objects)

# tflite 변환기 생성
converter = tf.lite.TFLiteConverter.from_keras_model(model)

# tflite 모델로 변환
tflite_model = converter.convert()

# 변환된 모델을 파일로 저장
tflite_model_path = 'pretrained_models/tflite/best_model.tflite'
with open(tflite_model_path, 'wb') as f:
    f.write(tflite_model)


