---
name: "source-command-commit"
description: "변경사항을 검토하고 프로젝트 커밋 컨벤션에 맞춰 커밋 생성"
---

# source-command-commit

Use this skill when the user asks to run the migrated source command `commit`.

## Command Template

다음 절차로 커밋을 만들어줘:

1. `.agents/rules/commit.md`를 먼저 읽어 컨벤션 확인
2. `git status`, `git diff --staged`, `git diff`, `git log -5`를 병렬 실행해 변경 상황 파악
3. 변경된 파일들의 **공통 의도(why)** 를 한 줄로 요약
4. 컨벤션에 따라 메시지 작성:
   - type: feat/fix/refactor/perf/wiki/test/build/chore/style/init 중 하나
   - scope: 변경 영향이 가장 큰 도메인명 또는 모듈명 (괄호 안)
   - subject: 한국어, 50자 이내, 명령형, 마침표 없음
   - 복잡한 변경이면 body로 why 부연
5. 작성한 메시지를 채팅에 먼저 보여주고 사용자 확인 대기
6. 확인 후:
   - 변경 파일을 명시적으로 `git add <파일들>` (절대 `git add -A` / `git add .` 금지)
   - `git commit -m "$(cat <<'EOF' ... EOF)"` HEREDOC 형식으로 실행
7. `Co-Authored-By` 트레일러 절대 추가하지 않음
8. 커밋 후 `git status`로 결과 확인

주의:
- 한 커밋에 무관한 변경이 섞여 있으면 분리 권유 후 어느 것부터 커밋할지 묻기
- 비밀 정보(.env, credentials.json 등)가 staged면 경고하고 중단
- pre-commit 훅 실패 시 원인 수정 후 **새 커밋** (--amend 금지)
