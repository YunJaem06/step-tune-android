# StepTune

걸음 기록과 사용자의 음악 취향을 바탕으로 산책에 어울리는 음악 한 곡을 추천하는 개인 학습용 Android 프로젝트입니다.

Play Store 출시보다는 Android 앱 아키텍처, 로컬 데이터 관리, 인증, 서버 통신과 AI 추천 흐름을 직접 구현하고 설명할 수 있는 포트폴리오 완성을 목표로 합니다.

## 기술 스택

- Kotlin
- Jetpack Compose
- Hilt
- Room
- DataStore
- Navigation Compose
- Retrofit, OkHttp
- Credential Manager 기반 Google 로그인
- Foreground Service와 걸음 수 센서
- Spring Boot 서버 및 Gemini 추천 API 연동

## 주요 기능

- 오늘 걸음 수, 목표 달성률, 거리와 칼로리 표시
- 일간·주간·월간 걸음 통계
- 포그라운드 걸음 측정과 부팅 후 서비스 재시작
- Google 로그인과 Refresh Token 기반 자동 로그인
- 닉네임 중복 검사, 변경, 로그아웃과 회원 탈퇴
- 음악 장르·분위기 취향 온보딩 및 DataStore 저장
- 걸음 통계와 음악 취향을 반영한 AI 단일 곡 추천
- 추천 기록 조회, 보관함 저장·해제와 삭제
- 추천곡 YouTube 검색 연결

## 현재 아키텍처

멀티 모듈 전환 7단계까지 완료해 Auth, Home, Stats, Music, Settings를 별도 Feature 모듈로 분리했습니다. 8단계의 자동 회귀 테스트와 실기기 화면 테스트도 통과했으며, 실제 Google 계정·센서·Gemini를 사용하는 수동 검증은 별도로 구분합니다. 음악 화면에는 MVI를 적용했으며 나머지 화면은 기존 MVVM 동작을 유지합니다. `:app`은 Application, Navigation, 실행 설정과 걸음 측정 서비스를 조립합니다.

```text
step-tune-android/
├── app/src/main/java/hs/project/steptune/
│   ├── core/                # App 설정 주입, Navigation, 인증 만료 처리
│   └── service/             # 걸음 측정 서비스와 부팅 Receiver
├── core/
│   ├── common/              # 날짜 처리, 인증 이벤트와 공통 테스트
│   ├── designsystem/        # Compose 테마, 공통 아이콘과 문구
│   ├── ui/                  # Domain 모델을 사용하는 공유 UI (음악 취향 선택)
│   └── platform/            # Android 권한 확인 (Android 의존, Compose·Domain 비의존)
├── domain/                  # 모델, 오류, Repository 계약, UseCase와 테스트
├── data/src/main/kotlin/hs/project/steptune/
│   ├── api/                 # Retrofit API, Bearer Interceptor와 Authenticator
│   ├── data/
│   │   ├── config/          # API 경로와 주입받는 NetworkConfig
│   │   ├── di/              # Network, Room, DataStore와 Repository Hilt 설정
│   │   ├── local/           # Room, DataStore
│   │   ├── auth/, step/, ... # 요청·응답 DTO
│   │   └── repository/      # Domain Repository 구현체
│   └── util/                # 네트워크 로그 토큰 마스킹
└── feature/
    ├── auth/                # Splash, Google 로그인, 로그인 후 준비, 온보딩
    ├── home/                # 오늘 걸음과 주간 활동 요약
    ├── stats/               # 일간·주간·월간 통계와 서버 활동 비교
    ├── music/               # 추천 생성, 추천 기록·보관함 (MVI)
    └── settings/            # 계정, 신체 정보, 음악 취향과 앱 설정
```

음악 추천·보관함은 `State / Action / Effect` Contract를 사용하는 MVI입니다. 나머지 화면은 기존 MVVM을 유지합니다. 두 방식 모두 ViewModel과 Domain UseCase를 통해 단방향 데이터 흐름을 구성합니다.

```text
Compose UI (State 렌더링)
    ↓ 사용자 이벤트 (음악 화면: Action)
ViewModel
    ↓
UseCase
    ↓
Repository 인터페이스
    ↓
Repository 구현체
    ↓
Retrofit / Room / DataStore
```

### 계층별 책임

