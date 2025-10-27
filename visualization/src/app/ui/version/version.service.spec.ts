import {TestBed} from '@angular/core/testing';

import {VersionService} from './version.service';

describe('VersionService', () => {
  let testee: VersionService

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [VersionService]
    })
      .compileComponents();
    testee = TestBed.inject(VersionService)
  })

  it('should create', () => {
    expect(testee).toBeTruthy()
  })

  it('should return local version', async () => {
    expect(await testee.getVersion()).toEqual('0.0.0-local')
  })

});
