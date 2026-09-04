import { Component, computed, input } from '@angular/core';

type Tone = 'ok' | 'warn' | 'bad' | 'muted' | 'neutral';

const TONE_BY_CODE: Record<string, Tone> = {
  ACTIVE: 'ok',
  APPROVED: 'ok',
  DRAFT: 'warn',
  SUBMITTED: 'warn',
  UNDER_REVIEW: 'warn',
  REJECTED: 'bad',
  CANCELLED: 'bad',
  EXPIRED: 'bad',
  ADOPTED: 'muted',
  CLOSED: 'muted',
  WITHDRAWN: 'muted',
};

@Component({
  selector: 'app-status-chip',
  template: `<span class="chip" [class]="'chip--' + tone()">{{ label() }}</span>`,
  styleUrl: './status-chip.css',
})
export class StatusChip {
  code = input.required<string>();
  label = input.required<string>();
  tone = computed<Tone>(() => TONE_BY_CODE[this.code()] ?? 'neutral');
}