| 계층 | 책임 |
| --- | --- |
| Feature | Compose 화면, UI 상태, 사용자 이벤트 처리 |
| Domain | 모델, Repository 계약, 비즈니스 규칙 |
| Data | 서버·Room·DataStore 접근과 DTO 변환 |
| Core | 인증 이벤트, 날짜 처리, 디자인 시스템, 공유 UI와 Android 권한 확인 |
| App | Application, Navigation, 걸음 측정 서비스와 실행 설정 주입 |

### Data 모듈 설정

`local.properties`에서 읽는 서버 URL과 Google Web Client ID는 App의 `BuildConfig`에 유지합니다. App의 `AppConfigurationModule`이 서버 URL, 디버그 HTTP 로그 활성화 여부와 로그 출력 함수를 `NetworkConfig`로 묶어 Hilt에 제공하며, Data의 `NetworkModule`이 이를 사용합니다. Data 모듈은 App의 `Config`나 `BuildConfig`를 직접 참조하지 않습니다.

API 경로는 Data의 `ApiEndpoints`에 모았습니다. 디버그 BODY 로그의 토큰 마스킹과 릴리스 로그 비활성화 정책을 유지합니다. Room DB 이름·버전·Migration과 DataStore 파일명·키는 기존 값을 그대로 사용하므로, 모듈 이동에 따른 로컬 데이터 초기화는 필요하지 않습니다.

인증 만료 이벤트 버스는 `:core:common`에서 App과 Data가 공유합니다. 화면에서 처리하는 인증·추천 오류 타입은 `:domain`에 위치하며, Feature는 Domain의 모델·UseCase·오류 타입을 사용합니다.

### Auth, Home, Stats, Settings 모듈

각 Feature는 기존 패키지명을 유지한 채 Route, Screen, ViewModel, UiState와 전용 문자열·아이콘을 소유합니다. `:feature:settings`는 기존 닉네임 상태 테스트도 소유합니다. 모듈 이동과 화면별 MVI 전환을 분리해, 이 네 Feature의 MVVM 로직과 UI는 그대로 유지했습니다.

Google Web Client ID는 App의 `Config`에 유지하고 `LoginRoute(googleWebClientId, onLoginSucceeded)`로 전달합니다. Credential Manager와 Google 로그인 의존성은 Auth 모듈이 관리합니다. Feature는 App의 `Config`, `BuildConfig`나 Data 구현체를 직접 참조하지 않습니다.

걸음 측정 Service와 부팅 Receiver는 App에 남습니다. App이 PostLogin·Onboarding·Settings Route에 `onStartTracking`/`onStopTracking` 콜백을 전달해 기존 시작·중지 시점과 권한·자동 시작 조건을 유지합니다. Android 권한 확인 유틸은 순수 Kotlin인 `:core:common` 대신 `:core:platform`에서 공유합니다. Android 패키지명, Manifest의 서비스·Receiver 이름, Room·DataStore 저장 구조는 바꾸지 않았습니다.

앱 표시명은 `:core:designsystem`에서 공유하고, App은 하단 Navigation 문구·아이콘, 시스템 Splash 테마와 걸음 측정 알림 리소스를 관리합니다. 각 Feature의 Compose Preview 의존성도 해당 모듈에 두었습니다.

### 음악 Feature와 공유 UI

`:feature:music`은 추천 생성과 추천 기록·보관함의 Route, Screen, ViewModel, Contract와 전용 문구를 소유합니다. Domain UseCase로 데이터에 접근하며 App이나 Data 모듈을 참조하지 않습니다. App의 Navigation은 기존 `MusicRecommendationRoute(onBack)`와 `MusicLibraryRoute(onRecommendationClick)`을 호출합니다.

음악 취향 선택 컴포넌트와 상태는 온보딩·설정·추천 화면에서 함께 사용하므로 `:core:ui`에 두었습니다. 이 모듈은 Domain 모델을 사용하는 공유 UI를 담당합니다. `:core:designsystem`은 Domain에 의존하지 않으며 테마, 공통 아이콘과 문구를 관리합니다.

`MusicRecommendationContract`와 `MusicLibraryContract`는 각각 화면 상태·입력·일회성 동작을 정의합니다. Screen은 `state`와 `onAction`만 받으며 ViewModel은 `state: StateFlow`, `effects: Flow`, `onAction(Action)`을 공개합니다. Navigation과 YouTube 검색은 Effect를 받은 Route가 실행합니다.

