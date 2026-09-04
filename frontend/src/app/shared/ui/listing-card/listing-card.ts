import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TitleCasePipe } from '@angular/common';
import { ListingSummary } from '../../../core/models/listing';
import { StatusChip } from '../status-chip/status-chip';

@Component({
  selector: 'app-listing-card',
  imports: [RouterLink, TitleCasePipe, StatusChip],
  templateUrl: './listing-card.html',
  styleUrl: './listing-card.css',
})
export class ListingCard {
  listing = input.required<ListingSummary>();
  showStatus = input(false);
}
