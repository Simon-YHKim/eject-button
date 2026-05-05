// Build drag_confirm.json — progress-driven concentric ripple overlay (v3).
//
// REVISED per real One UI 8.5 reference frames 3–9 (1.6s / 48-frame GIF):
//   - Outer ring CENTERED on the pressed button, GROWS from ~100→180dp.
//     (Previously held at 150 — wrong. It expands as drag progresses.)
//   - Inner ring (touch tracker) grows 30→160dp.
//   - Both fade in over p=0..0.05 and fade out over p=0.95..1.0
//     (the trigger moment is a soft dissolve, not a hard cut).
//   - The Compose-side layer fades the PRESSED button as p→1, and the
//     OPPOSITE button only at p>0.85. Lottie does not own that.
//
// Canvas: 400×400dp transparent, center (200,200), 60fps, op=100 frames.
// (Outer max diameter 360 fits with 20dp safety margin.)
// Compose host: Modifier.size(400.dp), centered on the pressed button.
// Same JSON used for accept and decline — Compose anchors to whichever side.

(function (global) {
  'use strict';

  const W = 400;
  const H = 400;
  const FPS = 60;
  const OP = 100;

  // Radii (dp) — calibrated to reference frames 3–9
  const INNER_R_START = 30;
  const INNER_R_END   = 160;
  const OUTER_R_START = 100;
  const OUTER_R_END   = 180;

  const INNER_RGB = [150 / 255, 150 / 255, 150 / 255, 1];
  const INNER_ALPHA_PCT = 45;
  const OUTER_RGB = [200 / 255, 200 / 255, 200 / 255, 1];
  const OUTER_ALPHA_PCT = 30;

  // Fade in / out frame markers
  const FADE_IN_END  = 5;    // p ≈ 0.05
  const FADE_OUT_START = 95; // p ≈ 0.95

  // Linear easing — Compose drives progress directly from drag distance.
  function linKf(t, s, isLast) {
    const kf = { t, s: Array.isArray(s) ? s : [s] };
    if (!isLast) {
      kf.o = { x: [0], y: [0] };
      kf.i = { x: [1], y: [1] };
    }
    return kf;
  }

  function ellipse(sizeAnim) {
    return { ty: 'el', d: 1, s: sizeAnim, p: { a: 0, k: [0, 0] }, nm: 'Ellipse' };
  }
  function fill(rgba, opacityAnim) {
    return { ty: 'fl', c: { a: 0, k: rgba }, o: opacityAnim, r: 1, nm: 'Fill' };
  }
  function transform() {
    return {
      ty: 'tr',
      p: { a: 0, k: [0, 0] },
      a: { a: 0, k: [0, 0] },
      s: { a: 0, k: [100, 100] },
      r: { a: 0, k: 0 },
      o: { a: 0, k: 100 },
      sk: { a: 0, k: 0 },
      sa: { a: 0, k: 0 },
      nm: 'Transform',
    };
  }
  function group(items, name) {
    return { ty: 'gr', it: items, nm: name, np: items.length };
  }
  function shapeLayer(name, ind, shapes) {
    return {
      ddd: 0, ind, ty: 4, nm: name, sr: 1,
      ks: {
        o: { a: 0, k: 100 },
        r: { a: 0, k: 0 },
        p: { a: 0, k: [W / 2, H / 2, 0] },
        a: { a: 0, k: [0, 0, 0] },
        s: { a: 0, k: [100, 100, 100] },
      },
      ao: 0, shapes, ip: 0, op: OP, st: 0, bm: 0,
    };
  }

  // Helper — build a size-animation that grows linearly from rStart to rEnd
  function sizeAnim(rStart, rEnd) {
    return {
      a: 1,
      k: [
        linKf(0,  [rStart * 2, rStart * 2]),
        linKf(OP, [rEnd   * 2, rEnd   * 2], true),
      ],
    };
  }
  // Helper — build an opacity animation that fades in / holds / fades out
  function fadeAnim(peakPct) {
    return {
      a: 1,
      k: [
        linKf(0,                [0]),
        linKf(FADE_IN_END,      [peakPct]),
        linKf(FADE_OUT_START,   [peakPct]),
        linKf(OP,               [0], true),
      ],
    };
  }

  // ── INNER (drag-tracking, smaller, darker gray) ─────────────────────────
  const innerShape = group(
    [
      ellipse(sizeAnim(INNER_R_START, INNER_R_END)),
      fill(INNER_RGB, fadeAnim(INNER_ALPHA_PCT)),
      transform(),
    ],
    'Inner',
  );

  // ── OUTER (boundary halo, larger, lighter gray, also grows) ─────────────
  const outerShape = group(
    [
      ellipse(sizeAnim(OUTER_R_START, OUTER_R_END)),
      fill(OUTER_RGB, fadeAnim(OUTER_ALPHA_PCT)),
      transform(),
    ],
    'Outer',
  );

  // Back→front: outer first, inner on top
  const layers = [
    shapeLayer('Outer', 2, [outerShape]),
    shapeLayer('Inner', 1, [innerShape]),
  ];

  function build() {
    return {
      v: '5.7.0',
      meta: { g: 'eject-button drag_confirm v3', a: '', k: '', d: '', tc: '' },
      fr: FPS,
      ip: 0, op: OP, w: W, h: H,
      nm: 'drag_confirm',
      ddd: 0, assets: [], layers, markers: [],
    };
  }

  global.buildDragConfirmLottie = build;
})(typeof window !== 'undefined' ? window : globalThis);
