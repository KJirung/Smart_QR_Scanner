from sklearn.linear_model import LogisticRegression
from sklearn.metrics import roc_auc_score
from sklearn.preprocessing import StandardScaler
from sklearn.model_selection import train_test_split
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np


# 데이터 불러오기
data = pd.read_csv('../datasets/Final_Data')

# label열을 제외한 나머지 열의 이름 추출
feature_names = data.columns[0:-1]


# 특성과 label 분리
X = data.drop('url_type', axis=1).values
Y = data['url_type'].values


# train-test 데이터 분할 (80%, 20%)
X_train, X_test, Y_train, Y_test = train_test_split(X, Y, test_size=0.2, random_state=42)


# 로지스틱 회귀 모델
model = LogisticRegression()


# 모델 학습 진행
model.fit(X_train,Y_train)


# 테스트 데이터에 대한 성능 측정
Y_pred = model.predict(X_test)
auc_score = roc_auc_score(Y_test, Y_pred)

print(f"AUC score : {auc_score}")


# 학습된 모델의 계수 확인
coef = model.coef_[0]
intercept = model.intercept_


# Odd ratio 계산
odd_ratio = np.exp(coef)


# Odd ratio 출력
for i, name in enumerate(feature_names):
    print("Feature:", name)
    print("Odd ratio:", np.round(odd_ratio[i],2))
    print("----------------------------------------")



