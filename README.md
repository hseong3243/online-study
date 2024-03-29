# 나의 공부방

## 프로젝트 내용
동일한 학습 목표를 가진 학습자들과 그룹을 형성하고, 학습을 기록하여 도표로 보여줄 수 있는 웹 어플리케이션을 목표로 개발하였습니다.<br/>
본 프로젝트는 개인 프로젝트로 수행하였습니다.

## API 문서
[API 문서](https://studybread.shop/docs/index.html)

## 서버 구성
![온라인스터디 서버 구조](https://user-images.githubusercontent.com/48748265/233829738-18b01b21-d509-4a42-896d-6057de9d72e5.png)

## 사용 기술
- Java, Spring
- Spring Data JPA
- QueryDSL
- Spring Rest Docs
    - 프로덕션 코드와 문서화 코드가 섞이는 것을 피하기 위해 사용하였습니다.
- MySQL
- redis
    - 다중 서버 환경의 세션 동기화를 위하여 사용하였습니다.
- Jenkins
    - develop 브랜치 push 시에 Build를 진행합니다.
    - Build 후 docker image build → docker hub를 통해 원격 서버에 배포하는 스크립트를 실행합니다.
- NGINX
    - 2대의 WAS에 대하여 로드밸런싱을 수행합니다.
    - SSL 인증서를 발급받아 https 통신을 이용하도록 구성하였습니다.
    - http 접속시 https로 리다이렉트 되도록 설정했습니다.

## DB
![온라인스터디DB(수정)](https://user-images.githubusercontent.com/48748265/233852756-9034111f-4f91-4a0e-8fb3-7c38c18e73ff.png)

- ticket, study_ticket, rest_ticket 3개의 테이블이 있었으나, ticket 테이블 1개로 반정규화 하였습니다. 조인횟수를 줄이기 위해 JPA의 단일 테이블 전략을 사용하며 ticket 테이블의 ticket_status 컬럼에 의해 테이블이 구분됩니다.

## 화면 구성
![프로토타입](https://user-images.githubusercontent.com/48748265/233851677-ae6a8334-8f2e-4af6-bc4e-e2d17dfd4619.jpg)
아래 링크를 통해 화면구성을 위한 프론트엔드 작업본을 확인하실 수 있습니다. 화면 구성과 프론트, 백엔드 간의 통신을 확인하기 위한 프로토타입으로써 Vue.js를 이용하여 작업하였습니다.<br/>
https://github.com/hseong3243/online-study-front/tree/develop

## 문제 및 해결방법
- 그룹원들의 지속적인 상태 갱신
    - 같은 그룹 내에서 현재 공부하고 있는 인원이 얼마나 되는지 시각적인 정보를 사용자에게 전달해주고 싶었습니다. 이를 위해 그룹원들의 학습 시작, 휴식, 종료 상태의 변화에 따라 지속적으로 클라이언트의 화면을 갱신하는 실시간성을 확보할 필요가 있었습니다.
    - 하나의 그룹은 최대 30명의 인원으로 구성됩니다. 처음에는 클라이언트가 주기적으로 요청을 보내는 폴링 방식을 고려하였으나, 개별 클라이언트마다 요청과 응답이 발생한다는 점, 요청 주기에 따라 의미 없는 요청이 다수 발생하거나, 실시간성을 저하시켜 의도한 목적을 이루지 못하는 점으로 인해 적절하지 않다 판단하였습니다. 따라서 실시간성을 보장할 수 있는 다른 방법인 WebSocket 통신을 이용하였습니다.
    - 사용자의 상태가 변화했을 때만 클라이언트와 서버 사이에 통신이 이루어질 수 있다는 점, pub-sub 구조를 통해 서버는 하나의 메시지만 발행해도 해당 토픽을 구독중인 다수의 클라이언트가 메시지를 받아볼 수 있다는 점을 들어 폴링 방식보다 적합하다 판단했습니다. WebScoket STOMP 프로토콜을 이용하여 스터디 화면에서 그룹원들의 상태가 지속적으로 갱신되는 실시간성을 제공할 수 있었습니다.
