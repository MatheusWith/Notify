import { Component, inject } from '@angular/core';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-theme-toggle',
  imports: [MatIconButton, MatIcon],
  templateUrl: './theme-toggle.html',
})
export class ThemeToggle {
  private readonly themeService = inject(ThemeService);
  readonly isDark = this.themeService.isDark;

  toggle(): void {
    this.themeService.toggle();
  }
}