State는 `collectAsStateWithLifecycle`로 구독하고, Effect는 화면 Lifecycle이 `STARTED`일 때만 소비합니다. Effect는 버퍼가 있는 Channel을 사용해 구독이 잠시 중단된 동안 보관하되 소비된 이벤트는 다시 구독해도 재생하지 않습니다. ViewModel이 소멸된 뒤의 전달이나 프로세스 종료 시 복원까지 보장하는 저장소는 아닙니다.

추천 생성·보관함 저장·삭제와 페이지 로딩은 시작 즉시 진행 상태를 갱신해 중복 Action을 차단합니다. 보관함은 필터 변경·새로고침 시 이전 조회를 취소하고 요청 번호도 확인해 늦게 도착한 이전 응답이 최신 상태를 덮어쓰지 않도록 합니다. 취향 로딩 실패 시 추천 생성을 막고 재시도를 제공합니다.

## 데이터 저장 책임

| Android | Spring Boot 서버 |
| --- | --- |
| 걸음 원본과 일일 걸음 기록 | 인증과 사용자 정보 |
| 목표 걸음 수와 신체 정보 | Refresh Token 관리 |
| 음악 장르·분위기 취향 | 걸음 요약 동기화 데이터 |
| 걸음 측정 서비스 상태 | Gemini 추천 생성 |
|  | 사용자별 추천 기록과 보관함 상태 |

추천 기록은 서버를 Source of Truth로 사용합니다. Android Room은 걸음 기록만 관리하며, 추천 생성 성공 후 기록·보관함 화면은 서버 API에서 다시 조회합니다.

## 음악 추천 API

```http
POST   /api/v1/music-recommendations/generate
GET    /api/v1/music-recommendations/history?page=0&size=20&favoriteOnly=false
PATCH  /api/v1/music-recommendations/{recommendationId}/favorite
DELETE /api/v1/music-recommendations/{recommendationId}
```

- 생성 API는 현재 사용자의 걸음 기록과 음악 취향으로 정확히 한 곡을 추천하고 서버에 자동 저장합니다.
- 기록 API는 최신순 페이지를 반환하며 `favoriteOnly=true`이면 보관한 곡만 반환합니다.
- Android는 추천곡의 `searchQuery`를 사용해 YouTube 검색 화면을 엽니다.
- 인증이 필요한 요청에는 OkHttp Interceptor가 Access Token을 자동으로 추가합니다.

## 목표 아키텍처

Feature 모듈 분리는 완료했으며, **멀티 모듈 + Clean Architecture + Feature별 MVI** 중 나머지 화면의 MVI 전환은 후속 작업으로 진행합니다.

```text
step-tune-android/
├── app/                  # Application, MainActivity, 전체 Navigation
├── core/
│   ├── common/           # Android 비의존 공통 기능
│   ├── designsystem/     # Compose Theme, 공통 아이콘과 문구
│   ├── ui/               # Domain 모델을 사용하는 공유 UI
│   └── platform/         # Android 권한 등 플랫폼 공통 기능
├── domain/               # 순수 Kotlin 모델, Repository 계약, UseCase
├── data/                 # Retrofit, Room, DataStore, Repository 구현
└── feature/
    ├── auth/             # Splash, 로그인, 온보딩
    ├── home/             # 오늘 걸음 화면
    ├── stats/            # 걸음 통계
    ├── music/            # 추천 생성, 추천 기록과 보관함
    └── settings/         # 사용자와 앱 설정
```

### 목표 모듈 의존성

```text
:app ───────────────→ :data
  ├─────────────────→ :feature:auth
  ├─────────────────→ :feature:home
  ├─────────────────→ :feature:stats
  ├─────────────────→ :feature:music
  └─────────────────→ :feature:settings

:feature:* ─────────→ :domain
:feature:* ─────────→ :core:designsystem
:feature:auth/settings/music → :core:ui
:core:ui ───────────→ :domain
:core:ui ───────────→ :core:designsystem
:data ──────────────→ :domain
:data ──────────────→ :core:common
:feature:home ──────→ :core:common
:feature:auth ──────→ :core:platform
:feature:settings ──→ :core:platform
:app ───────────────→ :core:platform
```

