import {ComponentFixture, TestBed} from '@angular/core/testing';

import {JsonLoaderComponent} from './json.loader.component';

describe('JsonLoaderComponent', () => {
  let component: JsonLoaderComponent;
  let fixture: ComponentFixture<JsonLoaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JsonLoaderComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(JsonLoaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
