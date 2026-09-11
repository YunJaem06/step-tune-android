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

현재는 하나의 `:app` Gradle 모듈 안에서 패키지를 책임별로 분리한 구조입니다.

```text
app/src/main/java/hs/project/steptune/
├── api/                 # Retrofit API와 인증 클라이언트
├── core/
│   ├── auth/            # 인증 세션 이벤트
│   ├── di/              # Hilt 모듈
│   ├── navigation/      # 앱 전체 Navigation
│   └── util/            # 날짜, 권한 등 공통 기능
├── data/
│   ├── auth/            # 인증 DTO
│   ├── local/           # Room과 DataStore
│   ├── recommendation/  # 추천 요청·응답 DTO
│   ├── step/            # 걸음 API 요청·응답 DTO
│   └── repository/      # Repository 구현체
├── domain/
│   ├── model/           # 앱에서 사용하는 도메인 모델
│   ├── repository/      # Repository 인터페이스
│   └── usecase/         # 비즈니스 유스케이스
├── feature/
│   ├── home/
│   ├── login/
│   ├── musiclibrary/
│   ├── musicpreference/
│   ├── onboarding/
│   ├── recommendation/
│   ├── settings/
│   ├── splash/
│   └── stats/
├── service/             # 걸음 측정 서비스와 부팅 Receiver
└── ui/theme/            # Compose 테마
```

현재 Presentation 구조는 MVVM을 사용하며, `UiState`를 관찰하고 UI 이벤트를 ViewModel로 전달하는 단방향 데이터 흐름을 함께 적용하고 있습니다.

```text
Compose UI
    ↓ 사용자 이벤트
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
| Core | 내비게이션, DI, 인증 이벤트와 공통 기능 |

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

다음 단계에서는 현재 동작을 유지하면서 **멀티 모듈 + Clean Architecture + Feature별 MVI** 구조로 점진적으로 전환합니다.

```text
step-tune-android/
├── app/                  # Application, MainActivity, 전체 Navigation
├── core/
│   ├── common/           # Android 비의존 공통 기능
│   └── designsystem/     # Compose Theme과 공통 UI
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
:data ──────────────→ :domain
:data ──────────────→ :core:common
```

의존성 규칙은 다음과 같습니다.

- Feature 모듈은 Data 모듈을 직접 참조하지 않습니다.
- Domain 모듈은 Android, Compose, Retrofit, Room을 참조하지 않습니다.
- Data 모듈은 Domain의 Repository 인터페이스를 구현합니다.
- App 모듈은 각 모듈을 조립하고 앱 전체 Navigation을 관리합니다.
- 모듈 간 이동은 공개된 Route와 Navigation 콜백을 통해 처리합니다.

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
        val isLoading: Boolean = false,
        val recommendations: List<MusicRecommendationUiModel> = emptyList()
    )

    sealed interface Action {
        data object Load : Action
        data object LoadMore : Action
        data class ToggleFavorite(val recommendationId: String) : Action
        data class OpenYouTube(val searchQuery: String) : Action
    }

    sealed interface Effect {
        data class OpenYouTube(val searchQuery: String) : Effect
    }
}
```

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

1. 현재 기능을 기준 상태로 확정
2. `:core:common`, `:core:designsystem` 모듈 생성
3. `:domain` 모듈 분리 및 Android·Compose 의존성 제거
4. `:data` 모듈 분리 및 App BuildConfig 값을 Hilt로 주입
5. 음악 기능을 `:feature:music`으로 이동
6. 음악 추천과 보관함 화면을 MVI Contract로 전환
7. Auth, Home, Stats, Settings 순서로 Feature 모듈 분리
8. 로그인·걸음 측정·추천 전체 흐름 회귀 테스트
9. 최종 모듈 구조와 선택 이유 문서화

## 테스트 원칙

- 걸음 수 계산, 목표 달성률과 날짜 처리 등 핵심 비즈니스 로직 테스트
- 인증 토큰 갱신과 API 요청 형식 테스트
- 서버 DTO를 Domain 모델로 변환하는 파싱 테스트
- 추천 기록 페이지, 보관함 변경과 삭제 상태 처리 테스트
- 대규모 TDD보다 오류 가능성이 높은 로직 중심으로 테스트
