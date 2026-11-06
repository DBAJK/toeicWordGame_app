# 토익 단어 타자 (Android) 🧁

낙하하는 토익 단어를 타이핑해 맞히는 학습 게임. Kotlin과 XML 레이아웃(Activity)으로 만든 Android 앱이다.

## 열기

1. Android Studio에서 **File > Open**으로 이 폴더(`toeicApp`)를 연다.
2. Gradle 동기화가 끝나면 기기나 에뮬레이터를 고르고 **Run**을 누른다.

명령줄에서 빌드할 때는 JDK 21이 필요하다. Android Studio에 들어 있는 JDK를 쓰면 된다.

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew assembleDebug testDebugUnitTest lintDebug
```

APK는 `app/build/outputs/apk/debug/app-debug.apk`에 나온다.

## 단어 데이터

`app/src/main/assets/words.json`은 단어 데이터 파일이다. 외부 사이트에서 수집한 원본이라 git에서 제외했고,
저장소를 새로 받으면 직접 넣어야 한다. 파일이 없으면 앱이 안내 문구를 보여 준다.
정답 후보(`answersEn`, `answersKo`)는 파일을 만들 때 미리 정규화해서 넣어 두므로 앱에는 변환 규칙이 없다.
배포·과제 제출 전에는 직접 작성한 데이터로 바꿔야 한다.

## 제출용 APK 만들기

출시용 서명 키로 서명한 APK를 만든다. 키(`.jks`)와 비밀번호 파일(`keystore.properties`)은 git에서 제외되어 있다.
키를 잃어버리면 같은 앱으로 업데이트할 수 없으니 두 파일을 따로 백업해 둔다.

1. 키를 한 번만 만든다. 비밀번호와 이름 등을 물어보면 직접 입력한다.

   ```bash
   "/c/Program Files/Android/Android Studio/jbr/bin/keytool" -genkeypair -v -keystore toeicword-release.jks -alias toeicword -keyalg RSA -keysize 2048 -validity 10000
   ```

2. 프로젝트 루트에 `keystore.properties`를 만든다.

   ```properties
   storeFile=toeicword-release.jks
   storePassword=키 저장소 비밀번호
   keyAlias=toeicword
   keyPassword=키 비밀번호
   ```

3. 빌드한다. 결과물은 `app/build/outputs/apk/release/app-release.apk`다.

   ```bash
   ./gradlew assembleRelease
   ```

`keystore.properties`가 없으면 서명 없이 `app-release-unsigned.apk`가 나오고, 이 파일은 휴대폰에 설치되지 않는다.
제출 전에 `versionCode`·`versionName`(`app/build.gradle.kts`)을 필요에 맞게 올린다.

## 구성

| 영역 | 내용 |
|---|---|
| UI | Activity + XML 레이아웃 (View Binding) |
| 구조 | Activity → `AppStore` → 저장소 |
| 저장 | Room(게임 기록, 별표·외움), SharedPreferences(메인 화면 선택, 배속) |
| 게임 루프 | `Choreographer` 프레임 콜백 |

화면마다 Activity 하나와 레이아웃 XML이 짝을 이룬다.

| 화면 | Activity | 레이아웃 |
|---|---|---|
| 메인 | `MainActivity` | `activity_main.xml` |
| 게임 | `GameActivity` | `activity_game.xml` |
| 단어 사전 | `DictionaryActivity` | `activity_dictionary.xml` |

화면 크기별 배치는 Android의 리소스 폴더 규칙으로 나뉜다. 같은 이름의 파일이 있으면 기기에 맞는 쪽을 읽는다.

| 폴더 | 쓰이는 때 |
|---|---|
| `res/layout/` | 기본(휴대폰 세로) |
| `res/layout-w600dp/` | 폭 600dp 이상(휴대폰 가로) |
| `res/layout-w700dp/` | 폭 700dp 이상(단어 목록에 분류·메모 칸 추가) |
| `res/layout-w600dp-h480dp/` | 넓고 높은 화면의 사전 |
| `res/layout-w860dp-h640dp/` | 넓은 화면의 메인 |

`include_*.xml`은 여러 레이아웃이 함께 쓰는 조각이고, 색·글자 크기는 `res/values/`, 버튼·패널 모양은 `res/drawable/`에 있다.
안드로이드 스튜디오의 레이아웃 편집기로 열어 고칠 수 있다.

게임 보드만은 XML이 아니라 코드로 그린다. 단어가 매 프레임 움직이기 때문이다. `activity_game.xml`의 `GameBoardView`가 그 자리이고, 머리글과 하단 바, 일시정지·결과 패널은 XML이다.

```
app/src/main/java/com/toeicword/app/
├─ MainActivity, GameActivity, DictionaryActivity
├─ domain/        Types, Normalize, Judge, Selection
├─ game/          Config, Stages, Themes, Engine
├─ data/          AppDatabase(Room), Repositories
└─ ui/
   ├─ AppStore.kt       앱 전체의 상태. Activity들이 함께 본다
   ├─ BaseActivity.kt   세 Activity의 공통 설정
   ├─ widget/           커스텀 뷰: 무대 카드, 드롭다운, 윤곽 글자, 배경 그림
   ├─ game/             GameBoardView, 하트, 연출 키프레임
   └─ dictionary/       단어 목록 어댑터, 카드 학습
```

`domain/`과 `game/`은 Android에 의존하지 않는 순수 Kotlin이라 기기 없이 단위 테스트를 돌릴 수 있다.
