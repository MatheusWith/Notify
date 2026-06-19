import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { NewsletterProfilePage } from './newsletter-profile-page';
import { provideRouter } from '@angular/router';

describe('NewsletterProfilePage', () => {
  let component: NewsletterProfilePage;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'newsletter/:slug', component: NewsletterProfilePage }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    component = TestBed.runInInjectionContext(() => new NewsletterProfilePage());
    expect(component).toBeTruthy();
  });
});
