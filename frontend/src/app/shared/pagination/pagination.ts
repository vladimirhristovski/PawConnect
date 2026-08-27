import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  templateUrl: './pagination.html',
})
export class Pagination {
  current = input.required<number>();
  totalPages = input.required<number>();
  pageChange = output<number>();
}
