# Tokyo Last Train Finder

도쿄 수도권 전철망(지하철, JR, 사철)을 대상으로 출발역에서 도착역까지 **가장 늦게 출발할 수 있는 막차 경로**를 탐색하는 풀스택 웹 애플리케이션입니다.

## Features

- **막차 경로 탐색** — 직통부터 최대 3회 환승까지, 가장 늦은 출발 시각의 최적 경로 도출
- **역 자동완성** — 일본어(히라가나/한자) 및 영어 입력 지원, 300ms 디바운스 적용
- **상세 경로 정보** — 출발/도착 시각, 운행 노선, 열차 종별(각정/쾌속 등), 환승역, 도보 이동 시간
- **요금 계산** — IC카드 기준 구간별 요금 자동 합산
- **캘린더 자동 판별** — 평일/토요일/공휴일 시간표를 날짜 기반으로 자동 선택, Fallback 로직 포함

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Frontend                             │
│              React 19 + TypeScript + Vite                   │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │ StationInput │  │  SearchForm  │  │    RouteCard     │  │
│  │ (autocomplete│  │              │  │  (route detail)  │  │
│  │  + debounce) │  │              │  │                  │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────────────┘  │
│         │                 │                                  │
│  ┌──────┴─────────────────┴──────────────────────────────┐  │
│  │  useStationSearch / useLastTrain (Custom Hooks)       │  │
│  └──────────────────────┬────────────────────────────────┘  │
│                         │ fetch(/api/v1/...)                 │
└─────────────────────────┼───────────────────────────────────┘
                          │ Vite proxy
┌─────────────────────────┼───────────────────────────────────┐
│                    Backend (port 8080)                       │
│              Java 21 + Spring Boot 3.4.4                    │
│                         │                                    │
│  ┌──────────────────────┴────────────────────────────────┐  │
│  │              REST Controllers                         │  │
│  │  GET /api/v1/stations/search?query=...                │  │
│  │  GET /api/v1/last-train?from=...&to=...               │  │
│  └──────────────────────┬────────────────────────────────┘  │
│                         │                                    │
│  ┌──────────────────────┴────────────────────────────────┐  │
│  │              Service Layer                            │  │
│  │  ┌─────────────────────┐  ┌────────────────────────┐  │  │
│  │  │ StationSearchService│  │   LastTrainService     │  │  │
│  │  │ (name index lookup) │  │ (calendar resolution,  │  │  │
│  │  │                     │  │  fare calculation)     │  │  │
│  │  └─────────────────────┘  └───────────┬────────────┘  │  │
│  │                                       │               │  │
│  │  ┌────────────────────────────────────┴────────────┐  │  │
│  │  │         Reverse RAPTOR Engine                   │  │  │
│  │  │  - Round-based backward traversal               │  │  │
│  │  │  - Transfer graph expansion per round           │  │  │
│  │  │  - Midnight/early-morning time handling         │  │  │
│  │  └────────────────────────────────────┬────────────┘  │  │
│  └───────────────────────────────────────┼───────────────┘  │
│                                          │                   │
│  ┌───────────────────────────────────────┴───────────────┐  │
│  │            TransitDataCache (In-Memory)               │  │
│  │                                                       │  │
│  │  ConcurrentHashMap-based indices:                     │  │
│  │  - stationsById        (역 데이터)                     │  │
│  │  - trainTimetables     (열차 시간표)                    │  │
│  │  - nameIndex           (다국어 역명 검색)               │  │
│  │  - transferGraph       (환승 그래프)                    │  │
│  │  - stationToTrainTimetables (역별 시간표 역인덱스)      │  │
│  │  - fares               (구간 요금)                     │  │
│  └───────────────────────────────────────┬───────────────┘  │
│                                          │ @PostConstruct    │
└──────────────────────────────────────────┼──────────────────┘
                                           │ WebClient (256MB buffer)
                                           │ 30s connect / 3min read