의존성 규칙은 다음과 같습니다.

- Feature 모듈은 Data 모듈을 직접 참조하지 않습니다.
- Domain 모듈은 Android, Compose, Retrofit, Room을 참조하지 않습니다.
- Data 모듈은 Domain의 Repository 인터페이스를 구현합니다.
- App 모듈은 각 모듈을 조립하고 앱 전체 Navigation을 관리합니다.
- 모듈 간 이동은 공개된 Route와 Navigation 콜백을 통해 처리합니다.
- Android 서비스 실행은 Feature의 콜백을 App이 처리하고, Google 설정도 App이 Auth에 전달합니다.

## Feature MVI 규칙

각 화면은 `State`, `Action`, `Effect`를 하나의 Contract로 관리합니다.

```text
사용자 입력
    ↓
Action
    ↓
ViewModel
    ↓
UseCase / Repository
    ↓
State 갱신
    ↓
Compose UI
```

- `State`: 화면을 그리는 데 필요한 지속 상태
- `Action`: 클릭, 입력, 재시도와 같은 사용자 이벤트
- `Effect`: Navigation, 외부 앱 실행, 일회성 메시지

```kotlin
object MusicLibraryContract {
    data class State(
        val isLoading: Boolean = true,
        val recommendations: List<MusicRecommendation> = emptyList()
    )

    sealed interface Action {
        data object Refresh : Action
        data object LoadMore : Action
        data class ToggleFavorite(val recommendationId: String) : Action
        data class OpenYouTube(val recommendationId: String) : Action
    }

    sealed interface Effect {
        data class OpenYouTube(val searchQuery: String) : Effect
    }
}
```

위 예시는 Contract의 일부입니다. 보관함 Action은 추천 ID를 전달하고 ViewModel이 현재 State에서 해당 곡을 찾습니다. 오래된 화면에서 발생한 입력이나 삭제된 ID는 무시합니다. 추천 결과, 에러, 삭제 확인 창과 로딩 여부는 State에 남고 화면 이동·외부 앱 실행만 Effect로 보냅니다.

Screen은 ViewModel을 직접 참조하지 않고 상태와 단일 이벤트 함수만 받습니다.

```kotlin
@Composable
fun MusicLibraryScreen(
    state: MusicLibraryContract.State,
    onAction: (MusicLibraryContract.Action) -> Unit
)
```

## 멀티 모듈 전환 순서

구조 변경과 기능 변경을 같은 커밋에 섞지 않고 다음 순서로 진행합니다.

현재 1번부터 7번까지 완료했고, 8번의 자동 검증을 마쳤습니다. 실제 로그인·걷기·Gemini 추천의 수동 확인은 아래 검증 현황에 구분합니다. Auth, Home, Stats, Settings의 MVI 전환은 모듈 이동과 별도의 후속 작업으로 진행합니다.

1. 현재 기능을 기준 상태로 확정
2. `:core:common`, `:core:designsystem` 모듈 생성
3. `:domain` 모듈 분리 및 Android·Compose 의존성 제거
4. `:data` 모듈 분리 및 App BuildConfig 값을 Hilt로 주입
5. 음악 기능을 `:feature:music`으로 이동
6. 음악 추천과 보관함 화면을 MVI Contract로 전환
7. Auth, Home, Stats, Settings 순서로 Feature 모듈 분리
8. 로그인·걸음 측정·추천 전체 흐름 회귀 테스트
9. 최종 모듈 구조와 선택 이유 문서화

### 8단계 회귀 검증 현황

2026-10-02 기준입니다. 테스트에서 사용하는 토큰·곡·걸음 수는 고정된 테스트 값이며, MockWebServer의 성공 응답은 실제 Google 인증이나 Gemini 호출 성공을 의미하지 않습니다.

| 검증 | 결과 | 범위 |
| --- | --- | --- |
| JVM 테스트 | 89개 통과 | 공통 2, Domain 18, Data 35, Auth 8, Music 15, Settings 2, App 9 |
| 실기기 계측 테스트 | 6개 통과 | Samsung SM-A165N / Android 16, 로그인 버튼·권한 온보딩·홈·추천·보관함의 화면/클릭과 앱 패키지 확인 |
| Debug APK와 테스트 APK | 빌드 성공 | 모듈 의존성, 리소스, Hilt 구성과 JVM 17 호환 확인 |
| 실제 앱 시작 | 성공 | 연결된 실기기에서 MainActivity cold start 확인 |
| 개발 서버 접근 | 응답 확인 | 인증 없는 내 정보 요청이 401을 반환; 인증된 추천 200 확인과는 별개 |

