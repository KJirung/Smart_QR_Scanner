import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
import tensorflow as tf
from tensorflow.keras.models import Sequential, load_model
from tensorflow.keras.layers import Dense, Dropout
from tensorflow.keras.metrics import Precision, Recall
from keras import backend as K
from tensorflow.keras.callbacks import ModelCheckpoint
import matplotlib.pyplot as plt



# 데이터 불러오기
df = pd.read_csv('../../datasets/Final_data.csv')


# 입력과 타겟 데이터 분리
X = df.drop('url_type', axis=1).values
Y = df['url_type'].values


# 훈련 데이터와 테스트 데이터 분리
x_train, x_test_val, y_train, y_test_val = train_test_split(X, Y, test_size=0.3)
x_val, x_test, y_val, y_test = train_test_split(x_test_val, y_test_val, test_size=0.5)


# Sequential 모델 생성
model = Sequential()

# 입력 레이어 및 첫 번째 은닉 레이어 추가
model.add(Dense(128, input_dim=7, activation='relu'))  
model.add(Dropout(0.5))  

model.add(Dense(64, activation='relu'))  
model.add(Dropout(0.5))  

# 세 번째 은닉 레이어 추가
model.add(Dense(32, activation='relu'))  
model.add(Dropout(0.5)) 

# 출력 레이어 추가
model.add(Dense(1, activation='sigmoid'))  

# 옵티마이저 설정
optimizer_ = optimizer=tf.keras.optimizers.Adam(learning_rate=0.001)

# 각각 정밀도, 재현율
precision = Precision(name='precision')
recall = Recall(name='recall')

# F1 Score 계산 함수
def f1_score(y_true, y_pred):
    precision_val = precision(y_true, y_pred)
    recall_val = recall(y_true, y_pred)
    return 2 * ((precision_val * recall_val) / (precision_val + recall_val + K.epsilon()))

# 모델 컴파일
model.compile(loss='binary_crossentropy', optimizer=optimizer_, metrics=['accuracy', precision, recall, f1_score])


# 학습 과정 중 성능 개선시 모델 파일 저장
checkpoint_path = "../pretrained_models/h5/best_model.h5"
checkpoint_callback = ModelCheckpoint(filepath=checkpoint_path,
                                      save_best_only=True,
                                      monitor='val_f1_score',  
                                      mode='max',  
                                      verbose=1)

# 학습 진행
history = model.fit(x_train, y_train, epochs=50, batch_size=32, validation_data=(x_val, y_val), callbacks=[checkpoint_callback])



# 시각화
fig, axs = plt.subplots(1, 3, figsize=(12, 5))

# 손실 그래프
axs[0].plot(range(1, len(history.history["loss"]) + 1), history.history["loss"], color='blue', label='Training')
axs[0].plot(range(1, len(history.history["val_loss"]) + 1), history.history["val_loss"], color='red', label='Validation')
axs[0].set_title("Loss")
axs[0].set_ylabel("Loss")
axs[0].set_xlabel("Epoch")
axs[0].legend()

# 정확도 그래프
axs[1].plot(range(1, len(history.history["accuracy"]) + 1), history.history["accuracy"], color='blue', label='Training')
axs[1].plot(range(1, len(history.history["val_accuracy"]) + 1), history.history["val_accuracy"], color='red', label='Validation')
axs[1].set_title("Accuracy")
axs[1].set_ylabel("Accuracy")
axs[1].set_xlabel("Epoch")
axs[1].legend()

# F1 스코어 그래프
axs[2].plot(range(1, len(history.history["f1_score"]) + 1), history.history["f1_score"], color='blue', label='Training')
axs[2].plot(range(1, len(history.history["val_f1_score"]) + 1), history.history["val_f1_score"], color='red', label='Validation')
axs[2].set_title("F1 Score")
axs[2].set_ylabel("F1 Score")
axs[2].set_xlabel("Epoch")
axs[2].legend()

plt.tight_layout()

plt.show()


# 학습 과정 중 가장 성능이 좋은 모델 선택
best_model = load_model("../pretrained_models/h5/best_model.h5")


# 테스트 데이터에 대한 성능 측정
loss, accuracy, precision, recall, f1_score = best_model.evaluate(x_test, y_test)


# 결과 출력
print(f'Test Loss: {loss:.4f}')
print(f'Test Accuracy: {accuracy:.4f}')
print(f'Test Precision: {precision:.4f}')
print(f'Test Recall: {recall:.4f}')
print(f'Test F1 Score: {f1_score:.4f}')