┌──────────────────────────────────────────┴──────────────────┐
│                    ODPT API (v4)                             │
│         https://api.odpt.org/api/v4                         │
│                                                             │
│  Dump endpoints:                                            │
│  - odpt:Station          (전체 역)                          │
│  - odpt:Railway          (전체 노선)                        │
│  - odpt:TrainTimetable   (전체 열차 시간표)                  │
│  - odpt:StationTimetable (전체 역 시간표)                    │
│  - odpt:RailwayFare      (전체 구간 요금)                    │
│  - odpt:TrainType        (열차 종별)                        │
│  - odpt:Calendar         (운행 캘린더)                       │
└─────────────────────────────────────────────────────────────┘
```

## Reverse RAPTOR Algorithm

일반적인 RAPTOR 알고리즘이 출발역에서 도착역 방향으로 **가장 빠른 도착**을 찾는 반면, 이 프로젝트의 Reverse RAPTOR는 **도착역에서 출발역 방향으로 역탐색**하여 가장 늦은 출발 시각을 찾습니다.

```
Round 0: 도착역을 직통으로 연결하는 열차 역방향 스캔
Round 1: Round 0 결과 + 환승 1회 확장
Round 2: Round 1 결과 + 환승 2회 확장
Round 3: Round 2 결과 + 환승 3회 확장
```

**각 라운드 처리 흐름:**

1. **열차 역방향 스캔** — marked station을 지나는 모든 열차 시간표를 조회하고, 해당 열차의 이전 정차역들에 대해 "가장 늦은 출발 시각"을 갱신
2. **환승 전파** — 갱신된 역에서 환승 그래프를 통해 연결된 역으로 전파 (도보 환승 5분 감산)
3. **출발역 도달 확인** — 출발역이 갱신되었으면 해당 라운드의 최적 경로를 결과에 추가

**심야 시간 처리:**
- 24시, 25시 등 철도 시간 표기를 `LocalTime`으로 변환 (25:00 → 01:00)
- 새벽 1:30 이전 열차는 전일 운행분으로 판별

## Tech Stack

| Layer    | Stack                                                   |
|----------|---------------------------------------------------------|
| Backend  | Java 21, Spring Boot 3.4.4, Spring WebFlux, Jackson     |
| Frontend | React 19, TypeScript 5.9, Vite 8                        |
| HTTP     | Reactor Netty (WebClient, 256MB buffer, redirect follow) |
| Data     | ODPT API v4 (Dump API, 7 endpoints)                     |

## Project Structure

```
tokyo-last-train/
├── src/main/java/tokyo/lasttrain/
│   ├── controller/
│   │   ├── StationController.java      # GET /api/v1/stations/search
│   │   └── LastTrainController.java    # GET /api/v1/last-train
│   ├── service/
│   │   ├── StationSearchService.java   # 역 검색 인터페이스
│   │   ├── LastTrainService.java       # 막차 탐색 인터페이스
│   │   └── impl/
│   │       ├── StationSearchServiceImpl.java  # 역명 인덱스 기반 검색
│   │       ├── LastTrainServiceImpl.java      # 캘린더 해석, 요금 계산, DTO 변환
│   │       └── ReverseRaptorEngine.java       # 핵심 경로 탐색 알고리즘
│   ├── model/                          # ODPT JSON → Java Record 매핑
│   │   ├── OdptStation.java
│   │   ├── OdptRailway.java
│   │   ├── OdptTrainTimetable.java
│   │   ├── OdptStationTimetable.java
│   │   ├── OdptRailwayFare.java
│   │   ├── OdptTrainType.java
│   │   └── OdptCalendar.java
│   ├── dto/
│   │   ├── LastTrainResponse.java      # 경로 응답 (routes, transfers, fare)
│   │   └── StationSearchResponse.java  # 역 검색 응답
│   ├── cache/
│   │   └── TransitDataCache.java       # 인메모리 캐시 + 인덱스 빌더
│   ├── client/
│   │   └── OdptApiClient.java          # ODPT Dump/Filter API 클라이언트
│   └── config/
│       ├── AppConfig.java              # WebClient 빈 (버퍼, 타임아웃)
│       ├── OdptApiProperties.java      # application.yml 프로퍼티 바인딩
│       └── WebConfig.java              # CORS 설정
├── src/main/resources/
│   └── application.yml                 # 서버 포트, ODPT API 설정
├── frontend/
│   ├── src/
│   │   ├── App.tsx                     # 메인 레이아웃
│   │   ├── types.ts                    # TypeScript 인터페이스 정의
│   │   ├── components/
│   │   │   ├── Header.tsx
│   │   │   ├── SearchForm.tsx          # 출발역/도착역 입력 폼
│   │   │   ├── StationInput.tsx        # 자동완성 드롭다운
│   │   │   ├── RouteList.tsx           # 검색 결과 목록
│   │   │   ├── RouteCard.tsx           # 개별 경로 카드
│   │   │   ├── TransferStep.tsx        # 환승 정보 표시
│   │   │   └── LoadingSpinner.tsx
│   │   ├── hooks/
│   │   │   ├── useStationSearch.ts     # 역 자동완성 (디바운스 + AbortController)
│   │   │   └── useLastTrain.ts         # 막차 검색 상태 관리
│   │   ├── api/
│   │   │   └── client.ts              # fetch wrapper
│   │   └── utils/
│   │       ├── debounce.ts
│   │       └── format.ts
│   ├── vite.config.ts                  # 개발 서버 (3000) + API 프록시
│   ├── tsconfig.json
│   └── package.json
└── pom.xml
```

## Getting Started

### Prerequisites

- Java 21
- Node.js 18+
- [ODPT API key](https://developer.odpt.org/) (무료 등록)

### Backend

```bash
export ODPT_API_KEY=your_api_key
mvn spring-boot:run
```

서버 기동 시 ODPT Dump API에서 전체 데이터를 로딩합니다 (초기 기동 약 2~3분 소요).
백엔드: `http://localhost:8080`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