주요 자동 회귀 시나리오는 다음과 같습니다.

- Splash에서 저장된 Refresh Token으로 자동 로그인, 401 시 로그인 이동, 통신 실패 시 세션 보존·재시도
- Google ID Token의 서버 로그인 성공·실패와 계정 선택 취소 후 로딩 해제
- 실제 Retrofit/OkHttp/DataStore 구성과 MockWebServer를 연결한 로그인 → 자동 로그인 → 닉네임 검사·변경 → 401 토큰 갱신 → 걸음 동기화 → 단일 곡 추천 → 기록 조회·보관·삭제 → 로그아웃
- 갱신 거절 시 인증 만료 이벤트 전달·세션 삭제, 걸음 동기화 실패 시 추천 요청 차단
- 회원 탈퇴 성공 시 로컬 데이터 삭제, 실패 시 인증·로컬 기록 보존; 로그아웃에서는 로컬 기록 삭제하지 않음
- 센서 기준값 초기화, 날짜 전환, 재부팅·카운터 감소, 반복 측정과 정수 오버플로 방어

회귀 검증 중 센서 카운터가 기존 기준값보다 높지만 이전 측정보다 낮아진 경우 저장된 걸음 수를 감소시키는 문제를 수정했습니다. 저장된 오늘 기록을 서비스 시작 시 읽고, 계산값이 저장값보다 작으면 기록을 보존하면서 센서 기준값을 다시 설정합니다.

모듈 이동 후 남은 App 소스의 빈 패키지 디렉터리 53개를 정리했습니다. 폴더 정리를 위해 소스 파일을 삭제하거나 Room/DataStore 저장 구조를 변경하지는 않았습니다.

실제 환경의 수동 확인 체크리스트:

2026-10-02 사용자로부터 실사용 확인 완료를 전달받았습니다. 이는 자동 테스트와 별도의 사용자 확인 보고입니다. 재부팅·테스트 계정 회원 탈퇴 등 개별 시나리오의 확인 여부는 추가 점검 시 구분합니다.

- Google 계정 선택 → 로그인 → 온보딩 → 홈 진입
- 앱 재실행 후 실제 Refresh Token 자동 로그인
- 걸으면서 오늘 걸음 수 증가, 앱 재실행 후 기록 유지, 재부팅 후 측정 재개
- 실제 서버의 추천 200 응답, Gemini 단일 곡 결과와 YouTube 검색 연결
- 실제 추천 기록·보관함 반영과 닉네임 변경·로그아웃; 회원 탈퇴는 별도 테스트 계정에서만 확인

### 테스트 실행

Android Studio의 Gradle JDK를 17로 설정한 뒤 프로젝트 루트에서 실행합니다.

```powershell
.\gradlew.bat :core:common:test :domain:test :data:testDebugUnitTest :feature:auth:testDebugUnitTest :feature:music:testDebugUnitTest :feature:settings:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
```

전용 테스트 기기 또는 에뮬레이터를 연결한 뒤 화면 테스트를 실행합니다. 이 테스트는 서버 없이 고정 State와 콜백으로 Screen을 확인하며 실제 Google 계정 선택·추천 생성·회원 탈퇴를 실행하지 않습니다. 계측 테스트의 APK 설치·제거 과정을 고려해 개인 기록이 있는 주 사용 기기보다 전용 테스트 환경을 권장합니다.

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

## 테스트 원칙

- 걸음 수 계산, 목표 달성률과 날짜 처리 등 핵심 비즈니스 로직 테스트
- 인증 토큰 갱신과 API 요청 형식 테스트
- 서버 DTO를 Domain 모델로 변환하는 파싱 테스트
- 추천 기록 페이지, 보관함 변경과 삭제 상태 처리 테스트
- 음악 ViewModel의 Action→State 전환, 중복 요청 차단, 오래된 조회 응답 무시와 Effect 소비 테스트
- 대규모 TDD보다 오류 가능성이 높은 로직 중심으로 테스트
