# 코드 스타일과 리팩터링 규칙

백엔드(`ballpark-ticketing-be`)와 프론트(`ballpark-ticketing-fe`)가 같이 따르는 규칙이다.
CLAUDE.md의 "코드 규칙"과 충돌하면 CLAUDE.md가 우선이다.
저장소 사본: `docs/conventions/code-style.md`. 옵시디언 사본: `00. Design/3. Convention/`.

## 1. 원칙
- **책임 하나**: 클래스나 컴포넌트가 바뀌는 이유는 하나여야 한다. 이유가 둘 이상이면 나눈다.
- **기술은 필요해질 때 넣는다**: 리팩터링이 새 라이브러리나 추상화를 끌고 오지 않는다.
- **동작은 그대로**: 리팩터링 PR은 동작을 바꾸지 않는다. 바꿔야 하면 기능 PR로 따로 한다.
- **엔티티와 도메인 규칙은 엔티티 안에**: 상태 전이 검증은 `Game.start()`처럼 엔티티 메서드가 한다.

## 2. 백엔드 (Java 25, Spring Boot 4)

### 2.1 Lombok
- 생성자 주입은 `@RequiredArgsConstructor`로 쓴다. 본문이 `this.x = x;`만 있는 생성자를 손으로 쓰지 않는다.
- 로그는 `@Slf4j`. 손으로 `LoggerFactory`를 만들지 않는다.
- 엔티티: `@Getter`는 쓴다. `@Setter`, `@Data`는 쓰지 않는다. 기본 생성자는 `@NoArgsConstructor(access = PROTECTED)`.
- 생성자에 `@Value("${...}")`를 두지 않는다. 설정은 `@ConfigurationProperties` 레코드로 묶는다 (예: `LiveSimulatorProperties`).
- 값 객체와 DTO는 `record`. 필드가 많아서 빌더가 필요할 때만 `@Builder`.
- 예외: 엔티티 생성자는 도메인 검증이 들어가므로 손으로 쓴다.

### 2.2 import 순서
`com.ballpark` → `jakarta` → `java` → `lombok` → `org`. 정적 import는 맨 위.
빈 줄로 그룹을 나누지 않는다(현재 파일들과 같다).

### 2.3 계층
- Controller: 요청 변환과 응답 DTO 조립만. 도메인 판단을 하지 않는다.
- Service: 유스케이스 순서와 트랜잭션 경계. 상태 판단은 엔티티에 맡긴다.
- 순수 계산은 `static` 메서드나 `final` 클래스로 빼고 단위 테스트한다.
  예: `StandingCalculator`, `InningRules`.
- 조회 전용 값은 JPQL DTO 프로젝션으로 읽는다(엔티티를 올리지 않는다).

### 2.4 이름
- `XxxService`: 유스케이스. `XxxCalculator`, `XxxRules`: 순수 규칙. `XxxPicker`, `XxxRoll`: 무작위 선택.
- `XxxHub`: 커밋 뒤에 구독자에게 보내는 역할.
- 상수는 `SCREAMING_SNAKE_CASE`, 시간 상수는 단위를 이름에 쓴다 (`EXPIRY_MINUTES`).

### 2.5 주석
- "무엇"이 아니라 "왜"가 필요한 곳에만 한국어로 쓴다.
- 클래스 주석은 역할과 경계(무엇을 하고 무엇을 하지 않는지)를 한두 줄로.

## 3. 프론트 (React 19, TypeScript, Vite)

### 3.1 나누는 기준
- **페이지**: 데이터를 모으고 하위 컴포넌트를 조립한다. 계산이나 그리기는 하지 않는다.
- **`components/<기능>/`**: 페이지의 한 부분. 위젯 하나, 카드 하나가 한 파일이다.
- **`lib/`**: 화면과 무관한 순수 함수(날짜, 포맷, 기하). 테스트하기 쉬워야 한다.
- **`hooks/`**: 서버 상태(react-query)와 브라우저 상태(localStorage, SSE)를 감싼다.
- **`api/`**: 엔드포인트 호출과 타입. 화면 코드가 `fetch`를 직접 쓰지 않는다.

### 3.2 타입
- 타입만 가져올 때는 `import type`. `verbatimModuleSyntax`가 켜져 있다.
- API 응답 타입은 `api/types.ts`에 둔다. 백엔드 DTO와 이름을 맞춘다.

### 3.3 스타일
- 들여쓰기 2칸, 세미콜론 없음, 작은따옴표 (기존 파일과 같다).
- Tailwind 클래스는 JSX에 직접 쓴다. 반복되는 묶음만 `lib/`의 상수로 뺀다.
- 색은 `teamColor`, `stadiumPalette`처럼 한 곳에서 정한다. 컴포넌트 안에 색 상수를 흩뿌리지 않는다.

## 4. 리팩터링 절차
1. **테스트부터**: 바꾸기 전에 `./gradlew build` 또는 `tsc` + `vite build`가 통과하는지 본다.
2. **옮기기 먼저, 고치기 나중**: 함수 본문을 그대로 옮기고 `export`와 import 경로만 바꾼 커밋을 먼저 낸다.
3. **한 번에 한 가지**: 순서 정리, Lombok 전환, 책임 분리를 각각 다른 커밋으로 나눈다.
4. **확인**: 빌드, 테스트, 가능하면 화면에서 직접 확인한다. 확인하지 못한 항목은 PR 본문에 적는다.
5. **PR 본문**: Before(무엇이 섞여 있었나) / After(어디로 나뉘었나) / How(동작 변경 없음을 어떻게 확인했나).

## 5. 하지 않는 것
- 동작을 바꾸면서 리팩터링이라고 부르지 않는다.
- 필요해지기 전에 추상 계층(인터페이스 하나짜리 구현 등)을 만들지 않는다.
- 리팩터링 PR에 기능 추가를 섞지 않는다.
- 커밋 작성자를 직접 지정하지 않는다 (git 설정 그대로 쓴다).