프론트엔드: `http://localhost:3000` (API 요청은 Vite 프록시로 백엔드에 전달)

## API

### Station Search

```
GET /api/v1/stations/search?query=shibuya
```

```json
{
  "stations": [
    {
      "stationId": "odpt.Station:TokyoMetro.Ginza.Shibuya",
      "nameJa": "渋谷",
      "nameEn": "Shibuya",
      "railway": "odpt.Railway:TokyoMetro.Ginza",
      "railwayNameJa": "東京メトロ銀座線",
      "railwayNameEn": "Ginza Line",
      "operator": "odpt.Operator:TokyoMetro",
      "latitude": 35.659,
      "longitude": 139.7016
    }
  ]
}
```

### Last Train Search

```
GET /api/v1/last-train?from=odpt.Station:TokyoMetro.Ginza.Shibuya&to=odpt.Station:JR-East.Yamanote.Tokyo
```

```json
{
  "fromStation": "odpt.Station:TokyoMetro.Ginza.Shibuya",
  "toStation": "odpt.Station:JR-East.Yamanote.Tokyo",
  "calendarType": "Weekday",
  "routes": [
    {
      "departureTime": "23:45",
      "arrivalTime": "00:12",
      "railway": "odpt.Railway:TokyoMetro.Ginza",
      "railwayNameJa": "東京メトロ銀座線",
      "railwayNameEn": "Ginza Line",
      "railDirection": "odpt.RailDirection:TokyoMetro.Asakusa",
      "trainType": "Local",
      "destinationNameJa": "浅草",
      "destinationNameEn": "Asakusa",
      "transfers": [],
      "totalFare": 200
    }
  ]
}
```

## Data Flow

```
[User Input] → StationInput (debounce 300ms)
    → GET /api/v1/stations/search → nameIndex lookup → autocomplete results

[Search Click] → SearchForm
    → GET /api/v1/last-train
    → resolveCalendar (date → Weekday/Saturday/Holiday)
    → ReverseRaptorEngine.findLastTrains()
        → Round 0~3: scanRoutesReverse() + transfer expansion
    → toRoute(): railway name, train type, fare calculation
    → LastTrainResponse (JSON)
    → RouteList → RouteCard + TransferStep (UI rendering)
```

## License

This project uses data from the [ODPT API](https://developer.odpt.org/). Please comply with their terms of use.
