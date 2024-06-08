from sklearn.ensemble import RandomForestClassifier
import pandas as pd
import matplotlib.pyplot as plt


# 데이터 불러오기
data = pd.read_csv('../datasets/Final_Data.csv')


# 입력과 타겟 데이터 분리
X = data.drop('url_type', axis=1).values
Y = data['url_type'].values


# 랜덤 포레스트 모델 
model = RandomForestClassifier()


# 모델 학습 진행
model.fit(X,Y)


# 특성 중요도 추출
importance = model.feature_importances_
feature_columns = data.columns[:-1]


# 특성 중요도 출력
plt.barh(range(X.shape[1]), importance, tick_label=feature_columns)
plt.xlabel('Feautre Importance')
plt.ylabel('Features')
plt.show()


