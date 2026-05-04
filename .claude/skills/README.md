# Skills

이 디렉토리는 Claude Code 가 세션 시작 시 로드하는 skill 집합입니다.

**19개 skill** 포함:
- **13 simon-stack** skill — orchestrator · security · method · tools
- **6 일반 개발** skill — commit · review · debug · refactor · test-gen · explain

## 전체 카테고리 맵

→ **[INDEX.md](./INDEX.md)** 참조 (51 skill 풀맵, Gstack 36 포함)

## Skill 구조

각 skill 은 디렉토리 + `SKILL.md` 파일 한 개:

```yaml
---
name: skill-name
description: 트리거 키워드 및 발동 조건
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
version: 1.0.0
author: simon
---

# Skill Title

## When to use
## Workflow
## Checklist
## Anti-patterns
## Related skills
```

Claude Code 는 `description` 의 키워드로 발동 여부를 결정합니다.

## 새 skill 추가하기

1. `<skill-name>/SKILL.md` 생성 + YAML frontmatter 작성
2. `description` 에 한국어·영어 트리거 키워드 구체 명시
3. 본문 5 섹션 구조 유지 (`When to use` → `Workflow` → `Checklist` → `Anti-patterns` → `Related skills`)
4. [INDEX.md](./INDEX.md) 업데이트

## 관련 문서

- [../../README.md](../../README.md) — 레포 개요
- [../../docs/INSTALL.md](../../docs/INSTALL.md) — 로컬 설치
- [../../docs/USING-IN-OTHER-REPOS.md](../../docs/USING-IN-OTHER-REPOS.md) — 다른 레포에서 사용
- [../../docs/MORNING-START.md](../../docs/MORNING-START.md) — 빠른 시작
- [../instincts/](../instincts/) — 누적 학습 시스템
