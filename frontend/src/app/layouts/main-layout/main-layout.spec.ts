import { TestBed } from '@angular/core/testing';
import { MainLayout } from './main-layout';
import { provideRouter } from '@angular/router';

describe('MainLayout', () => {
  it('should be created', () => {
    TestBed.configureTestingModule({
      imports: [MainLayout],
      providers: [provideRouter([])],
    });

    const component = TestBed.runInInjectionContext(() => new MainLayout());
    expect(component).toBeTruthy();
  });
});
