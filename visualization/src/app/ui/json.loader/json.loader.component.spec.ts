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

  describe('openFileDialog', () => {
    it('should click the file input element', () => {
      // Given
      const mockClick = jasmine.createSpy('click');
      component.fileInput = { nativeElement: { click: mockClick } } as any;

      // When
      component.openFileDialog();

      // Then
      expect(mockClick).toHaveBeenCalled();
    });
  });

  describe('clearSelectedInput', () => {
    it('should clear input value when target is HTMLInputElement', () => {
      // Given
      const inputElement = document.createElement('input');
      inputElement.value = 'test.json';

      // When
      component.clearSelectedInput(inputElement);

      // Then
      expect(inputElement.value).toBe('');
    });

    it('should do nothing when target is not HTMLInputElement', () => {
      // Given
      const divElement = document.createElement('div');

      // When/Then - no error thrown
      component.clearSelectedInput(divElement);
    });

    it('should do nothing when target is null', () => {
      // When/Then - no error thrown
      component.clearSelectedInput(null);
    });
  });
});
