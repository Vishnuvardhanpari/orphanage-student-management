import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from './core/services/theme.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit {
  private readonly themeService = inject(ThemeService);

  ngOnInit(): void {
    // Ensure theme class is applied on bootstrap (effect also runs).
    this.themeService.setMode(this.themeService.mode());
  }
}
