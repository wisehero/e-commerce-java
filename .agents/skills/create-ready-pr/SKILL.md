---
name: create-ready-pr
description: 이 프로젝트에서 GitHub Pull Request를 생성하거나 게시하고 Ready for review 상태를 검증한다. 사용자가 "PR 올려", "PR 생성", "커밋하고 PR", "푸시하고 PR"처럼 PR 생성을 요청할 때 사용한다.
---

# Ready PR 생성

기본 성공 조건을 **열린 Ready for review PR**로 둔다. PR URL 생성만으로 완료 처리하지 않는다.

## 실행 순서

1. 현재 브랜치, 변경 범위, 커밋, push 상태를 확인한다.
2. Draft 생성을 전제로 하는 도구나 스킬(현재 `github:yeet`)을 사용하지 않는다.
3. Ready PR을 지원하는 GitHub 도구에서 `draft=false`를 명시하거나 `gh pr create`를 `--draft` 없이 실행한다.
4. 사용자가 `draft` 또는 `초안`을 명시했더라도 먼저 Ready PR로 생성한다.
5. PR 생성 직후 다음 명령을 실행한다.

```bash
.agents/skills/create-ready-pr/scripts/ensure-ready.sh <PR URL 또는 번호>
```

6. 명령이 출력한 URL과 Ready 상태를 최종 응답에 포함한다. 명령이 실패하면 PR 생성 작업도 미완료로 취급한다.

## 명시적 Draft 요청

사용자가 `draft` 또는 `초안`을 직접 요청한 경우에만 Ready 검증 후 다음 명령으로 전환한다.

```bash
gh pr ready --undo <PR URL 또는 번호>
gh pr view <PR URL 또는 번호> --json isDraft --jq .isDraft
```

두 번째 명령이 `true`를 출력해야 완료한다. 처음부터 Draft로 생성하지 않는다.

## 도구 실패

GitHub 앱에 생성 또는 상태 변경 권한이 없으면 인증된 `gh` CLI로 전환한다. 같은 권한 오류를 반복하지 않는다.

`.github/workflows/enforce-ready-pr.yml`이 실수로 Draft 상태로 열린 새 PR을 Ready로 복구하지만, 로컬 검증을 생략하는 근거로 삼지 않는다.
