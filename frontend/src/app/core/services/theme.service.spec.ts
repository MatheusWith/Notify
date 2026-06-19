import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

function createMockStorage(): Storage {
  const store = new Map<string, string>();
  return {
    getItem: (key: string) => store.get(key) ?? null,
    setItem: (key: string, value: string) => store.set(key, value),
    removeItem: (key: string) => store.delete(key),
    clear: () => store.clear(),
    length: 0,
    key: () => null,
  };
}

describe('ThemeService', () => {
  let service: ThemeService;
  let storage: Storage;

  beforeEach(() => {
    storage = createMockStorage();
    vi.spyOn(globalThis, 'localStorage', 'get').mockReturnValue(storage);
    document.documentElement.classList.remove('dark');

    TestBed.configureTestingModule({});
    service = TestBed.inject(ThemeService);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    document.documentElement.classList.remove('dark');
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should start with light mode when nothing is stored', () => {
    expect(service.isDark()).toBe(false);
  });

  it('should toggle to dark mode and persist to localStorage', () => {
    service.toggle();

    expect(service.isDark()).toBe(true);
    expect(localStorage.getItem('notify_theme')).toBe('dark');
  });

  it('should restore dark mode from localStorage on creation', () => {
    localStorage.setItem('notify_theme', 'dark');

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({});
    const newService = TestBed.inject(ThemeService);

    expect(newService.isDark()).toBe(true);
    expect(localStorage.getItem('notify_theme')).toBe('dark');
  });

  it('should add .dark class on html element when dark mode', () => {
    expect(document.documentElement.classList.contains('dark')).toBe(false);

    service.toggle();

    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });

  it('should remove .dark class on html element when toggled back', () => {
    service.toggle();
    expect(document.documentElement.classList.contains('dark')).toBe(true);

    service.toggle();
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });

  it('should toggle back to light and persist', () => {
    localStorage.setItem('notify_theme', 'dark');

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({});
    const newService = TestBed.inject(ThemeService);

    expect(newService.isDark()).toBe(true);

    newService.toggle();

    expect(newService.isDark()).toBe(false);
    expect(localStorage.getItem('notify_theme')).toBe('light');
  });
});
