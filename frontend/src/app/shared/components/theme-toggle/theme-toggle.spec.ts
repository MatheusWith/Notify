import { TestBed } from '@angular/core/testing';
import { ThemeToggle } from './theme-toggle';
import { ThemeService } from '../../../core/services/theme.service';

describe('ThemeToggle', () => {
  let component: ThemeToggle;
  let themeService: ThemeService;

  beforeEach(() => {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem('notify_theme');
    }

    TestBed.configureTestingModule({
      imports: [ThemeToggle],
      providers: [ThemeService],
    });

    component = TestBed.runInInjectionContext(() => new ThemeToggle());
    themeService = TestBed.inject(ThemeService);
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should toggle theme when clicked', () => {
    const spy = vi.spyOn(themeService, 'toggle');

    component.toggle();

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should reflect the current dark state', () => {
    expect(component.isDark()).toBe(false);

    themeService.toggle();

    expect(component.isDark()).toBe(true);
  });
});
