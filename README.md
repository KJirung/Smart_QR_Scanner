# 📱 Smart QR Scanner

> AI-based Android QR scanning application that detects malicious URLs using a deep learning model and provides safe-browsing alerts to users.  
> 딥러닝 기반 악성 URL 탐지 모델을 적용하여 QR 코드 스캔 시 악성 사이트 여부를 판별하고 사용자에게 안전 정보를 제공하는 안드로이드 앱입니다.

---

## 📌 Overview (개요)

- Project Introduction  
- Motivation  
- Tech Stack  
- System Flow

---

## 💡 Project Introduction (프로젝트 소개)

This project extends the functionality of traditional QR scanners by integrating a **deep learning-based malicious URL detection model**.  
When a user scans a QR code, the app automatically classifies the scanned URL as either *malicious* or *safe* and provides instant feedback.

이 프로젝트는 기존 QR 스캐너에 **딥러닝 기반 악성 URL 탐지 모델**을 추가하여,  
스캔된 URL이 악성인지 정상인지 자동으로 판별한 후 사용자에게 실시간 정보를 제공하는 어플리케이션입니다.

---

## 🎯 Motivation (프로젝트 선정 배경)

Recently, *Qshing*—a phishing method that exploits QR codes—has become increasingly prevalent in Korea.  
To help mitigate this threat, we developed a deep learning-based prevention system that detects and blocks malicious links before users access them.

최근 QR 코드를 이용한 피싱 공격, 일명 **‘큐싱(Qshing)’**이 국내에 확산되고 있습니다.  
이에 따라 인공지능 기술을 활용해 큐싱을 예방할 수 있는 모바일 앱을 개발하고자 본 프로젝트를 진행하였습니다.

---

## 🧰 Tech Stack (기술 스택)

| Category | Tools / Frameworks |
|-----------|--------------------|
| **Languages** | Python, Java |
| **Frameworks / Libraries** | TensorFlow, Scikit-learn, Matplotlib, Whoisdomain |
| **Development Tools** | Visual Studio Code, Android Studio, Jupyter Notebook |

---

## 🔁 System Flow (프로그램 흐름도)

- The app integrates a deep learning model **on-device** for local processing (no server dependency).  
- Optional features allow users to toggle “extra info” display (e.g., domain metadata, registration data).  
- The system ensures both **speed** and **privacy** by performing all inference directly on the device.

앱 내에 **온디바이스(offline)** 환경으로 딥러닝 모델을 내장하여 실시간 URL 분석이 가능합니다.  
또한 부가기능(도메인 정보 제공 등)의 on/off 선택 기능을 제공하며,  
모든 분석 과정을 로컬에서 수행하여 속도와 개인 정보 보호를 강화했습니다.

---

### (1) System Architecture  
![System Flow](https://github.com/KJirung/Smart_QR_Scanner/assets/142071404/7c2a8525-64ce-4045-8d54-e31a6e6b22ce)

### (2) Optional Feature Description  
![Optional Feature](https://github.com/KJirung/Smart_QR_Scanner/assets/142071404/d3c11614-09c9-4b4e-953e-a1e8c84f3dfd)

> **(1)** shows the full system workflow, and **(2)** explains optional user functions.  
> **(1)** 은 시스템의 전체적인 흐름도이며, **(2)** 는 사용자 부가기능을 설명합니다.
