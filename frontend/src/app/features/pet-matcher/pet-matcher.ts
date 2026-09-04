import { Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { PetMatcherService } from '../../core/services/pet-matcher.service';
import { PetMatcherResponse } from '../../core/models/pet-matcher';
import { apiErrorMessage } from '../../core/api-error';

@Component({
  selector: 'app-pet-matcher',
  imports: [DecimalPipe],
  templateUrl: './pet-matcher.html',
  styleUrl: './pet-matcher.css',
})
export class PetMatcher {
  private petMatcherService = inject(PetMatcherService);

  prompt = signal('');
  result = signal<PetMatcherResponse | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);

  submit() {
    const value = this.prompt().trim();
    if (value.length < 5) {
      this.error.set('Tell me a little more about your lifestyle first.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.result.set(null);

    this.petMatcherService.recommend(value).subscribe({
      next: (res) => {
        this.result.set(res);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(apiErrorMessage(err, 'Could not get a recommendation right now.'));
        this.loading.set(false);
      },
    });
  }

  reset() {
    this.result.set(null);
    this.error.set(null);
    this.prompt.set('');
  }
}
