import {
  Component,
  ElementRef,
  AfterViewInit,
  OnDestroy,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import type * as LeafletNS from 'leaflet';
import { Coordinates } from '../../core/models/coordinates';

const DEFAULT_CENTER: Coordinates = { lat: 41.9981, lng: 21.4254 };
const DEFAULT_ZOOM = 12;
const PICKED_ZOOM = 15;

@Component({
  selector: 'app-map-picker',
  imports: [DecimalPipe],
  templateUrl: './map-picker.html',
  styleUrl: './map-picker.css',
})
export class MapPicker implements AfterViewInit, OnDestroy {
  initial = input<Coordinates | null>(null);

  confirmed = output<Coordinates>();
  cancelled = output<void>();

  private mapContainer = viewChild.required<ElementRef<HTMLDivElement>>('mapContainer');

  private leaflet: typeof LeafletNS | undefined;
  private map: LeafletNS.Map | undefined;
  private marker: LeafletNS.Marker | undefined;

  selected = signal<Coordinates | null>(null);
  mapLoadError = signal(false);

  async ngAfterViewInit(): Promise<void> {
    let L: typeof LeafletNS;
    try {
      L = await import('leaflet');
    } catch {
      this.mapLoadError.set(true);
      return;
    }
    this.leaflet = L;

    Object.assign(L.Icon.Default.prototype.options, {
      iconUrl: '/leaflet/marker-icon.png',
      iconRetinaUrl: '/leaflet/marker-icon-2x.png',
      shadowUrl: '/leaflet/marker-shadow.png',
    });

    const preselected = this.initial();
    const start = preselected ?? DEFAULT_CENTER;
    const zoom = preselected ? PICKED_ZOOM : DEFAULT_ZOOM;

    this.map = L.map(this.mapContainer().nativeElement).setView([start.lat, start.lng], zoom);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(this.map);

    if (preselected) {
      this.placeMarker(preselected);
    } else if ('geolocation' in navigator) {
      navigator.geolocation.getCurrentPosition(
        (pos) => this.map?.setView([pos.coords.latitude, pos.coords.longitude], PICKED_ZOOM),
        () => {},
        { timeout: 5000 },
      );
    }

    this.map.on('click', (event: { latlng: { lat: number; lng: number } }) => {
      this.placeMarker({ lat: event.latlng.lat, lng: event.latlng.lng });
    });
  }

  ngOnDestroy(): void {
    this.map?.remove();
  }

  confirm(): void {
    const coords = this.selected();
    if (coords) this.confirmed.emit(coords);
  }

  close(): void {
    this.cancelled.emit();
  }

  private placeMarker(coords: Coordinates): void {
    this.selected.set(coords);

    if (this.marker) {
      this.marker.setLatLng([coords.lat, coords.lng]);
      return;
    }

    if (!this.leaflet || !this.map) return;
    const marker = this.leaflet.marker([coords.lat, coords.lng], { draggable: true }).addTo(this.map);
    marker.on('dragend', () => {
      const pos = marker.getLatLng();
      this.selected.set({ lat: pos.lat, lng: pos.lng });
    });
    this.marker = marker;
  }
}
